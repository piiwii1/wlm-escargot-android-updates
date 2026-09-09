package ch.piiwii.listybridge;

import android.content.Context;
import android.net.Uri;

public final class BridgeConfig {
    public static final String KEY_ENDPOINT = "endpoint";
    public static final String KEY_TOKEN = "token";
    private static final String KEY_PAIR_ENDPOINT = "pair_endpoint";
    private static final String KEY_PAIR_ID = "pair_id";
    private static final String KEY_PAIR_CODE = "pair_code";

    private BridgeConfig() {}

    public static boolean parseAndSave(Context c, String text) {
        if (text == null) return false;
        text = text.trim();
        if (!text.startsWith("listybridge://pair")) return false;
        Uri u = Uri.parse(text);

        String endpoint = u.getQueryParameter("endpoint");
        String token = u.getQueryParameter("token");
        if (endpoint != null && endpoint.startsWith("https://") && token != null && token.startsWith("LYM1.")) {
            return saveFinal(c, endpoint, token);
        }

        String pairEndpoint = u.getQueryParameter("pair_endpoint");
        String pairId = u.getQueryParameter("pair_id");
        String code = u.getQueryParameter("code");
        if (pairEndpoint != null && pairEndpoint.startsWith("https://") && pairId != null && pairId.matches("[A-Fa-f0-9]{16}") && code != null && code.replace("-", "").matches("[A-Za-z2-9]{8}")) {
            SecureStore.put(c, KEY_PAIR_ENDPOINT, pairEndpoint);
            SecureStore.put(c, KEY_PAIR_ID, pairId.toLowerCase());
            SecureStore.put(c, KEY_PAIR_CODE, code.replace("-", "").toUpperCase());
            return true;
        }
        return false;
    }

    public static boolean saveFinal(Context c, String syncEndpoint, String token) {
        if (syncEndpoint == null || !syncEndpoint.startsWith("https://") || token == null || !token.startsWith("LYM1.")) return false;
        SecureStore.put(c, KEY_ENDPOINT, syncEndpoint);
        SecureStore.put(c, KEY_TOKEN, token);
        clearPending(c);
        return true;
    }

    public static String endpoint(Context c) { return SecureStore.get(c, KEY_ENDPOINT); }
    public static String token(Context c) { return SecureStore.get(c, KEY_TOKEN); }
    public static boolean paired(Context c) { return !endpoint(c).isEmpty() && !token(c).isEmpty(); }

    public static String pairEndpoint(Context c) { return SecureStore.get(c, KEY_PAIR_ENDPOINT); }
    public static String pairId(Context c) { return SecureStore.get(c, KEY_PAIR_ID); }
    public static String pairCode(Context c) { return SecureStore.get(c, KEY_PAIR_CODE); }
    public static boolean hasPending(Context c) { return !pairEndpoint(c).isEmpty() && !pairId(c).isEmpty() && !pairCode(c).isEmpty(); }

    public static void clearPending(Context c) {
        SecureStore.put(c, KEY_PAIR_ENDPOINT, "");
        SecureStore.put(c, KEY_PAIR_ID, "");
        SecureStore.put(c, KEY_PAIR_CODE, "");
    }

    private static String siblingEndpoint(Context c, String name) {
        String e = endpoint(c);
        return e.endsWith("/bridge-sync") ? e.substring(0, e.length() - "/bridge-sync".length()) + "/" + name : e;
    }
    public static String pingEndpoint(Context c) { return siblingEndpoint(c, "bridge-ping"); }
    public static String reportEndpoint(Context c) { return siblingEndpoint(c, "bridge-report"); }
}
