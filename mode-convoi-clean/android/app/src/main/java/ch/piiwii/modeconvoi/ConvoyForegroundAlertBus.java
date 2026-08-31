package ch.piiwii.modeconvoi;

import org.json.JSONObject;

import java.lang.ref.WeakReference;

/**
 * Small in-process bridge between background convoy event processing and the
 * currently resumed activity. Background/lock-screen delivery still falls back
 * to Android notifications when no foreground listener accepts the event.
 */
public final class ConvoyForegroundAlertBus {
    public interface Listener {
        boolean onConvoyEvent(JSONObject event);
    }

    private static final Object LOCK = new Object();
    private static WeakReference<Listener> listenerRef = new WeakReference<>(null);

    private ConvoyForegroundAlertBus() {}

    public static void register(Listener listener) {
        synchronized (LOCK) {
            listenerRef = new WeakReference<>(listener);
        }
    }

    public static void unregister(Listener listener) {
        synchronized (LOCK) {
            Listener current = listenerRef.get();
            if (current == null || current == listener) listenerRef = new WeakReference<>(null);
        }
    }

    public static boolean dispatch(JSONObject event) {
        Listener listener;
        synchronized (LOCK) { listener = listenerRef.get(); }
        if (listener == null || event == null) return false;
        try {
            return listener.onConvoyEvent(new JSONObject(event.toString()));
        } catch (Throwable ignored) {
            return false;
        }
    }
}
