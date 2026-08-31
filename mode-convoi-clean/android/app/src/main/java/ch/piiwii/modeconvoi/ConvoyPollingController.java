package ch.piiwii.modeconvoi;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Owns foreground convoy synchronization for the activity.
 *
 * MainActivity only reacts to snapshots and connection state. Scheduling,
 * backoff, in-flight protection and repository access live here.
 */
public final class ConvoyPollingController implements AutoCloseable {
    public enum ConnectionState { CONNECTED, RECONNECTING, OFFLINE }

    public interface Listener {
        void onSnapshot(JSONObject snapshot, boolean renamed, long synchronizedAt);
        void onConnectionState(ConnectionState state, int consecutiveFailures);
        void onSessionInvalidated(int statusCode);
    }

    private static final long SUCCESS_INTERVAL_MS = 3500L;
    private static final long FIRST_RETRY_MS = 5000L;
    private static final long MAX_RETRY_MS = 30000L;

    private final Context context;
    private final AppPrefs prefs;
    private final String fallbackServer;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ScheduledThreadPoolExecutor network = new ScheduledThreadPoolExecutor(1);
    private final Object stateLock = new Object();

    private ScheduledFuture<?> pending;
    private boolean running;
    private boolean closed;
    private boolean inFlight;
    private boolean refreshRequested;
    private int consecutiveFailures;
    private long generation;
    private volatile long lastSuccessfulSyncAt;

    public ConvoyPollingController(Context context, AppPrefs prefs, String fallbackServer, Listener listener) {
        this.context = context.getApplicationContext();
        this.prefs = prefs;
        this.fallbackServer = fallbackServer == null ? "" : fallbackServer;
        this.listener = listener;
        network.setRemoveOnCancelPolicy(true);
    }

    public void start() {
        synchronized (stateLock) {
            if (closed || running || !prefs.hasActiveConvoy()) return;
            running = true;
            consecutiveFailures = 0;
            scheduleLocked(0L);
        }
    }

    public void stop() {
        synchronized (stateLock) {
            running = false;
            inFlight = false;
            refreshRequested = false;
            generation++;
            if (pending != null) pending.cancel(false);
            pending = null;
        }
    }

    public void refreshNow() {
        synchronized (stateLock) {
            if (closed || !running || !prefs.hasActiveConvoy()) return;
            if (inFlight) {
                refreshRequested = true;
                return;
            }
            if (pending != null) pending.cancel(false);
            pending = null;
            scheduleLocked(0L);
        }
    }

    public long lastSuccessfulSyncAt() {
        return lastSuccessfulSyncAt;
    }

    @Override public void close() {
        synchronized (stateLock) {
            if (closed) return;
            closed = true;
            running = false;
            inFlight = false;
            refreshRequested = false;
            generation++;
            if (pending != null) pending.cancel(false);
            pending = null;
        }
        network.shutdownNow();
    }

    private void scheduleLocked(long delayMs) {
        if (closed || !running || !prefs.hasActiveConvoy()) return;
        final long scheduledGeneration = generation;
        pending = network.schedule(() -> poll(scheduledGeneration), Math.max(0L, delayMs), TimeUnit.MILLISECONDS);
    }

    private boolean abandonIfStale(long scheduledGeneration) {
        synchronized (stateLock) {
            if (closed || !running || scheduledGeneration != generation || !prefs.hasActiveConvoy()) {
                inFlight = false;
                return true;
            }
            return false;
        }
    }

    private void poll(long scheduledGeneration) {
        synchronized (stateLock) {
            pending = null;
            if (closed || !running || scheduledGeneration != generation || !prefs.hasActiveConvoy()) return;
            if (inFlight) {
                refreshRequested = true;
                return;
            }
            inFlight = true;
        }

        try {
            JSONObject snapshot = ConvoySnapshotRepository.get(
                    prefs,
                    fallbackServer,
                    ConvoySnapshotRepository.FOREGROUND_MAX_AGE_MS);

            // A pause/resume may have replaced this polling generation while the HTTP request was running.
            // Never process events or update the UI from that stale request.
            if (abandonIfStale(scheduledGeneration)) return;

            ConvoyEventProcessor.process(context, prefs, snapshot);

            String newName = snapshot.optString("name", "");
            String oldName = prefs.get("convoyName", "");
            boolean renamed = !newName.isEmpty() && !newName.equals(oldName);
            if (renamed) prefs.put("convoyName", newName);

            long synchronizedAt = System.currentTimeMillis();
            lastSuccessfulSyncAt = synchronizedAt;

            synchronized (stateLock) {
                inFlight = false;
                if (closed || !running || scheduledGeneration != generation) return;
                consecutiveFailures = 0;
                long nextDelay = refreshRequested ? 0L : SUCCESS_INTERVAL_MS;
                refreshRequested = false;
                scheduleLocked(nextDelay);
            }

            main.post(() -> {
                synchronized (stateLock) {
                    if (closed || !running || scheduledGeneration != generation) return;
                }
                if (listener == null) return;
                listener.onConnectionState(ConnectionState.CONNECTED, 0);
                listener.onSnapshot(snapshot, renamed, synchronizedAt);
            });
        } catch (Exception error) {
            synchronized (stateLock) {
                if (closed || !running || scheduledGeneration != generation) {
                    inFlight = false;
                    return;
                }
            }

            int statusCode = error instanceof ConvoyApi.ApiException
                    ? ((ConvoyApi.ApiException) error).statusCode : 0;

            if (statusCode == 401 || statusCode == 404) {
                synchronized (stateLock) {
                    inFlight = false;
                    running = false;
                    refreshRequested = false;
                    generation++;
                    if (pending != null) pending.cancel(false);
                    pending = null;
                }
                main.post(() -> {
                    if (listener != null) listener.onSessionInvalidated(statusCode);
                });
                return;
            }

            final int failures;
            final long retry;
            synchronized (stateLock) {
                inFlight = false;
                if (closed || !running || scheduledGeneration != generation) return;
                consecutiveFailures++;
                failures = consecutiveFailures;
                long factor = 1L << Math.min(3, Math.max(0, failures - 1));
                retry = Math.min(MAX_RETRY_MS, FIRST_RETRY_MS * factor);
                refreshRequested = false;
                scheduleLocked(retry);
            }

            main.post(() -> {
                synchronized (stateLock) {
                    if (closed || !running || scheduledGeneration != generation) return;
                }
                if (listener == null) return;
                listener.onConnectionState(
                        failures < 3 ? ConnectionState.RECONNECTING : ConnectionState.OFFLINE,
                        failures);
            });
        }
    }
}
