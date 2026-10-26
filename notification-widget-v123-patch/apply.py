from pathlib import Path
from xml.etree import ElementTree
import shutil
source=Path("notification-widget-v123-patch/drawable")
target=Path("piiwii-notification-widget-v123/app/src/main/res/drawable")
expected=("pic_brand_whatsapp.xml","pic_charge_induction.xml","pic_charge_induction_fast.xml")
for filename in expected:
    data=(source/filename).read_text(encoding="utf-8")
    root=ElementTree.fromstring(data)
    assert root.tag=="vector",filename
    assert len(root)==(3 if filename=="pic_brand_whatsapp.xml" else (8 if filename.endswith("_fast.xml") else 7)),(filename,len(root))
    shutil.copy2(source/filename,target/filename)
    assert (target/filename).read_bytes()==(source/filename).read_bytes()
gradle=Path("piiwii-notification-widget-v123/app/build.gradle")
data=gradle.read_text()
old="versionCode 23; versionName '1.0.22'"
assert data.count(old)==1
gradle.write_text(data.replace(old,"versionCode 24; versionName '1.0.23'"))
java=Path("piiwii-notification-widget-v123/app/src/main/java/ch/piiwii/notificationwidget/GraphicIcons.java").read_text()
assert 'case "whatsapp": return R.drawable.pic_brand_whatsapp;' in java
assert 'case "induction": return R.drawable.pic_charge_induction;' in java
assert 'case "induction_fast": return R.drawable.pic_charge_induction_fast;' in java
assert 'if(!colorful(key))d.setTint(tint);' in java
main=Path("piiwii-notification-widget-v123/app/src/main/java/ch/piiwii/notificationwidget/MainActivity.java").read_text()
for token in ("if(tab==0)showHome();","else if(tab==1)showCategories();","else if(tab==2)showAppearanceGuarded();","else showTests();","void showInlineIconManager()"):assert token in main
print("PASS: 3 vector designs, matching drawable mappings, safe Appearance and 4-page navigation")
