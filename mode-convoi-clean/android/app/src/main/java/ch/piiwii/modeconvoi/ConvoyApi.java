package ch.piiwii.modeconvoi;

import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public final class ConvoyApi {
    private static final Object SNAPSHOT_LOCK = new Object();
    private static String snapshotKey = "";
    private static JSONObject snapshotCache;
    private static long snapshotCachedAt = 0L;
    private static final long FOREGROUND_COALESCE_MS = 250L;

    public static final class ApiException extends IOException {
        public final int statusCode;
        public ApiException(int statusCode, String message) { super(message); this.statusCode=statusCode; }
    }

    public static JSONObject get(String base, String path) throws Exception {
        if (isConvoySnapshotPath(path)) return getSnapshot(base, path, FOREGROUND_COALESCE_MS);
        return getDirect(base, path);
    }

    public static JSONObject getCachedSnapshot(String base, String path, long maxAgeMs) throws Exception {
        if (!isConvoySnapshotPath(path)) return get(base, path);
        return getSnapshot(base, path, Math.max(0L, maxAgeMs));
    }

    public static JSONObject post(String base, String path, JSONObject body, String adminKey) throws Exception {
        HttpURLConnection c = open(base, path, "POST");
        if (adminKey != null && !adminKey.isEmpty()) c.setRequestProperty("X-Admin-Key", adminKey);
        c.setDoOutput(true);
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        c.setFixedLengthStreamingMode(bytes.length);
        try (OutputStream out = c.getOutputStream()) { out.write(bytes); }
        return read(c);
    }

    private static JSONObject getSnapshot(String base, String path, long maxAgeMs) throws Exception {
        String key = normalizedBase(base) + path;
        synchronized (SNAPSHOT_LOCK) {
            long now = System.currentTimeMillis();
            if (snapshotCache != null && key.equals(snapshotKey) && now - snapshotCachedAt <= maxAgeMs) {
                return copy(snapshotCache);
            }
            JSONObject fresh = getDirect(base, path);
            snapshotKey = key;
            snapshotCachedAt = System.currentTimeMillis();
            snapshotCache = copy(fresh);
            return fresh;
        }
    }

    private static JSONObject getDirect(String base, String path) throws Exception {
        HttpURLConnection c = open(base, path, "GET");
        return read(c);
    }

    private static JSONObject copy(JSONObject source) throws Exception {
        return new JSONObject(source == null ? "{}" : source.toString());
    }

    private static boolean isConvoySnapshotPath(String path) {
        if (path == null) return false;
        int query = path.indexOf('?');
        String clean = query >= 0 ? path.substring(0, query) : path;
        String prefix = "/api/convoys/";
        if (!clean.startsWith(prefix)) return false;
        String tail = clean.substring(prefix.length());
        return !tail.isEmpty() && tail.indexOf('/') < 0;
    }

    private static String normalizedBase(String base) throws IOException {
        String b = base == null ? "" : base.trim();
        while (b.endsWith("/")) b = b.substring(0, b.length()-1);
        if (b.isEmpty()) throw new IOException("Adresse du serveur manquante");
        return b;
    }

    private static HttpURLConnection open(String base, String path, String method) throws Exception {
        String b = normalizedBase(base);
        URL url = new URL(b + path);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(7000);
        c.setReadTimeout(7000);
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        c.setRequestProperty("User-Agent", "ModeConvoi-Android/0.3.27");
        c.setUseCaches(false);
        return c;
    }
    private static JSONObject read(HttpURLConnection c) throws Exception {
        int code = c.getResponseCode();
        InputStream in = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
        StringBuilder sb = new StringBuilder();
        if (in != null) try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line; while ((line = r.readLine()) != null) sb.append(line);
        }
        String raw=sb.toString().trim();
        JSONObject o;
        try { o = raw.isEmpty() ? new JSONObject() : new JSONObject(raw); }
        catch (Exception parse) {
            if(code==404) throw new ApiException(code,"Serveur Mode Convoi introuvable ou plugin non activé");
            throw new ApiException(code,"Réponse invalide du serveur Mode Convoi");
        }
        if (code < 200 || code >= 300) throw new ApiException(code, o.optString("error", "HTTP " + code));
        return o;
    }
}
