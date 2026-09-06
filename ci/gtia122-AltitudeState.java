package ch.piiwii.gtialtimeter;

import android.content.Context;
import android.content.SharedPreferences;

public final class AltitudeState {
    private static final String PREFS = "gtialtimeter_state";
    private static final long GPS_FRESH_MS = 30000L;
    private static final long BARO_FRESH_MS = 15000L;
    private AltitudeState() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void saveLocation(Context c, double lat, double lon, float hAcc,
                                    double rawAlt, double correctedAlt, double filteredAlt,
                                    float vAcc, long fixTime, String source, boolean rejected) {
        long now = System.currentTimeMillis();
        p(c).edit()
                .putBoolean("position_received", true)
                .putLong("lat", Double.doubleToRawLongBits(lat))
                .putLong("lon", Double.doubleToRawLongBits(lon))
                .putFloat("hacc", hAcc)
                .putFloat("accepted_hacc", hAcc)
                .putLong("raw_alt", Double.doubleToRawLongBits(rawAlt))
                .putLong("corrected_alt", Double.doubleToRawLongBits(correctedAlt))
                .putLong("filtered_alt", Double.doubleToRawLongBits(filteredAlt))
                .putFloat("vacc", vAcc)
                .putFloat("accepted_vacc", vAcc)
                .putLong("fix_time", fixTime)
                .putLong("accepted_fix_time", fixTime)
                .putLong("raw_fix_time", fixTime)
                .putLong("last_gps_callback", now)
                .putLong("last_update", now)
                .putString("source", source)
                .putString("reject_reason", "—")
                .putBoolean("last_rejected", rejected)
                .apply();
    }

    public static void noteGpsCallback(Context c, long fixTime) {
        p(c).edit()
                .putBoolean("position_received", true)
                .putLong("raw_fix_time", fixTime)
                .putLong("last_gps_callback", System.currentTimeMillis())
                .apply();
    }

    public static void saveRejected(Context c, double lat, double lon, float hAcc, double rawAlt,
                                    float vAcc, long fixTime, String reason) {
        long now = System.currentTimeMillis();
        SharedPreferences.Editor e = p(c).edit()
                .putBoolean("position_received", true)
                .putLong("lat", Double.doubleToRawLongBits(lat))
                .putLong("lon", Double.doubleToRawLongBits(lon))
                .putFloat("hacc", hAcc)
                .putFloat("vacc", vAcc)
                .putLong("raw_fix_time", fixTime)
                .putLong("last_gps_callback", now)
                .putLong("last_update", now)
                .putString("reject_reason", reason)
                .putBoolean("last_rejected", true);
        if (!Double.isNaN(rawAlt) && !Double.isInfinite(rawAlt)) {
            e.putLong("raw_alt", Double.doubleToRawLongBits(rawAlt));
        }
        e.apply();
    }

    public static void setTrend(Context c, double meters) {
        p(c).edit().putLong("trend_m", Double.doubleToRawLongBits(meters)).putLong("trend_time", System.currentTimeMillis()).apply();
    }
    public static double trend(Context c) { return Double.longBitsToDouble(p(c).getLong("trend_m", Double.doubleToRawLongBits(Double.NaN))); }
    public static long trendTime(Context c) { return p(c).getLong("trend_time", 0L); }

    public static void saveHeading(Context c, float degrees, String source) {
        if (Float.isNaN(degrees)) return;
        while (degrees < 0f) degrees += 360f;
        while (degrees >= 360f) degrees -= 360f;
        p(c).edit().putFloat("heading", degrees).putString("heading_source", source).putLong("heading_time", System.currentTimeMillis()).apply();
    }
    public static float heading(Context c) { return p(c).getFloat("heading", Float.NaN); }
    public static String headingSource(Context c) { return p(c).getString("heading_source", "—"); }
    public static long headingTime(Context c) { return p(c).getLong("heading_time", 0L); }

    public static void setCompassSensors(Context c, boolean present) { p(c).edit().putBoolean("compass_sensors", present).apply(); }
    public static boolean compassSensors(Context c) { return p(c).getBoolean("compass_sensors", false); }

    public static void setGpsEnabled(Context c, boolean enabled) { p(c).edit().putBoolean("gps_enabled", enabled).apply(); }
    public static boolean gpsEnabled(Context c) { return p(c).getBoolean("gps_enabled", false); }
    public static boolean positionReceived(Context c) { return p(c).getBoolean("position_received", false); }
    public static double lat(Context c) { return Double.longBitsToDouble(p(c).getLong("lat", Double.doubleToRawLongBits(Double.NaN))); }
    public static double lon(Context c) { return Double.longBitsToDouble(p(c).getLong("lon", Double.doubleToRawLongBits(Double.NaN))); }
    public static float hAcc(Context c) { return p(c).getFloat("hacc", Float.NaN); }
    public static float vAcc(Context c) { return p(c).getFloat("vacc", Float.NaN); }
    public static float acceptedHAcc(Context c) { return p(c).getFloat("accepted_hacc", hAcc(c)); }
    public static float acceptedVAcc(Context c) { return p(c).getFloat("accepted_vacc", vAcc(c)); }
    public static double raw(Context c) { return Double.longBitsToDouble(p(c).getLong("raw_alt", Double.doubleToRawLongBits(Double.NaN))); }
    public static double corrected(Context c) { return Double.longBitsToDouble(p(c).getLong("corrected_alt", Double.doubleToRawLongBits(Double.NaN))); }
    public static double filtered(Context c) { return Double.longBitsToDouble(p(c).getLong("filtered_alt", Double.doubleToRawLongBits(Double.NaN))); }

    public static long fixTime(Context c) {
        SharedPreferences sp = p(c);
        long accepted = sp.getLong("accepted_fix_time", 0L);
        if (accepted > 0L) return accepted;
        if (!sp.getBoolean("last_rejected", false)) return sp.getLong("fix_time", 0L);
        return 0L;
    }
    public static long rawFixTime(Context c) {
        SharedPreferences sp = p(c);
        long raw = sp.getLong("raw_fix_time", 0L);
        return raw > 0L ? raw : sp.getLong("fix_time", 0L);
    }
    public static long lastGpsCallback(Context c) { return p(c).getLong("last_gps_callback", 0L); }
    public static long lastUpdate(Context c) { return p(c).getLong("last_update", 0L); }
    public static String source(Context c) { return p(c).getString("source", "GPS"); }
    public static boolean lastRejected(Context c) { return p(c).getBoolean("last_rejected", false); }
    public static String rejectReason(Context c) { return p(c).getString("reject_reason", "—"); }

    public static void saveBarometer(Context c, boolean present, float pressure) {
        SharedPreferences.Editor e = p(c).edit().putBoolean("barometer_present", present);
        if (!Float.isNaN(pressure) && pressure > 100f) {
            e.putFloat("pressure", pressure).putLong("pressure_time", System.currentTimeMillis());
        }
        e.apply();
    }
    public static boolean barometerPresent(Context c) { return p(c).getBoolean("barometer_present", false); }
    public static float pressure(Context c) { return p(c).getFloat("pressure", Float.NaN); }
    public static long pressureTime(Context c) { return p(c).getLong("pressure_time", 0L); }

    public static void calibrateBarometer(Context c, float pressure, double altitude) {
        if (Float.isNaN(pressure) || pressure <= 100f || Double.isNaN(altitude) || Double.isInfinite(altitude)) return;
        long now = System.currentTimeMillis();
        p(c).edit()
                .putBoolean("baro_calibrated", true)
                .putFloat("baro_anchor_pressure", pressure)
                .putLong("baro_anchor_alt", Double.doubleToRawLongBits(altitude))
                .putLong("baro_anchor_time", now)
                .putLong("baro_alt", Double.doubleToRawLongBits(altitude))
                .putLong("baro_alt_time", now)
                .apply();
    }

    public static void saveBaroAltitude(Context c, double altitude) {
        if (Double.isNaN(altitude) || Double.isInfinite(altitude)) return;
        p(c).edit()
                .putLong("baro_alt", Double.doubleToRawLongBits(altitude))
                .putLong("baro_alt_time", System.currentTimeMillis())
                .putLong("last_update", System.currentTimeMillis())
                .apply();
    }
    public static boolean baroCalibrated(Context c) { return p(c).getBoolean("baro_calibrated", false); }
    public static float baroAnchorPressure(Context c) { return p(c).getFloat("baro_anchor_pressure", Float.NaN); }
    public static double baroAnchorAltitude(Context c) { return Double.longBitsToDouble(p(c).getLong("baro_anchor_alt", Double.doubleToRawLongBits(Double.NaN))); }
    public static long baroAnchorTime(Context c) { return p(c).getLong("baro_anchor_time", 0L); }
    public static double baroAltitude(Context c) { return Double.longBitsToDouble(p(c).getLong("baro_alt", Double.doubleToRawLongBits(Double.NaN))); }
    public static long baroAltitudeTime(Context c) { return p(c).getLong("baro_alt_time", 0L); }

    public static boolean isGpsFresh(Context c) {
        long t = fixTime(c);
        return gpsEnabled(c) && t > 0L && System.currentTimeMillis() - t <= GPS_FRESH_MS && !Double.isNaN(filtered(c));
    }
    public static boolean isRawGpsFresh(Context c) {
        long t = lastGpsCallback(c);
        return gpsEnabled(c) && t > 0L && System.currentTimeMillis() - t <= GPS_FRESH_MS;
    }
    public static boolean isBaroFresh(Context c) {
        long t = baroAltitudeTime(c);
        return baroCalibrated(c) && t > 0L && System.currentTimeMillis() - t <= BARO_FRESH_MS && !Double.isNaN(baroAltitude(c));
    }

    public static double displayAltitude(Context c) {
        if (isGpsFresh(c)) return filtered(c);
        if (isBaroFresh(c)) return baroAltitude(c);
        return Double.NaN;
    }

    public static String displaySource(Context c) {
        if (isGpsFresh(c)) return baroCalibrated(c) ? "GPS + BARO" : "GPS";
        if (isBaroFresh(c)) return "BARO";
        return "—";
    }

    public static long displayTime(Context c) {
        if (isGpsFresh(c)) return fixTime(c);
        if (isBaroFresh(c)) return baroAltitudeTime(c);
        return 0L;
    }

    public static void touchService(Context c) {
        p(c).edit().putLong("service_heartbeat", System.currentTimeMillis()).apply();
    }
    public static long serviceHeartbeat(Context c) { return p(c).getLong("service_heartbeat", 0L); }
    public static boolean serviceAlive(Context c) {
        long t = serviceHeartbeat(c);
        return t > 0L && System.currentTimeMillis() - t <= 15000L;
    }

    public static void saveWidgetSize(Context c, int wDp, int hDp) {
        p(c).edit().putInt("widget_w", wDp).putInt("widget_h", hDp).apply();
    }
    public static int widgetWidth(Context c) { return p(c).getInt("widget_w", 164); }
    public static int widgetHeight(Context c) { return p(c).getInt("widget_h", 92); }
}
