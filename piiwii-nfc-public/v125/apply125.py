from pathlib import Path
root=Path('.')
layout=root/'app/src/main/res/layout/activity_main.xml'
s=layout.read_text()
# Bottom navigation: tighter typography/alignment without changing navigation structure.
s=s.replace('android:drawablePadding="1dp" android:textColor="@color/accent" android:textSize="9sp"', 'android:drawablePadding="0dp" android:includeFontPadding="false" android:textColor="@color/accent" android:textSize="9sp"', 1)
s=s.replace('android:drawablePadding="1dp" android:textColor="@color/muted" android:textSize="9sp"', 'android:drawablePadding="0dp" android:includeFontPadding="false" android:textColor="@color/muted" android:textSize="9sp"', 3)
# Slightly soften the home shortcut and hero status so the hierarchy matches the mockup better.
s=s.replace('android:layout_height="54dp"\n                    android:layout_marginTop="9dp"', 'android:layout_height="52dp"\n                    android:layout_marginTop="8dp"', 1)
s=s.replace('android:layout_height="36dp"\n                        android:layout_marginTop="7dp"', 'android:layout_height="34dp"\n                        android:layout_marginTop="6dp"', 1)
s=s.replace('android:textSize="12sp"\n                            android:textStyle="bold"', 'android:textSize="11sp"\n                            android:textStyle="bold"', 1)
s=s.replace('PiiWii NFC 1.0.24\nLecture, écriture, copie et gestion de tags NDEF.', 'PiiWii NFC 1.0.25\nLecture, écriture, copie et gestion de tags NDEF.', 1)
layout.write_text(s)

java=root/'app/src/main/java/ch/piiwii/nfc/MainActivity.java'
j=java.read_text()
old='''        navHomeButton.setTextColor(homeActive ? active : inactive);\n        navToolsButton.setTextColor(toolsActive ? active : inactive);\n        navHistoryButton.setTextColor(historyActive ? active : inactive);\n        navSettingsButton.setTextColor(settingsActive ? active : inactive);\n        navHomeButton.setBackgroundResource(android.R.color.transparent);\n        navToolsButton.setBackgroundResource(android.R.color.transparent);\n        navHistoryButton.setBackgroundResource(android.R.color.transparent);\n        navSettingsButton.setBackgroundResource(android.R.color.transparent);'''
new='''        tintNavButton(navHomeButton, homeActive, active, inactive);\n        tintNavButton(navToolsButton, toolsActive, active, inactive);\n        tintNavButton(navHistoryButton, historyActive, active, inactive);\n        tintNavButton(navSettingsButton, settingsActive, active, inactive);'''
if old not in j:
    raise SystemExit('updateBottomNav block not found')
j=j.replace(old,new,1)
insert='''\n    private void tintNavButton(Button button, boolean selected, int active, int inactive) {\n        if (button == null) return;\n        int color = selected ? active : inactive;\n        button.setTextColor(color);\n        button.setBackgroundResource(android.R.color.transparent);\n        android.graphics.drawable.Drawable[] drawables = button.getCompoundDrawables();\n        for (android.graphics.drawable.Drawable drawable : drawables) {\n            if (drawable != null) {\n                drawable = drawable.mutate();\n                drawable.setTint(color);\n            }\n        }\n        button.setSelected(selected);\n        button.setAlpha(selected ? 1.0f : 0.82f);\n    }\n'''
marker='\n    private void startCopyFlow() {'
if marker not in j:
    raise SystemExit('startCopyFlow marker not found')
j=j.replace(marker, insert+marker,1)
java.write_text(j)

build=root/'app/build.gradle'
b=build.read_text().replace("applicationId 'ch.piiwii.nfc.v124'","applicationId 'ch.piiwii.nfc.v125'").replace('versionCode 25','versionCode 26').replace("versionName '1.0.24'","versionName '1.0.25'")
build.write_text(b)
chg=root/'CHANGELOG.md'
chg.write_text(chg.read_text()+'''\n\n## 1.0.25\n- Finition de cohérence visuelle de la navigation basse.\n- Icônes et textes des onglets actifs/inactifs utilisent maintenant la même teinte.\n- Onglets inactifs légèrement atténués, sans gros encadré.\n- Alignement vertical de la navigation amélioré.\n- Hero NFC et raccourci Historique encore légèrement allégés.\n- Aucun changement du moteur NFC.\n''')
