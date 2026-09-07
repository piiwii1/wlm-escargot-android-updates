package ch.piiwii.gtialtimeter;

import android.content.Context;
import android.content.SharedPreferences;

final class SkinPrefs {
    static final int PREMIUM = 1;
    static final int SILHOUETTE = 2;
    private static final String PREFS = "gti_altimeter_skin";
    private static final String KEY = "skin";

    private SkinPrefs() {}

    static int get(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY, PREMIUM);
    }

    static void set(Context context, int skin) {
        if (skin != SILHOUETTE) skin = PREMIUM;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY, skin).apply();
    }

    static String label(Context context) {
        return get(context) == SILHOUETTE ? "Silhouette" : "Premium";
    }
}
