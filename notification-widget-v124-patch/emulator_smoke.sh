#!/usr/bin/env bash
set -euo pipefail
adb install -r /tmp/widget-smoke.apk
adb logcat -c
adb shell am start -n ch.piiwii.notificationwidget/.MainActivity
sleep 5
adb shell uiautomator dump /sdcard/smoke_before.xml
adb pull /sdcard/smoke_before.xml /tmp/smoke_before.xml >/dev/null
python3 - <<'PY'
import xml.etree.ElementTree as ET,re
root=ET.parse('/tmp/smoke_before.xml').getroot()
for n in root.iter('node'):
  if n.attrib.get('content-desc','').startswith('Apparence'):
    p=list(map(int,re.findall(r'\d+',n.attrib['bounds'])))
    open('/tmp/appearance_point.txt','w').write(f'{(p[0]+p[2])//2} {(p[1]+p[3])//2}\n')
    print('Found Appearance navigation',p)
    break
else:raise RuntimeError('Appearance tab not found')
PY
read x y < /tmp/appearance_point.txt
adb shell input tap "$x" "$y"
sleep 4
found=0
for attempt in 1 2 3 4 5 6; do
  adb shell uiautomator dump /sdcard/appearance.xml >/dev/null
  adb pull /sdcard/appearance.xml /tmp/appearance.xml >/dev/null
  if python3 - <<'PY'
import xml.etree.ElementTree as ET,re
root=ET.parse('/tmp/appearance.xml').getroot()
for n in root.iter('node'):
  if 'Personnaliser mes pictogrammes' in (n.attrib.get('text','')+' '+n.attrib.get('content-desc','')):
    p=list(map(int,re.findall(r'\d+',n.attrib['bounds'])))
    open('/tmp/icons_point.txt','w').write(f'{(p[0]+p[2])//2} {(p[1]+p[3])//2}\n')
    print('Found pictogram manager',p)
    break
else:raise SystemExit(1)
PY
  then found=1; break;fi
  adb shell input swipe 500 1500 500 600 380
  sleep 1
done
test "$found" -eq 1 || { echo 'Pictogram manager button not found';exit 1; }
read x y < /tmp/icons_point.txt
adb shell input tap "$x" "$y"
sleep 4
pid=$(adb shell pidof ch.piiwii.notificationwidget | tr -d '\r')
test -n "$pid" || { echo 'App crashed after icon manager opening';adb logcat -d -t 220;exit 1; }
adb shell uiautomator dump /sdcard/icons_window.xml >/dev/null
adb pull /sdcard/icons_window.xml /tmp/icons_window.xml >/dev/null
python3 - <<'PY'
import xml.etree.ElementTree as ET
root=ET.parse('/tmp/icons_window.xml').getroot()
content=' '.join(n.attrib.get('text','')+' '+n.attrib.get('content-desc','') for n in root.iter('node'))
for phrase in ['Mes pictogrammes','12 sur 38 réactions affichées','Changer','Rechercher une réaction']:
  assert phrase in content,(phrase,content[:5000])
print('PASS: 1.0.24 Appearance and lazy icon editor opened with 12 of 38 visible')
PY
if adb logcat -d -v brief | grep -E 'FATAL EXCEPTION|Process: ch.piiwii.notificationwidget' | grep -q 'Process: ch.piiwii.notificationwidget';then
  echo 'FATAL EXCEPTION in application';exit 1
fi
echo 'PASS: no crash in Appearance or icon manager'
