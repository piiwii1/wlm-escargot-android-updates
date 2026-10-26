from pathlib import Path
import base64
root=Path("piiwii-notification-widget-v125")
main=root/"app/src/main"
manifest=main/"AndroidManifest.xml"
s=manifest.read_text()
assert 'android:label="PiiWii Notification Widget"' in s
s=s.replace('android:label="PiiWii Notification Widget"','android:label="NotiPulse"')
s=s.replace('android:label="PiiWii · 2 informations"','android:label="NotiPulse · 2 informations"')
s=s.replace('android:label="PiiWii · 3 informations"','android:label="NotiPulse · 3 informations"')
manifest.write_text(s)
strings=main/"res/values/strings.xml"
s=strings.read_text().replace("PiiWii ·","NotiPulse ·")
strings.write_text(s)
activity=main/"java/ch/piiwii/notificationwidget/MainActivity.java"
s=activity.read_text()
s=s.replace('"PiiWii Notification"','"NotiPulse"')
s=s.replace('"WIDGET  ·  1.0.24"','"WIDGET  ·  1.0.25"')
s=s.replace('PiiWii Notification Widget','NotiPulse')
s=s.replace('Widgets → PiiWii ·','Widgets → NotiPulse ·')
activity.write_text(s)
gradle=root/"app/build.gradle"
s=gradle.read_text()
assert "versionCode 25; versionName '1.0.24'" in s
gradle.write_text(s.replace("versionCode 25; versionName '1.0.24'","versionCode 26; versionName '1.0.25'"))
# Original transparent icon artwork downsampled for binary-safe transport.
# Keep package name and signing identity unchanged for in-place upgrade.
original_icon=main/"res/drawable/ic_launcher.xml"
assert original_icon.exists()
original_icon.unlink()
target=main/"res/drawable-nodpi"
target.mkdir(exist_ok=True)
(target/"ic_launcher.png").write_bytes(base64.b64decode(Path("notipulse-rebrand/logo80.png.base64").read_text().strip()))
assert (target/"ic_launcher.png").read_bytes().startswith(b'\x89PNG')
assert len((target/"ic_launcher.png").read_bytes()) > 1000
assert "applicationId 'ch.piiwii.notificationwidget'" in gradle.read_text()
print("NotiPulse 1.0.25 brand only; existing Android package, saved widgets and settings unchanged.")
