from pathlib import Path
import re

ROOT = Path('mode-convoi-clean/android/app/src/main/java/ch/piiwii/modeconvoi')
main = ROOT / 'MainActivity.java'
service = ROOT / 'LocationShareService.java'
gradle = Path('mode-convoi-clean/android/app/build.gradle')

# This maintenance refactor is intentionally idempotent: it transforms the canonical
# source once, then future builds compile the canonical source directly.

# --- MainActivity: extract participant defaults and cache vehicle images ---
s = main.read_text()
if 'ParticipantDefaults.ensure(prefs);' not in s:
    s, n = re.subn(r'\bensureParticipantDefaults\(\);', 'ParticipantDefaults.ensure(prefs);', s, count=1)
    if n != 1:
        raise SystemExit(f'participant defaults call replacement count={n}')

if 'private void ensureParticipantDefaults' in s:
    pattern = re.compile(r'\n    private void ensureParticipantDefaults\(\)\{.*?\n    private boolean saveProfileChecked', re.S)
    s, n = pattern.subn('\n    private boolean saveProfileChecked', s, count=1)
    if n != 1:
        raise SystemExit(f'participant defaults block removal count={n}')

old_decode = 'try{byte[] raw=Base64.decode(image,Base64.DEFAULT);Bitmap b=BitmapFactory.decodeByteArray(raw,0,raw.length);if(b!=null){ImageView iv=new ImageView(this);'
new_decode = 'try{Bitmap b=VehicleImageCache.decode(image);if(b!=null){ImageView iv=new ImageView(this);'
if old_decode in s:
    s = s.replace(old_decode, new_decode, 1)
elif 'VehicleImageCache.decode(image)' not in s:
    raise SystemExit('vehicle image decode block not found')
main.write_text(s)

# --- LocationShareService: batch one GPS fix into one SharedPreferences transaction ---
t = service.read_text()
old_gps = '''        prefs.put(PREF_GPS_LAT, String.valueOf(loc.getLatitude()));
        prefs.put(PREF_GPS_LON, String.valueOf(loc.getLongitude()));
        prefs.put(PREF_GPS_ACC, String.valueOf(loc.hasAccuracy()?loc.getAccuracy():-1));
        prefs.putLong(PREF_GPS_FIX_AT, t);
        prefs.remove(PREF_GPS_ERROR);'''
new_gps = '        prefs.putLocationFix(loc.getLatitude(), loc.getLongitude(), loc.hasAccuracy()?loc.getAccuracy():-1, t);'
if old_gps in t:
    t = t.replace(old_gps, new_gps, 1)
elif 'prefs.putLocationFix(' not in t:
    raise SystemExit('GPS preference block not found')
service.write_text(t)

# --- Version ---
g = gradle.read_text()
g = re.sub(r'versionCode\s+\d+', 'versionCode 29', g)
g = re.sub(r"versionName\s+'[^']+'", "versionName '0.3.26'", g)
gradle.write_text(g)

# Safety checks: no user-facing feature change.
main_text = main.read_text()
service_text = service.read_text()
assert 'ParticipantDefaults.ensure(prefs);' in main_text
assert 'private void ensureParticipantDefaults' not in main_text
assert 'VehicleImageCache.decode(image)' in main_text
assert 'ConvoyEventProcessor.process(this,prefs,s);' in main_text
assert 'private void processEvents()' not in main_text
assert 'prefs.putLocationFix(' in service_text
assert 'startVoicePolling' not in service_text
assert 'pollVoice' not in service_text
assert 'ConvoyEventProcessor.process(this,prefs,snapshot);' in service_text
assert "versionName '0.3.26'" in gradle.read_text()
print('0.3.26 structural refactor applied successfully')
