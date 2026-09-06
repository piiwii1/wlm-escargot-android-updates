package ch.piiwii.gtigps;

import android.content.Context;
import android.content.SharedPreferences;

public final class GpsState {
    public static final String PREFS = "gti_gps";
    public static final String KEY_PANEL_ENABLED = "panel_enabled";
    public static final String KEY_LAST_LAT = "last_lat";
    public static final String KEY_LAST_LON = "last_lon";
    public static final String KEY_LAST_ACC = "last_acc";
    public static final String KEY_LAST_TIME = "last_time";
    public static final String KEY_LAST_BEARING = "last_bearing";
    public static final String KEY_THEME = "theme"; // auto/day/night
    public static final String KEY_ORIENTATION = "orientation"; // vehicle/north
    public static final String KEY_AUTO_RESUME = "auto_resume";
    public static final String KEY_ZOOM = "zoom";

    private GpsState() {}

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String theme(Context context) {
        return prefs(context).getString(KEY_THEME, "auto");
    }

    public static String orientation(Context context) {
        return prefs(context).getString(KEY_ORIENTATION, "vehicle");
    }

    public static float zoom(Context context) {
        return prefs(context).getFloat(KEY_ZOOM, 15.5f);
    }
}
