package ch.piiwii.gtigps;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.util.DisplayMetrics;

public final class WindowPrefs {
    private static final String PREFS = "gti_google_maps_window";
    private static final String AUTO_BOOT = "auto_boot";
    private static final String LAST_MODE = "last_mode";

    private WindowPrefs() {}

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /**
     * 2.1.0 : zone de TEST volontairement fixe.
     *
     * Sur le TS18 1024x600, on reprend la zone historique du Launcher GTI :
     * x=330, y=47, 380x350.
     *
     * Sur téléphone/tablette en portrait, la zone est calculée avec les mêmes
     * proportions que le grand widget visible sur l'écran d'accueil. Cela permet
     * de tester le comportement "Maps par-dessus le launcher" sans calibration.
     */
    public static Rect getBounds(Context c) {
        DisplayMetrics dm = c.getResources().getDisplayMetrics();
        int sw = dm.widthPixels;
        int sh = dm.heightPixels;

        int x, y, w, h;
        if (sw >= 900 && sh <= 800) {
            x = 330;
            y = 47;
            w = 380;
            h = 350;
        } else if (sh > sw) {
            x = Math.round(sw * 0.065f);
            y = Math.round(sh * 0.067f);
            w = Math.round(sw * 0.870f);
            h = Math.round(sh * 0.440f);
        } else {
            x = Math.round(sw * 0.08f);
            y = Math.round(sh * 0.08f);
            w = Math.round(sw * 0.62f);
            h = Math.round(sh * 0.72f);
        }

        w = Math.max(240, Math.min(w, sw));
        h = Math.max(180, Math.min(h, sh));
        x = Math.max(0, Math.min(x, Math.max(0, sw - w)));
        y = Math.max(0, Math.min(y, Math.max(0, sh - h)));
        return new Rect(x, y, x + w, y + h);
    }

    public static String boundsSource(Context c) {
        DisplayMetrics dm = c.getResources().getDisplayMetrics();
        if (dm.widthPixels >= 900 && dm.heightPixels <= 800) {
            return "preset TS18 / Launcher GTI";
        }
        return dm.heightPixels > dm.widthPixels ? "preset test accueil portrait" : "preset test accueil paysage";
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
