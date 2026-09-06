from pathlib import Path

ROOT = Path('ci-gtialtimeter-1.2.4-project')

def replace_once(path, old, new, label):
    p = ROOT / path
    s = p.read_text()
    if old not in s:
        raise SystemExit(f'{label}: pattern not found in {path}')
    s = s.replace(old, new, 1)
    p.write_text(s)

# Version
replace_once('app/build.gradle', 'versionCode 8', 'versionCode 9', 'versionCode')
replace_once('app/build.gradle', "versionName '1.2.3'", "versionName '1.2.4'", 'versionName')

# AltitudeState: migrate orphaned altitude/baro state, track GPS retries, and bind BARO to a real GPS anchor.
p = ROOT / 'app/src/main/java/ch/piiwii/gtialtimeter/AltitudeState.java'
s = p.read_text()
s = s.replace(
'''    private static final long BARO_FRESH_MS = 15000L;\n    private AltitudeState() {}\n''',
'''    private static final long BARO_FRESH_MS = 15000L;\n    private static final int STATE_SCHEMA_124 = 124;\n    private static final double BARO_MAX_ANCHOR_DISTANCE_M = 1500.0;\n    private AltitudeState() {}\n''', 1)
s = s.replace(
'''    private static SharedPreferences p(Context c) {\n        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);\n    }\n''',
'''    private static SharedPreferences p(Context c) {\n        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);\n    }\n\n    /**\n     * 1.2.4 migration: older versions could retain a filtered altitude and a\n     * barometer anchor although no accepted GPS timestamp existed anymore.\n     */\n    public static void migrateAndSanitize(Context c) {\n        SharedPreferences sp = p(c);\n        if (sp.getInt("state_schema", 0) >= STATE_SCHEMA_124) return;\n        long accepted = sp.getLong("accepted_fix_time", 0L);\n        double filtered = Double.longBitsToDouble(sp.getLong("filtered_alt", Double.doubleToRawLongBits(Double.NaN)));\n        SharedPreferences.Editor e = sp.edit().putInt("state_schema", STATE_SCHEMA_124);\n        if (accepted <= 0L && !Double.isNaN(filtered) && !Double.isInfinite(filtered)) {\n            e.remove("filtered_alt")\n                    .remove("corrected_alt")\n                    .remove("accepted_hacc")\n                    .remove("accepted_vacc")\n                    .remove("fix_time")\n                    .remove("baro_calibrated")\n                    .remove("baro_anchor_pressure")\n                    .remove("baro_anchor_alt")\n                    .remove("baro_anchor_time")\n                    .remove("baro_anchor_lat")\n                    .remove("baro_anchor_lon")\n                    .remove("baro_anchor_fix_time")\n                    .remove("baro_alt")\n                    .remove("baro_alt_time")\n                    .putString("source", "—")\n                    .putString("reject_reason", "ancienne altitude sans fix accepté réinitialisée");\n        }\n        e.apply();\n    }\n''', 1)
s = s.replace(
'''    public static long lastGpsCallback(Context c) { return p(c).getLong("last_gps_callback", 0L); }\n    public static long lastUpdate(Context c) { return p(c).getLong("last_update", 0L); }\n''',
'''    public static long lastGpsCallback(Context c) { return p(c).getLong("last_gps_callback", 0L); }\n    public static void noteGpsRequest(Context c, String mode) {\n        SharedPreferences sp = p(c);\n        int count = sp.getInt("gps_request_count", 0);\n        sp.edit().putLong("last_gps_request", System.currentTimeMillis())\n                .putInt("gps_request_count", count + 1)\n                .putString("gps_request_mode", mode == null ? "—" : mode)\n                .apply();\n    }\n    public static long lastGpsRequest(Context c) { return p(c).getLong("last_gps_request", 0L); }\n    public static int gpsRequestCount(Context c) { return p(c).getInt("gps_request_count", 0); }\n    public static String gpsRequestMode(Context c) { return p(c).getString("gps_request_mode", "—"); }\n    public static long lastUpdate(Context c) { return p(c).getLong("last_update", 0L); }\n''', 1)
old = '''        p(c).edit()\n                .putBoolean("baro_calibrated", true)\n                .putFloat("baro_anchor_pressure", pressure)\n                .putLong("baro_anchor_alt", Double.doubleToRawLongBits(altitude))\n                .putLong("baro_anchor_time", now)\n                .putLong("baro_alt", Double.doubleToRawLongBits(altitude))\n                .putLong("baro_alt_time", now)\n                .apply();\n'''
new = '''        SharedPreferences sp = p(c);\n        SharedPreferences.Editor e = sp.edit()\n                .putBoolean("baro_calibrated", true)\n                .putFloat("baro_anchor_pressure", pressure)\n                .putLong("baro_anchor_alt", Double.doubleToRawLongBits(altitude))\n                .putLong("baro_anchor_time", now)\n                .putLong("baro_anchor_fix_time", fixTime(c))\n                .putLong("baro_alt", Double.doubleToRawLongBits(altitude))\n                .putLong("baro_alt_time", now);\n        double lat = lat(c);\n        double lon = lon(c);\n        if (!Double.isNaN(lat) && !Double.isNaN(lon)) {\n            e.putLong("baro_anchor_lat", Double.doubleToRawLongBits(lat));\n            e.putLong("baro_anchor_lon", Double.doubleToRawLongBits(lon));\n        }\n        e.apply();\n'''
if old not in s: raise SystemExit('calibrateBarometer block missing')
s = s.replace(old, new, 1)
s = s.replace(
'''    public static double baroAltitude(Context c) { return Double.longBitsToDouble(p(c).getLong("baro_alt", Double.doubleToRawLongBits(Double.NaN))); }\n    public static long baroAltitudeTime(Context c) { return p(c).getLong("baro_alt_time", 0L); }\n\n    public static boolean isGpsFresh(Context c) {\n''',
'''    public static double baroAltitude(Context c) { return Double.longBitsToDouble(p(c).getLong("baro_alt", Double.doubleToRawLongBits(Double.NaN))); }\n    public static long baroAltitudeTime(Context c) { return p(c).getLong("baro_alt_time", 0L); }\n    public static long baroAnchorFixTime(Context c) { return p(c).getLong("baro_anchor_fix_time", 0L); }\n\n    public static void invalidateBarometer(Context c, String reason) {\n        SharedPreferences.Editor e = p(c).edit()\n                .remove("baro_calibrated")\n                .remove("baro_anchor_pressure")\n                .remove("baro_anchor_alt")\n                .remove("baro_anchor_time")\n                .remove("baro_anchor_lat")\n                .remove("baro_anchor_lon")\n                .remove("baro_anchor_fix_time")\n                .remove("baro_alt")\n                .remove("baro_alt_time");\n        if (reason != null && !reason.isEmpty()) e.putString("baro_invalid_reason", reason);\n        e.apply();\n    }\n\n    public static boolean baroAnchorTrusted(Context c) {\n        SharedPreferences sp = p(c);\n        if (!sp.getBoolean("baro_calibrated", false)) return false;\n        long anchorFix = sp.getLong("baro_anchor_fix_time", 0L);\n        if (anchorFix <= 0L) return false;\n        double aLat = Double.longBitsToDouble(sp.getLong("baro_anchor_lat", Double.doubleToRawLongBits(Double.NaN)));\n        double aLon = Double.longBitsToDouble(sp.getLong("baro_anchor_lon", Double.doubleToRawLongBits(Double.NaN)));\n        double curLat = lat(c), curLon = lon(c);\n        if (!Double.isNaN(aLat) && !Double.isNaN(aLon) && !Double.isNaN(curLat) && !Double.isNaN(curLon)) {\n            if (distanceMeters(aLat, aLon, curLat, curLon) > BARO_MAX_ANCHOR_DISTANCE_M) return false;\n        }\n        return true;\n    }\n\n    private static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {\n        final double r = 6371000.0;\n        double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2);\n        double dp = Math.toRadians(lat2 - lat1);\n        double dl = Math.toRadians(lon2 - lon1);\n        double a = Math.sin(dp / 2.0) * Math.sin(dp / 2.0)\n                + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2.0) * Math.sin(dl / 2.0);\n        return 2.0 * r * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));\n    }\n\n    public static boolean isGpsFresh(Context c) {\n''', 1)
s = s.replace(
'''        return baroCalibrated(c) && t > 0L && System.currentTimeMillis() - t <= BARO_FRESH_MS && !Double.isNaN(baroAltitude(c));\n''',
'''        return baroAnchorTrusted(c) && t > 0L && System.currentTimeMillis() - t <= BARO_FRESH_MS && !Double.isNaN(baroAltitude(c));\n''', 1)
p.write_text(s)

# AltitudeService: keep continuous registration alive; use an independent one-shot recovery request.
p = ROOT / 'app/src/main/java/ch/piiwii/gtialtimeter/AltitudeService.java'
s = p.read_text()
s = s.replace('private static final long BARO_BOOTSTRAP_MAX_AGE_MS = 6L * 60L * 60L * 1000L;',
              'private static final long BARO_BOOTSTRAP_MAX_AGE_MS = 30L * 60L * 1000L;', 1)
s = s.replace('''    private long lastGpsRequestAt;\n    private long lastBaroUiAt;\n''',
'''    private long lastGpsRequestAt;\n    private long lastOneShotAt;\n    private boolean gpsRegistered;\n    private long lastBaroUiAt;\n''', 1)
s = s.replace('''            if (AltitudeState.gpsEnabled(AltitudeService.this)\n                    && (lastCallback <= 0L || now - lastCallback > GPS_REREQUEST_AFTER_MS)) {\n                requestGps(false);\n            }\n''',
'''            if (AltitudeState.gpsEnabled(AltitudeService.this)\n                    && (lastCallback <= 0L || now - lastCallback > GPS_REREQUEST_AFTER_MS)) {\n                requestGps(false);\n                requestOneShotFix();\n            }\n''', 1)
s = s.replace('''        startForeground(NOTIFICATION_ID, buildNotification("Recherche GPS…"));\n        AltitudeState.touchService(this);\n''',
'''        startForeground(NOTIFICATION_ID, buildNotification("Recherche GPS…"));\n        AltitudeState.migrateAndSanitize(this);\n        AltitudeState.touchService(this);\n''', 1)
s = s.replace('''        requestGps(true);\n        ServiceWatchdog.schedule(this, ServiceWatchdog.NORMAL_DELAY_MS);\n        return START_STICKY;\n''',
'''        requestGps(false);\n        ServiceWatchdog.schedule(this, ServiceWatchdog.NORMAL_DELAY_MS);\n        return START_STICKY;\n''', 1)
start = s.index('    private void requestGps(boolean force) {')
end = s.index('\n    private void refreshProviderState()', start)
new_methods = '''    private void requestGps(boolean force) {\n        if (locationManager == null) return;\n        if (Build.VERSION.SDK_INT >= 23\n                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;\n        long now = System.currentTimeMillis();\n        if (!force && gpsRegistered) return;\n        if (!force && now - lastGpsRequestAt < GPS_REQUEST_THROTTLE_MS) return;\n        lastGpsRequestAt = now;\n        try {\n            if (force && gpsRegistered) {\n                locationManager.removeUpdates(this);\n                gpsRegistered = false;\n            }\n            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this, Looper.getMainLooper());\n            gpsRegistered = true;\n            AltitudeState.noteGpsRequest(this, force ? "continu-force" : "continu");\n        } catch (SecurityException | IllegalArgumentException ignored) {\n            gpsRegistered = false;\n        }\n    }\n\n    private void requestOneShotFix() {\n        if (locationManager == null) return;\n        if (Build.VERSION.SDK_INT >= 23\n                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;\n        long now = System.currentTimeMillis();\n        if (now - lastOneShotAt < GPS_REQUEST_THROTTLE_MS) return;\n        lastOneShotAt = now;\n        try {\n            AltitudeState.noteGpsRequest(this, Build.VERSION.SDK_INT >= 30 ? "one-shot-current" : "one-shot-single");\n            if (Build.VERSION.SDK_INT >= 30) {\n                locationManager.getCurrentLocation(LocationManager.GPS_PROVIDER, null, getMainExecutor(), location -> {\n                    if (location != null) onLocationChanged(location);\n                });\n            } else {\n                LocationListener oneShot = new LocationListener() {\n                    @Override public void onLocationChanged(Location location) {\n                        if (location != null) AltitudeService.this.onLocationChanged(location);\n                    }\n                    @Override public void onProviderEnabled(String provider) { }\n                    @Override public void onProviderDisabled(String provider) { }\n                    @Override public void onStatusChanged(String provider, int status, Bundle extras) { }\n                };\n                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, oneShot, Looper.getMainLooper());\n            }\n        } catch (SecurityException | IllegalArgumentException ignored) { }\n    }\n'''
s = s[:start] + new_methods + s[end:]
s = s.replace('''        } else {\n            AltitudeState.saveRejected(this, location.getLatitude(), location.getLongitude(), hAcc, raw, vAcc, fixTime, result.reason);\n        }\n        updateNotificationFromState();\n''',
'''        } else {\n            AltitudeState.saveRejected(this, location.getLatitude(), location.getLongitude(), hAcc, raw, vAcc, fixTime, result.reason);\n            if (AltitudeState.baroCalibrated(this) && !AltitudeState.baroAnchorTrusted(this)) {\n                AltitudeState.invalidateBarometer(this, "déplacement trop éloigné de l’ancre GPS");\n            }\n        }\n        updateNotificationFromState();\n''', 1)
s = s.replace('''        long last = AltitudeState.lastUpdate(this);\n        if (Double.isNaN(stored) || Double.isInfinite(stored) || last <= 0L) return;\n''',
'''        long last = AltitudeState.fixTime(this);\n        if (Double.isNaN(stored) || Double.isInfinite(stored) || last <= 0L) return;\n''', 1)
s = s.replace('''            try { locationManager.removeUpdates(this); } catch (SecurityException ignored) { }\n        }\n''',
'''            try { locationManager.removeUpdates(this); } catch (SecurityException ignored) { }\n            gpsRegistered = false;\n        }\n''', 1)
p.write_text(s)

# Diagnostics.
p = ROOT / 'app/src/main/java/ch/piiwii/gtialtimeter/MainActivity.java'
s = p.read_text().replace('La v1.2.3 maintient', 'La v1.2.4 maintient')
old = '''        addRow("Âge dernière trame GPS", age(AltitudeState.rawFixTime(this)), null);\n        addRow("Âge dernier fix accepté", age(AltitudeState.fixTime(this)), null);\n'''
new = '''        addRow("Âge dernière trame GPS", age(AltitudeState.rawFixTime(this)), null);\n        addRow("Âge callback GPS", age(AltitudeState.lastGpsCallback(this)), null);\n        addRow("Âge dernière relance GPS", age(AltitudeState.lastGpsRequest(this)), null);\n        addRow("Relances GPS", AltitudeState.gpsRequestCount(this) + " · " + AltitudeState.gpsRequestMode(this), null);\n        addRow("Âge dernier fix accepté", age(AltitudeState.fixTime(this)), null);\n'''
if old not in s: raise SystemExit('MainActivity diagnostic block missing')
s = s.replace(old, new, 1)
p.write_text(s)

# README marker for exact exported sources.
p = ROOT / 'README.md'
s = p.read_text().replace('1.2.3', '1.2.4')
s += '''\n\n## 1.2.4 — récupération GPS et état altitude\n- écoute GPS continue conservée ;\n- relance one-shot sans casser l’écoute continue ;\n- suppression des altitudes filtrées orphelines sans fix accepté ;\n- ancre barométrique rattachée à un vrai fix GPS et invalidée après déplacement important ;\n- bootstrap BARO basé sur le dernier fix GPS accepté.\n'''
p.write_text(s)
