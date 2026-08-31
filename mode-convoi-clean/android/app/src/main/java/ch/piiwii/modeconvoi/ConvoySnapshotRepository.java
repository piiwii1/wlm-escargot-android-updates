package ch.piiwii.modeconvoi;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;

/**
 * Single source of truth for the current convoy snapshot.
 *
 * The UI and the foreground location service share the same in-process snapshot cache.
 * The transport layer remains in ConvoyApi; this repository owns snapshot identity,
 * authentication query construction, defensive copies and freshness rules.
 */
public final class ConvoySnapshotRepository {
    public static final long FOREGROUND_MAX_AGE_MS = 250L;
    public static final long BACKGROUND_MAX_AGE_MS = 6000L;

    private static final Object LOCK = new Object();
    private static String cacheKey = "";
    private static JSONObject cachedSnapshot;
    private static long cachedAt = 0L;

    private ConvoySnapshotRepository() {}

    public static JSONObject getForBackground(AppPrefs prefs) throws Exception {
        return get(prefs, "", BACKGROUND_MAX_AGE_MS);
    }

    public static JSONObject get(AppPrefs prefs, String fallbackServer, long maxAgeMs) throws Exception {
        if (prefs == null) throw new IOException("Préférences Mode Convoi indisponibles");
        String base = prefs.get("serverUrl", fallbackServer == null ? "" : fallbackServer);
        String code = prefs.get("code", "");
        String participantId = prefs.get("participantId", "");
        String token = prefs.get("token", "");
        if (code.isEmpty() || participantId.isEmpty() || token.isEmpty()) {
            throw new IOException("Aucune session Mode Convoi active");
        }
        return get(base, snapshotPath(code, participantId, token), maxAgeMs);
    }

    static JSONObject get(String base, String path, long maxAgeMs) throws Exception {
        if (!isSnapshotPath(path)) return ConvoyApi.getDirect(base, path);
        String key = normalizedBase(base) + path;
        long allowedAge = Math.max(0L, maxAgeMs);
        synchronized (LOCK) {
            long now = System.currentTimeMillis();
            if (cachedSnapshot != null && key.equals(cacheKey) && now - cachedAt <= allowedAge) {
                return copy(cachedSnapshot);
            }
            JSONObject fresh = ConvoyApi.getDirect(base, path);
            cacheKey = key;
            cachedAt = System.currentTimeMillis();
            cachedSnapshot = copy(fresh);
            return copy(fresh);
        }
    }

    public static void invalidate() {
        synchronized (LOCK) {
            cacheKey = "";
            cachedSnapshot = null;
            cachedAt = 0L;
        }
    }

    static boolean isSnapshotPath(String path) {
        if (path == null) return false;
        int query = path.indexOf('?');
        String clean = query >= 0 ? path.substring(0, query) : path;
        String prefix = "/api/convoys/";
        if (!clean.startsWith(prefix)) return false;
        String tail = clean.substring(prefix.length());
        return !tail.isEmpty() && tail.indexOf('/') < 0;
    }

    private static String snapshotPath(String code, String participantId, String token) throws Exception {
        return "/api/convoys/" + code
                + "?participantId=" + URLEncoder.encode(participantId, "UTF-8")
                + "&token=" + URLEncoder.encode(token, "UTF-8");
    }

    private static String normalizedBase(String base) throws IOException {
        String b = base == null ? "" : base.trim();
        while (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        if (b.isEmpty()) throw new IOException("Adresse du serveur manquante");
        return b;
    }

    private static JSONObject copy(JSONObject source) throws Exception {
        return new JSONObject(source == null ? "{}" : source.toString());
    }
}
