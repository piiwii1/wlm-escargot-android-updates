package ch.piiwii.modeconvoi;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppPrefs {
    private static final String NAME = "mode_convoi";
    private final SharedPreferences p;

    public AppPrefs(Context c) { p = c.getSharedPreferences(NAME, Context.MODE_PRIVATE); }

    public String get(String k, String d) { return p.getString(k, d); }
    public long getLong(String k, long d) { return p.getLong(k, d); }
    public boolean getBool(String k, boolean d) { return p.getBoolean(k, d); }
    public void put(String k, String v) { p.edit().putString(k, v).apply(); }
    public void putLong(String k, long v) { p.edit().putLong(k, v).apply(); }
    public void putBool(String k, boolean v) { p.edit().putBoolean(k, v).apply(); }

    public void putLocationFix(double lat, double lon, float accuracy, long fixAt) {
        p.edit()
                .putString(LocationShareService.PREF_GPS_LAT, String.valueOf(lat))
                .putString(LocationShareService.PREF_GPS_LON, String.valueOf(lon))
                .putString(LocationShareService.PREF_GPS_ACC, String.valueOf(accuracy))
                .putLong(LocationShareService.PREF_GPS_FIX_AT, fixAt)
                .remove(LocationShareService.PREF_GPS_ERROR)
                .apply();
    }

    public void remove(String... keys) {
        SharedPreferences.Editor e = p.edit();
        for (String k : keys) e.remove(k);
        e.apply();
    }
    public boolean hasActiveConvoy() {
        return !get("code", "").isEmpty() && !get("participantId", "").isEmpty() && !get("token", "").isEmpty();
    }
    public void clearSession() {
        ConvoySnapshotRepository.invalidate();
        remove("code", "convoyName", "participantId", "token", "adminKey", "lastEventId", "lastVoiceId");
    }
}
