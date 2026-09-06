package ch.piiwii.gtigps;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.util.DisplayMetrics;

public final class WindowPrefs {
    private static final String PREFS = "gti_google_maps_window";
    private static final String X = "x";
    private static final String Y = "y";
    private static final String W = "w";
    private static final String H = "h";
    private static final String CONFIGURED = "configured";
    private static final String AUTO_BOOT = "auto_boot";
    private static final String LAST_MODE = "last_mode";

    private WindowPrefs() {}

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static Rect getBounds(Context c) {
        DisplayMetrics dm = c.getResources().getDisplayMetrics();
        int sw = dm.widthPixels;
        int sh = dm.heightPixels;

        int defX, defY, defW, defH;
        if (sw >= 900 && sh <= 800) {
            defX = 330; defY = 47; defW = 380; defH = 350;
        } else {
            defX = Math.max(0, Math.round(sw * 0.07f));
            defY = Math.max(0, Math.round(sh * 0.07f));
            defW = Math.max(320, Math.round(sw * 0.86f));
            defH = Math.max(260, Math.round(sh * 0.46f));
        }
        SharedPreferences p = prefs(c);
        int x = p.getInt(X, defX);
        int y = p.getInt(Y, defY);
        int w = p.getInt(W, defW);
        int h = p.getInt(H, defH);
        w = Math.max(240, Math.min(w, sw));
        h = Math.max(180, Math.min(h, sh));
        x = Math.max(0, Math.min(x, Math.max(0, sw - w)));
        y = Math.max(0, Math.min(y, Math.max(0, sh - h)));
        return new Rect(x, y, x + w, y + h);
    }

    public static void saveBounds(Context c, Rect r) {
        prefs(c).edit()
                .putInt(X, r.left)
                .putInt(Y, r.top)
                .putInt(W, r.width())
                .putInt(H, r.height())
                .putBoolean(CONFIGURED, true)
                .apply();
    }

    public static boolean isConfigured(Context c) {
        return prefs(c).getBoolean(CONFIGURED, false);
    }

    public static boolean autoBoot(Context c) {
        return prefs(c).getBoolean(AUTO_BOOT, false);
    }

    public static void setAutoBoot(Context c, boolean enabled) {
        prefs(c).edit().putBoolean(AUTO_BOOT, enabled).apply();
    }

    public static void setLastMode(Context c, String mode) {
        prefs(c).edit().putString(LAST_MODE, mode).apply();
    }

    public static String lastMode(Context c) {
        return prefs(c).getString(LAST_MODE, "jamais lancé");
    }
}
