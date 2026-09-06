package ch.piiwii.gtigps;

import android.content.Context;
import android.content.SharedPreferences;

public final class GpsState {
    public static final String PREFS = "gti_gps";
    public static final String KEY_PANEL_ENABLED = "panel_enabled";
    public static final String KEY_TRACKING_ENABLED = "tracking_enabled";
    public static final String KEY_LAST_LAT = "last_lat";
    public static final String KEY_LAST_LON = "last_lon";
    public static final String KEY_LAST_ACC = "last_acc";
    public static final String KEY_LAST_TIME = "last_time";
    public static final String KEY_LAST_BEARING = "last_bearing";
    public static final String KEY_THEME = "theme"; // auto/day/night
    public static final String KEY_ORIENTATION = "orientation"; // vehicle/north
    public static final String KEY_AUTO_RESUME = "auto_resume";
    public static final String KEY_ZOOM = "zoom";

    public static final String KEY_NAV_ACTIVE = "nav_active";
    public static final String KEY_DEST_NAME = "dest_name";
    public static final String KEY_DEST_LAT = "dest_lat";
    public static final String KEY_DEST_LON = "dest_lon";
    public static final String KEY_ROUTE_GEOMETRY = "route_geometry";
    public static final String KEY_ROUTE_DISTANCE_M = "route_distance_m";
    public static final String KEY_ROUTE_DURATION_S = "route_duration_s";
    public static final String KEY_ROUTE_UPDATED = "route_updated";
    public static final String KEY_NEXT_INSTRUCTION = "next_instruction";
    public static final String KEY_NEXT_DISTANCE_M = "next_distance_m";
    public static final String KEY_ROUTE_ERROR = "route_error";

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

    public static boolean hasFix(Context context) {
        return prefs(context).getLong(KEY_LAST_TIME, 0L) > 0L;
    }

    public static double lastLat(Context context) {
        return Double.longBitsToDouble(prefs(context).getLong(KEY_LAST_LAT, 0L));
    }

    public static double lastLon(Context context) {
        return Double.longBitsToDouble(prefs(context).getLong(KEY_LAST_LON, 0L));
    }

    public static boolean navigationActive(Context context) {
        return prefs(context).getBoolean(KEY_NAV_ACTIVE, false);
    }

    public static double destLat(Context context) {
        return Double.longBitsToDouble(prefs(context).getLong(KEY_DEST_LAT, 0L));
    }

    public static double destLon(Context context) {
        return Double.longBitsToDouble(prefs(context).getLong(KEY_DEST_LON, 0L));
    }

    public static void clearNavigation(Context context) {
        prefs(context).edit()
                .putBoolean(KEY_NAV_ACTIVE, false)
                .remove(KEY_DEST_NAME)
                .remove(KEY_DEST_LAT)
                .remove(KEY_DEST_LON)
                .remove(KEY_ROUTE_GEOMETRY)
                .remove(KEY_ROUTE_DISTANCE_M)
                .remove(KEY_ROUTE_DURATION_S)
                .remove(KEY_ROUTE_UPDATED)
                .remove(KEY_NEXT_INSTRUCTION)
                .remove(KEY_NEXT_DISTANCE_M)
                .remove(KEY_ROUTE_ERROR)
                .apply();
    }
}
