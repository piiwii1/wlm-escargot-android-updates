from pathlib import Path
import sys

root = Path(sys.argv[1])
java = root / 'app/src/main/java/ch/piiwii/gtialtimeter'

# AltitudeFilter: allow an explicit cold-recovery reset to the fresh raw altitude.
p = java / 'AltitudeFilter.java'
s = p.read_text()
needle = '\n    Result push(double raw, float horizontalAccuracy, float verticalAccuracy, long timeMs) {'
insert = '''\n    Result resetTo(double raw, long timeMs) {\n        window.clear();\n        filtered = raw;\n        lastRaw = raw;\n        lastTime = timeMs;\n        window.addLast(raw);\n        return new Result(true, filtered, "récupération GPS provisoire");\n    }\n'''
if 'Result resetTo(double raw, long timeMs)' not in s:
    if needle not in s:
        raise SystemExit('AltitudeFilter marker missing')
    s = s.replace(needle, insert + needle)
p.write_text(s)

# AltitudeState: add a way to throw away a barometer calibration tied to an obsolete GPS altitude.
p = java / 'AltitudeState.java'
s = p.read_text()
needle = '    public static boolean baroCalibrated(Context c) { return p(c).getBoolean("baro_calibrated", false); }\n'
insert = '''    public static void invalidateBarometerCalibration(Context c) {\n        p(c).edit()\n                .putBoolean("baro_calibrated", false)\n                .remove("baro_anchor_pressure")\n                .remove("baro_anchor_alt")\n                .remove("baro_anchor_time")\n                .remove("baro_alt")\n                .remove("baro_alt_time")\n                .apply();\n    }\n\n'''
if 'invalidateBarometerCalibration' not in s:
    if needle not in s:
        raise SystemExit('AltitudeState marker missing')
    s = s.replace(needle, insert + needle)
p.write_text(s)

# AltitudeService: fix the exact stale-altitude failure seen on-device.
p = java / 'AltitudeService.java'
s = p.read_text()
s = s.replace(
    'private static final long BARO_BOOTSTRAP_MAX_AGE_MS = 6L * 60L * 60L * 1000L;',
    'private static final long BARO_BOOTSTRAP_MAX_AGE_MS = 5L * 60L * 1000L;\n'
    '    private static final long GPS_PROVISIONAL_AFTER_MS = 5L * 60L * 1000L;\n'
    '    private static final float GPS_PROVISIONAL_HACC_MAX_M = 180f;\n'
    '    private static final float GPS_PROVISIONAL_VACC_MAX_M = 160f;'
)
old = '''        AltitudeFilter.Result result = filter.push(corrected, hAcc, vAcc, fixTime);\n        if (result.accepted) {\n            AltitudeState.saveLocation(this, location.getLatitude(), location.getLongitude(), hAcc,\n                    raw, corrected, result.filtered, vAcc, fixTime, "GPS", false);\n            maybeCalibrateBarometer(result.filtered);\n            updateTrend(result.filtered, fixTime, "GPS");\n        } else {\n            AltitudeState.saveRejected(this, location.getLatitude(), location.getLongitude(), hAcc, raw, vAcc, fixTime, result.reason);\n        }\n'''
new = '''        AltitudeFilter.Result result = filter.push(corrected, hAcc, vAcc, fixTime);\n        if (result.accepted) {\n            AltitudeState.saveLocation(this, location.getLatitude(), location.getLongitude(), hAcc,\n                    raw, corrected, result.filtered, vAcc, fixTime, "GPS", false);\n            maybeCalibrateBarometer(result.filtered);\n            updateTrend(result.filtered, fixTime, "GPS");\n        } else if (shouldUseProvisionalRecovery(raw, hAcc, vAcc, now)) {\n            AltitudeFilter.Result recovered = filter.resetTo(corrected, fixTime);\n            invalidateBadBarometerAnchor(raw, hAcc);\n            AltitudeState.saveLocation(this, location.getLatitude(), location.getLongitude(), hAcc,\n                    raw, corrected, recovered.filtered, vAcc, fixTime, "GPS PROV", false);\n            updateTrend(recovered.filtered, fixTime, "GPS PROV");\n        } else {\n            AltitudeState.saveRejected(this, location.getLatitude(), location.getLongitude(), hAcc, raw, vAcc, fixTime, result.reason);\n        }\n'''
if 'shouldUseProvisionalRecovery(raw, hAcc, vAcc, now)' not in s:
    if old not in s:
        raise SystemExit('AltitudeService location block missing')
    s = s.replace(old, new)

marker = '    private void maybeCalibrateBarometer(double acceptedAltitude) {\n'
helpers = '''    private boolean shouldUseProvisionalRecovery(double raw, float hAcc, float vAcc, long now) {\n        if (Double.isNaN(raw) || Double.isInfinite(raw)) return false;\n        long accepted = AltitudeState.fixTime(this);\n        if (accepted > 0L && now - accepted < GPS_PROVISIONAL_AFTER_MS) return false;\n        if (!Float.isNaN(hAcc) && hAcc > GPS_PROVISIONAL_HACC_MAX_M) return false;\n        if (!Float.isNaN(vAcc) && vAcc > GPS_PROVISIONAL_VACC_MAX_M) return false;\n        return true;\n    }\n\n    private void invalidateBadBarometerAnchor(double gpsAltitude, float hAcc) {\n        if (!AltitudeState.baroCalibrated(this)) return;\n        double baro = AltitudeState.baroAltitude(this);\n        if (Double.isNaN(baro) || Double.isInfinite(baro)) return;\n        double tolerance = 90.0;\n        if (!Float.isNaN(hAcc)) tolerance = Math.max(tolerance, hAcc * 1.35);\n        if (Math.abs(baro - gpsAltitude) > tolerance) {\n            AltitudeState.invalidateBarometerCalibration(this);\n        }\n    }\n\n'''
if 'private boolean shouldUseProvisionalRecovery' not in s:
    if marker not in s:
        raise SystemExit('AltitudeService helper marker missing')
    s = s.replace(marker, helpers + marker)

s = s.replace(
    '        long last = AltitudeState.lastUpdate(this);\n'
    '        if (Double.isNaN(stored) || Double.isInfinite(stored) || last <= 0L) return;\n'
    '        if (System.currentTimeMillis() - last > BARO_BOOTSTRAP_MAX_AGE_MS) return;\n',
    '        long last = AltitudeState.fixTime(this);\n'
    '        if (Double.isNaN(stored) || Double.isInfinite(stored) || last <= 0L) return;\n'
    '        if (System.currentTimeMillis() - last > BARO_BOOTSTRAP_MAX_AGE_MS) return;\n'
)
p.write_text(s)
