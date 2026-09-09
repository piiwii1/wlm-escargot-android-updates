package ch.piiwii.listybridge;

import android.content.Context;
import android.net.Uri;

public final class BridgeConfig {
    public static final String KEY_ENDPOINT = "endpoint";
    public static final String KEY_TOKEN = "token";
    private BridgeConfig() {}

    public static boolean parseAndSave(Context c, String text) {
        if (text == null) return false;
        text = text.trim();
        if (text.startsWith("listybridge://pair")) {
            Uri u = Uri.parse(text);
            String endpoint = u.getQueryParameter("endpoint");
            String token = u.getQueryParameter("token");
            if (endpoint != null && endpoint.startsWith("https://") && token != null && token.startsWith("LYM1.")) {
                SecureStore.put(c, KEY_ENDPOINT, endpoint);
                SecureStore.put(c, KEY_TOKEN, token);
                return true;
            }
        }
        return false;
    }

    public static String endpoint(Context c) { return SecureStore.get(c, KEY_ENDPOINT); }
    public static String token(Context c) { return SecureStore.get(c, KEY_TOKEN); }
    public static boolean paired(Context c) { return !endpoint(c).isEmpty() && !token(c).isEmpty(); }
    private static String siblingEndpoint(Context c, String name) {
        String e = endpoint(c);
        return e.endsWith("/bridge-sync") ? e.substring(0, e.length() - "/bridge-sync".length()) + "/" + name : e;
    }
    public static String pingEndpoint(Context c) { return siblingEndpoint(c, "bridge-ping"); }
    public static String reportEndpoint(Context c) { return siblingEndpoint(c, "bridge-report"); }
}
