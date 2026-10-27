from pathlib import Path
from xml.etree import ElementTree as ET
import sys

root=Path(sys.argv[1]); layouts=root/'app/src/main/res/layout'
for count in (2,3):
    for orientation in ('horizontal','vertical'):
        f=layouts/f'widget_{count}_{orientation}.xml'
        before=f.read_text(encoding='utf8')
        border=' android:background="@drawable/widget_slot_background"'
        assert before.count(border)==count, (f, before.count(border))
        after=before.replace(border,'')
        assert 'widget_slot_background' not in after
        parsed=ET.fromstring(after)
        assert len(parsed)==count, f
        for item in parsed:
            assert item.attrib.get('{http://schemas.android.com/apk/res/android}background') is None
        f.write_text(after,encoding='utf8')
        print('No slot backgrounds:',f.name)

gradle=root/'app/build.gradle'
s=gradle.read_text(encoding='utf8')
old="versionCode 33; versionName '1.0.32'"
assert s.count(old)==1
s=s.replace(old,"versionCode 34; versionName '1.0.33'")
gradle.write_text(s,encoding='utf8')
# Regression: no lines in any multi-slot variant; all controls, placeholders and rest icon logic retained.
p=root/'app/src/main/java/ch/piiwii/notificationwidget/WidgetProvider.java'
w=p.read_text(encoding='utf8')
assert 'WidgetProfiles.showIdleIcon(c,widgetId)' in w
assert 'remote.setViewVisibility(panel,View.INVISIBLE)' in w
assert 'remote.setTextViewText(labels[index],"")' in w
assert '"Libre"' not in w
for filename in ['widget.xml','widget_wide.xml','widget_large.xml']:
    ET.parse(layouts/filename)
for count in (2,3):
    for orientation in ('horizontal','vertical'):
        f=layouts/f'widget_{count}_{orientation}.xml'
        assert '@drawable/widget_slot_background' not in f.read_text()
        ET.parse(f)
print('PASS: four borderless multi-slot layouts, single-slot unchanged, v1.0.33 versionCode34')
