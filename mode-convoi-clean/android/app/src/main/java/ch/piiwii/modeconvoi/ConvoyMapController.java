package ch.piiwii.modeconvoi;

import android.os.Handler;
import android.os.Looper;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Owns convoy map WebView lifecycle, local GPS snapshot enrichment and refresh cadence.
 *
 * MainActivity remains responsible for visual layout only. This controller configures
 * the map surfaces, prepares the data sent to convoy_map.html and prevents stale
 * refresh loops from surviving page/full-screen teardown.
 */
public final class ConvoyMapController {
    private static final String MAP_URL = "file:///android_asset/convoy_map.html";
    private static final long LOCAL_REFRESH_MS = 2000L;

    public interface SnapshotProvider {
        JSONObject currentSnapshot();
    }

    public interface MainFrameErrorListener {
        void onMainFrameError();
    }

    private final AppPrefs prefs;
    private final SnapshotProvider snapshotProvider;
    private final Handler ui = new Handler(Looper.getMainLooper());

    private WebView pageView;
    private WebView fullScreenView;
    private boolean pageReady;
    private boolean fullScreenReady;
    private long pageGeneration;
    private long fullScreenGeneration;

    public ConvoyMapController(AppPrefs prefs, SnapshotProvider snapshotProvider) {
        this.prefs = prefs;
        this.snapshotProvider = snapshotProvider;
    }

    public void attachPage(WebView view, int backgroundColor, MainFrameErrorListener errorListener) {
        detachPage();
        pageView = view;
        pageReady = false;
        final long generation = ++pageGeneration;
        configure(view, backgroundColor);
        view.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView v, String url) {
                if (generation != pageGeneration || v != pageView) return;
                pageReady = true;
                pushPage();
                schedulePageRefresh(generation);
            }

            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (generation != pageGeneration || view != pageView) return;
                if (request != null && request.isForMainFrame() && errorListener != null) {
                    errorListener.onMainFrameError();
                }
            }
        });
        view.loadUrl(MAP_URL);
    }

    public void detachPage() {
        pageGeneration++;
        pageReady = false;
        pageView = null;
    }

    public void attachFullScreen(WebView view, int backgroundColor, MainFrameErrorListener errorListener) {
        detachFullScreen();
        fullScreenView = view;
        fullScreenReady = false;
        final long generation = ++fullScreenGeneration;
        configure(view, backgroundColor);
        view.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView v, String url) {
                if (generation != fullScreenGeneration || v != fullScreenView) return;
                fullScreenReady = true;
                pushFullScreen();
                scheduleFullScreenRefresh(generation);
            }

            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (generation != fullScreenGeneration || view != fullScreenView) return;
                if (request != null && request.isForMainFrame() && errorListener != null) {
                    errorListener.onMainFrameError();
                }
            }
        });
        view.loadUrl(MAP_URL);
    }

    public void detachFullScreen() {
        fullScreenGeneration++;
        fullScreenReady = false;
        fullScreenView = null;
    }

    public void pushPage() {
        push(pageView, pageReady);
    }

    public void pushFullScreen() {
        push(fullScreenView, fullScreenReady);
    }

    public void pushAll() {
        pushPage();
        pushFullScreen();
    }

    public JSONObject snapshotForMap() {
        JSONObject source = snapshotProvider == null ? null : snapshotProvider.currentSnapshot();
        try {
            JSONObject out = source == null ? new JSONObject() : new JSONObject(source.toString());
            JSONArray participants = out.optJSONArray("participants");
            if (participants == null) {
                participants = new JSONArray();
                out.put("participants", participants);
            }

            String meId = prefs.get("participantId", "");
            JSONObject me = null;
            for (int i = 0; i < participants.length(); i++) {
                JSONObject participant = participants.optJSONObject(i);
                if (participant != null && meId.equals(participant.optString("id"))) {
                    me = participant;
                    break;
                }
            }

            if (me == null && !meId.isEmpty()) {
                me = new JSONObject()
                        .put("id", meId)
                        .put("name", prefs.get("profileName", "Moi"))
                        .put("vehicle", prefs.get("profileVehicle", "Véhicule"));
                participants.put(me);
            }

            if (me != null) {
                me.put("vehicleIcon", prefs.get("profileVehicleIcon", "🚗"));
                me.put("vehicleMarkerColor", prefs.get("profileVehicleMarkerColor", "#FFB514"));
                me.put("vehicleColor", prefs.get("profileColor", ""));
                String image = prefs.get("profileVehicleImage", "");
                if (!image.isEmpty()) me.put("vehicleImage", image);
            }

            long fixAt = prefs.getLong(LocationShareService.PREF_GPS_FIX_AT, 0);
            if (me != null && fixAt > 0) {
                double lat = Double.parseDouble(prefs.get(LocationShareService.PREF_GPS_LAT, "0"));
                double lon = Double.parseDouble(prefs.get(LocationShareService.PREF_GPS_LON, "0"));
                if (Math.abs(lat) <= 90 && Math.abs(lon) <= 180 && (lat != 0 || lon != 0)) {
                    JSONObject location = new JSONObject()
                            .put("lat", lat)
                            .put("lon", lon)
                            .put("receivedAt", fixAt)
                            .put("deviceTime", fixAt);
                    try {
                        double accuracy = Double.parseDouble(prefs.get(LocationShareService.PREF_GPS_ACC, "-1"));
                        if (accuracy >= 0) location.put("accuracy", accuracy);
                    } catch (Exception ignored) {}
                    me.put("location", location);
                    out.put("serverTime", System.currentTimeMillis());
                }
            }
            return out;
        } catch (Exception ignored) {
            return source == null ? new JSONObject() : source;
        }
    }

    public void close() {
        detachPage();
        detachFullScreen();
    }

    private void configure(WebView view, int backgroundColor) {
        WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setBlockNetworkImage(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        view.setBackgroundColor(backgroundColor);
    }

    private void push(WebView view, boolean ready) {
        if (view == null || !ready) return;
        JSONObject data = snapshotForMap();
        String raw = JSONObject.quote(data.toString());
        String participantId = JSONObject.quote(prefs.get("participantId", ""));
        view.evaluateJavascript("window.updateConvoy(" + raw + "," + participantId + ")", null);
    }

    private void schedulePageRefresh(final long generation) {
        ui.postDelayed(new Runnable() {
            @Override public void run() {
                if (generation != pageGeneration || pageView == null || !pageReady) return;
                pushPage();
                schedulePageRefresh(generation);
            }
        }, LOCAL_REFRESH_MS);
    }

    private void scheduleFullScreenRefresh(final long generation) {
        ui.postDelayed(new Runnable() {
            @Override public void run() {
                if (generation != fullScreenGeneration || fullScreenView == null || !fullScreenReady) return;
                pushFullScreen();
                scheduleFullScreenRefresh(generation);
            }
        }, LOCAL_REFRESH_MS);
    }
}
