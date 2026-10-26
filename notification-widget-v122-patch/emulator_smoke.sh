#!/usr/bin/env bash
set -euo pipefail
adb install -r /tmp/widget-smoke.apk
adb logcat -c
adb shell am start -n ch.piiwii.notificationwidget/.MainActivity
sleep 6
adb shell uiautomator dump /sdcard/smoke_before.xml
adb pull /sdcard/smoke_before.xml /tmp/smoke_before.xml
python3 - <<'PY'
import xml.etree.ElementTree as ET
import re
root=ET.parse('/tmp/smoke_before.xml').getroot()
for node in root.iter('node'):
    desc=node.attrib.get('content-desc','')
    if desc.startswith('Apparence'):
        b=node.attrib.get('bounds','')
        p=list(map(int,re.findall(r'\d+',b)))
        assert len(p)==4,(desc,b)
        x=(p[0]+p[2])//2
        y=(p[1]+p[3])//2
        open('/tmp/appearance_point.txt','w').write(f'{x} {y}\n')
        print('Appearance tab:',desc,'at',x,y)
        break
else: raise RuntimeError('Appearance tab not found in activity hierarchy')
PY
read x y < /tmp/appearance_point.txt
adb shell input tap "$x" "$y"
sleep 6
pid=$(adb shell pidof ch.piiwii.notificationwidget | tr -d '\r')
test -n "$pid" || { echo 'App died on Appearance click'; adb logcat -d -t 220; exit 1; }
adb shell uiautomator dump /sdcard/smoke_after.xml
adb pull /sdcard/smoke_after.xml /tmp/smoke_after.xml
python3 - <<'PY'
import xml.etree.ElementTree as ET
root=ET.parse('/tmp/smoke_after.xml').getroot()
content=' '.join(n.attrib.get('text','')+' '+n.attrib.get('content-desc','') for n in root.iter('node'))
assert ('Galerie graphique' in content or 'Mes widgets indépendants' in content
        or 'Apparence — mode de secours' in content), content[:3000]
print('PASS: Appearance visible after clicking, normal page or guarded fallback.')
PY
if adb logcat -d -v brief | grep -E 'FATAL EXCEPTION|Process: ch.piiwii.notificationwidget' | grep -q 'Process: ch.piiwii.notificationwidget'; then
  echo 'Application FATAL EXCEPTION seen'
  adb logcat -d -t 140
  exit 1
fi
echo 'PASS: no application crash after tapping Appearance'
