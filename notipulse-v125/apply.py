from pathlib import Path
from xml.etree import ElementTree as ET
import base64
root=Path("piiwii-notification-widget-v125")
main=root/"app/src/main"
pkg=main/"java/ch/piiwii/notificationwidget"
def exact(path,old,new):
 s=path.read_text(encoding="utf-8")
 assert s.count(old)==1,(str(path),old[:65],s.count(old))
 path.write_text(s.replace(old,new,1),encoding="utf-8")
exact(root/"app/build.gradle","versionCode 25; versionName '1.0.24'","versionCode 26; versionName '1.0.25'")
manifest=main/"AndroidManifest.xml"
s=manifest.read_text(encoding="utf-8")
assert s.count('android:icon="@drawable/ic_launcher"')==1
s=s.replace('android:icon="@drawable/ic_launcher"','android:icon="@mipmap/ic_launcher" android:roundIcon="@mipmap/ic_launcher"')
s=s.replace('android:label="PiiWii Notification Widget"','android:label="NotiPulse"')
s=s.replace('PiiWii · ','NotiPulse · ')
manifest.write_text(s,encoding="utf-8")
strings=main/"res/values/strings.xml"
strings.write_text(strings.read_text().replace("PiiWii · ","NotiPulse · "),encoding="utf-8")
activity=pkg/"MainActivity.java"
exact(activity,'    TextView mark=label("◉",31,blue,true);brand.addView(mark,new LinearLayout.LayoutParams(d(45),d(46)));',
'''    ImageView mark=new ImageView(this);mark.setImageResource(R.drawable.notipulse_logo);
    mark.setScaleType(ImageView.ScaleType.FIT_CENTER);
    mark.setContentDescription("Icône NotiPulse");
    brand.addView(mark,new LinearLayout.LayoutParams(d(45),d(46)));''')
exact(activity,'info.addView(label("PiiWii Notification",21,white,true));','info.addView(label("NotiPulse",21,white,true));')
exact(activity,'info.addView(label("WIDGET  ·  1.0.24",11,muted,true));','info.addView(label("NOTIFICATIONS  ·  1.0.25",11,muted,true));')
s=activity.read_text(encoding="utf-8").replace("PiiWii Notification Widget","NotiPulse").replace("PiiWii · ","NotiPulse · ")
s=s.replace("NotiPulse 1.0.15 — Diagnostic","NotiPulse 1.0.25 — Diagnostic")
s=s.replace("Contrôle de la bêta — 1.0.15","Contrôle de la bêta — 1.0.25")
activity.write_text(s,encoding="utf-8")
for f in (main/"res/layout").glob("*.xml"):
 s=f.read_text(encoding="utf-8")
 if "PiiWii" in s:f.write_text(s.replace("PiiWii","NotiPulse"),encoding="utf-8")
settings=root/"settings.gradle"
settings.write_text(settings.read_text().replace("rootProject.name='PiiWiiNotificationWidget'","rootProject.name='NotiPulse'"))
src=Path("notipulse-v125/icon.webp.b64")
raw=base64.b64decode(src.read_text().strip(),validate=True)
assert raw[:4]==b'RIFF' and raw[8:12]==b'WEBP'
assert len(raw)==9324,len(raw)
target=main/"res/drawable/notipulse_logo.webp";target.write_bytes(raw)
adaptive=main/"res/mipmap-anydpi-v26";adaptive.mkdir(parents=True,exist_ok=True)
(adaptive/"ic_launcher.xml").write_text('''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
 <background android:drawable="@android:color/transparent"/>
 <foreground android:drawable="@drawable/notipulse_launcher_foreground"/>
</adaptive-icon>
''')
(main/"res/drawable/notipulse_launcher_foreground.xml").write_text('''<?xml version="1.0" encoding="utf-8"?>
<inset xmlns:android="http://schemas.android.com/apk/res/android" android:drawable="@drawable/notipulse_logo" android:inset="15dp"/>
''')
readme=root/"README.md"
readme.write_text("# NotiPulse 1.0.25 — nouveau nom et icône transparente\nLes widgets, données utilisateurs, autorisations, applicationId et préférences ne sont pas modifiés.\n\n"+readme.read_text())
for f in [manifest,strings,adaptive/"ic_launcher.xml",main/"res/drawable/notipulse_launcher_foreground.xml"]: ET.parse(f)
assert "applicationId 'ch.piiwii.notificationwidget'" in (root/"app/build.gradle").read_text()
assert "piiwii_notification_widget" in (pkg/"Categories.java").read_text()
assert all(t in activity.read_text() for t in ("if(tab==0)showHome();","else if(tab==1)showCategories();","else if(tab==2)showAppearanceGuarded();","else showTests();"))
print("BRANDING PASS: NotiPulse package-safe update, transparent adaptive launcher, 4 pages, 3 widget types")
