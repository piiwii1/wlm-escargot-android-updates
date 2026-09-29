from pathlib import Path
import re
root=Path('.')
layout=root/'app/src/main/res/layout/activity_main.xml'
s=layout.read_text()
# Header compact
s=s.replace('android:layout_height="72dp"\n        android:gravity="center_vertical"','android:layout_height="64dp"\n        android:gravity="center_vertical"',1)
s=s.replace('android:layout_width="48dp"\n            android:layout_height="48dp"','android:layout_width="42dp"\n            android:layout_height="42dp"',1)
s=s.replace('android:textSize="24sp"\n                android:textStyle="bold"','android:textSize="22sp"\n                android:textStyle="bold"',1)
# Home compact
s=s.replace('android:layout_height="268dp"','android:layout_height="220dp"',1)
s=s.replace('android:layout_width="132dp"\n                        android:layout_height="132dp"','android:layout_width="84dp"\n                        android:layout_height="84dp"',1)
s=s.replace('android:padding="24dp"','android:padding="14dp"',1)
s=s.replace('android:padding="18dp"\n                    android:background="@drawable/bg_hero"','android:padding="12dp"\n                    android:background="@drawable/bg_hero"',1)
s=s.replace('android:layout_height="58dp"\n                        android:layout_marginTop="15dp"','android:layout_height="46dp"\n                        android:layout_marginTop="10dp"',1)
s=s.replace('android:layout_height="142dp"','android:layout_height="112dp"',4)
s=s.replace('android:layout_height="92dp"','android:layout_height="74dp"',1)
s=s.replace('android:text="⌫   ›\\n\\nEFFACER\\nSupprimer le contenu\\nd’un tag"','android:text="EFFACER\\nSupprimer le contenu d’un tag"\n                        android:drawableTop="@drawable/ic_erase"\n                        android:drawablePadding="8dp"')
# Tools duplicate title/subtitle removal
s=s.replace('''                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Outils NFC" android:textColor="@color/text" android:textSize="24sp" android:textStyle="bold" />\n                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="3dp" android:text="Copie, effacement, sauvegarde et reprise" android:textColor="@color/muted" android:textSize="13sp" />\n''','')
s=s.replace('android:layout_marginTop="16dp" android:text="▣   COPIER UN TAG\\nLire une source puis écrire sur une cible"','android:layout_marginTop="8dp" android:text="COPIER UN TAG\\nLire une source puis écrire sur une cible" android:drawableTop="@drawable/ic_copy" android:drawablePadding="6dp"')
s=s.replace('android:text="⌫   EFFACER UN TAG\\nSupprimer son contenu NDEF"','android:text="EFFACER UN TAG\\nSupprimer son contenu NDEF" android:drawableTop="@drawable/ic_erase" android:drawablePadding="6dp"')
s=s.replace('android:layout_height="82dp"', 'android:layout_height="72dp"', 2)
# History duplicate heading section removal
old='''                <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="center_vertical" android:orientation="horizontal">\n                    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical">\n                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Historique" android:textColor="@color/text" android:textSize="26sp" android:textStyle="bold" />\n                        <TextView android:id="@+id/historyCountText" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="2dp" android:text="0 opération" android:textColor="@color/muted" android:textSize="12sp" />\n                    </LinearLayout>\n                    <Button android:id="@+id/clearHistoryButton" android:layout_width="wrap_content" android:layout_height="46dp" android:text="VIDER" android:textColor="@color/text" android:background="@drawable/bg_card_soft" android:stateListAnimator="@null" />\n                </LinearLayout>'''
new='''                <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="center_vertical" android:orientation="horizontal">\n                    <TextView android:id="@+id/historyCountText" android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:text="0 opération" android:textColor="@color/muted" android:textSize="12sp" />\n                    <Button android:id="@+id/clearHistoryButton" android:layout_width="wrap_content" android:layout_height="40dp" android:text="VIDER" android:textColor="@color/text" android:background="@drawable/bg_card_soft" android:stateListAnimator="@null" />\n                </LinearLayout>'''
if old not in s: print('WARN history header not matched')
else: s=s.replace(old,new,1)
# Replace filter horizontal scroll by 2 rows
pat=re.compile(r'''                <HorizontalScrollView android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="12dp" android:scrollbars="none">.*?</HorizontalScrollView>''',re.S)
rep='''                <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="10dp" android:orientation="vertical">\n                    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="horizontal" android:weightSum="3">\n                        <Button android:id="@+id/historyAll" android:layout_width="0dp" android:layout_height="40dp" android:layout_weight="1" android:text="Tous" android:textColor="@color/text" android:background="@drawable/bg_blue" android:stateListAnimator="@null" />\n                        <Button android:id="@+id/historyReads" android:layout_width="0dp" android:layout_height="40dp" android:layout_weight="1" android:layout_marginStart="6dp" android:text="Lectures" android:textColor="@color/text" android:background="@drawable/bg_chip" android:stateListAnimator="@null" />\n                        <Button android:id="@+id/historyWrites" android:layout_width="0dp" android:layout_height="40dp" android:layout_weight="1" android:layout_marginStart="6dp" android:text="Écritures" android:textColor="@color/text" android:background="@drawable/bg_chip" android:stateListAnimator="@null" />\n                    </LinearLayout>\n                    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="6dp" android:orientation="horizontal" android:weightSum="2">\n                        <Button android:id="@+id/historyCopies" android:layout_width="0dp" android:layout_height="40dp" android:layout_weight="1" android:text="Copies" android:textColor="@color/text" android:background="@drawable/bg_chip" android:stateListAnimator="@null" />\n                        <Button android:id="@+id/historyErase" android:layout_width="0dp" android:layout_height="40dp" android:layout_weight="1" android:layout_marginStart="6dp" android:text="Effacements" android:textColor="@color/text" android:background="@drawable/bg_chip" android:stateListAnimator="@null" />\n                    </LinearLayout>\n                </LinearLayout>'''
s,n=pat.subn(rep,s,count=1)
if n!=1: print('WARN filters replacement',n)
# Settings duplicate heading removal
s=s.replace('''                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Paramètres" android:textColor="@color/text" android:textSize="28sp" android:textStyle="bold" />\n                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="3dp" android:text="Personnalisez le comportement de PiiWii NFC" android:textColor="@color/muted" android:textSize="13sp" />\n''','')
s=s.replace('android:layout_marginTop="14dp" android:orientation="vertical" android:padding="16dp" android:background="@drawable/bg_setting_blue"','android:layout_marginTop="6dp" android:orientation="vertical" android:padding="14dp" android:background="@drawable/bg_setting_blue"',1)
s=s.replace('android:padding="16dp" android:background="@drawable/bg_setting_purple"','android:padding="14dp" android:background="@drawable/bg_setting_purple"',1)
s=s.replace('android:padding="16dp" android:background="@drawable/bg_setting_info"','android:padding="14dp" android:background="@drawable/bg_setting_info"',1)
s=s.replace('android:text="PiiWii NFC 1.0.19\\nLecture, écriture, copie et gestion de tags NDEF."','android:text="PiiWii NFC 1.0.20\\nLecture, écriture, copie et gestion de tags NDEF."')
# tint switches
s=s.replace('android:textColor="@color/text" />','android:textColor="@color/text" android:thumbTint="@color/switch_thumb" android:trackTint="@color/switch_track" />',1)
s=s.replace('android:textColor="@color/text" android:checked="true" />','android:textColor="@color/text" android:checked="true" android:thumbTint="@color/switch_thumb" android:trackTint="@color/switch_track" />',2)
# Bottom nav compact
s=s.replace('android:layout_height="82dp"\n        android:orientation="horizontal"','android:layout_height="68dp"\n        android:orientation="horizontal"',1)
for nav_id in ['navHome','navTools','navHistory','navSettings']:
    s=re.sub(r'(<Button android:id="@\+id/'+nav_id+r'"[^>]*?android:layout_height=")\d+dp("[^>]*>)', r'\g<1>56dp\2', s, count=1)
    s=re.sub(r'(<Button android:id="@\+id/'+nav_id+r'"[^>]*?android:layout_margin=")\d+dp("[^>]*>)', r'\g<1>3dp\2', s, count=1)
    s=re.sub(r'(<Button android:id="@\+id/'+nav_id+r'"[^>]*?android:drawablePadding=")\d+dp("[^>]*>)', r'\g<1>2dp\2', s, count=1)
    s=re.sub(r'(<Button android:id="@\+id/'+nav_id+r'"[^>]*?android:textSize=")\d+sp("[^>]*>)', r'\g<1>10sp\2', s, count=1)
layout.write_text(s)
# version/build
b=root/'app/build.gradle'
t=b.read_text().replace("applicationId 'ch.piiwii.nfc.v119'","applicationId 'ch.piiwii.nfc.v120'").replace('versionCode 20','versionCode 21').replace("versionName '1.0.19'","versionName '1.0.20'")
b.write_text(t)
# switch colors
colors=root/'app/src/main/res/color'; colors.mkdir(exist_ok=True)
(colors/'switch_thumb.xml').write_text('''<?xml version="1.0" encoding="utf-8"?>\n<selector xmlns:android="http://schemas.android.com/apk/res/android">\n    <item android:state_checked="true" android:color="#20B8FF"/>\n    <item android:color="#B8C4D7"/>\n</selector>\n''')
(colors/'switch_track.xml').write_text('''<?xml version="1.0" encoding="utf-8"?>\n<selector xmlns:android="http://schemas.android.com/apk/res/android">\n    <item android:state_checked="true" android:color="#355F84"/>\n    <item android:color="#455269"/>\n</selector>\n''')
