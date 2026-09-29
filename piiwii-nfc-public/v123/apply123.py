from pathlib import Path
root=Path('.')
layout=root/'app/src/main/res/layout/activity_main.xml'
s=layout.read_text()
old='''                <TextView\n                    android:id="@+id/statusText"\n                    android:layout_width="match_parent"\n                    android:layout_height="wrap_content"\n                    android:layout_marginTop="9dp"\n                    android:padding="10dp"\n                    android:text="Prêt. Choisis une opération."\n                    android:textColor="@color/text"\n                    android:textSize="12sp"\n                    android:background="@drawable/bg_status" />'''
new='''                <TextView\n                    android:id="@+id/statusText"\n                    android:layout_width="match_parent"\n                    android:layout_height="wrap_content"\n                    android:visibility="gone"\n                    android:text="Prêt. Choisis une opération."\n                    android:textColor="@color/text" />'''
if old not in s: raise SystemExit('status block not found')
s=s.replace(old,new,1)
s=s.replace('android:layout_marginTop="10dp"\n                    android:orientation="horizontal"\n                    android:weightSum="2">','android:layout_marginTop="8dp"\n                    android:orientation="horizontal"\n                    android:weightSum="2">',1)
s=s.replace('''                    android:minHeight="94dp"\n                    android:gravity="center"\n                    android:padding="16dp"\n                    android:text="Aucun historique.\n\nLes tags lus, écrits ou copiés apparaîtront ici."\n                    android:textColor="@color/muted"\n                    android:textSize="13sp"\n                    android:lineSpacingExtra="5dp"\n                    android:background="@drawable/bg_card" />''','''                    android:minHeight="128dp"\n                    android:gravity="center"\n                    android:padding="18dp"\n                    android:drawableTop="@drawable/ic_history"\n                    android:drawablePadding="10dp"\n                    android:text="Aucun historique\nVos lectures et écritures apparaîtront ici."\n                    android:textColor="@color/muted"\n                    android:textSize="13sp"\n                    android:lineSpacingExtra="5dp"\n                    android:background="@drawable/bg_card" />''',1)
s=s.replace('PiiWii NFC 1.0.21\nLecture, écriture, copie et gestion de tags NDEF.','PiiWii NFC 1.0.23\nLecture, écriture, copie et gestion de tags NDEF.',1)
s=s.replace('android:layout_marginTop="8dp" android:orientation="vertical" android:padding="13dp" android:background="@drawable/bg_setting_blue"','android:layout_marginTop="6dp" android:orientation="vertical" android:padding="12dp" android:background="@drawable/bg_setting_blue"',1)
s=s.replace('android:layout_height="44dp" android:layout_marginTop="9dp" android:text="Ouvrir les réglages NFC"','android:layout_height="42dp" android:layout_marginTop="8dp" android:text="Ouvrir les réglages NFC"',1)
s=s.replace('android:layout_marginTop="10dp" android:orientation="vertical" android:padding="13dp" android:background="@drawable/bg_setting_purple"','android:layout_marginTop="8dp" android:orientation="vertical" android:padding="12dp" android:background="@drawable/bg_setting_purple"',1)
s=s.replace('android:layout_height="44dp" android:layout_marginTop="6dp" android:text="Mode privé"','android:layout_height="40dp" android:layout_marginTop="5dp" android:text="Mode privé"',1)
s=s.replace('android:layout_height="44dp" android:layout_marginTop="4dp" android:text="Confirmer avant l’effacement"','android:layout_height="40dp" android:layout_marginTop="2dp" android:text="Confirmer avant l’effacement"',1)
s=s.replace('android:layout_height="44dp" android:text="Vibrations NFC"','android:layout_height="40dp" android:text="Vibrations NFC"',1)
s=s.replace('android:layout_marginTop="10dp" android:orientation="vertical" android:padding="13dp" android:background="@drawable/bg_setting_info"','android:layout_marginTop="8dp" android:orientation="vertical" android:padding="12dp" android:background="@drawable/bg_setting_info"',1)
layout.write_text(s)

java=root/'app/src/main/java/ch/piiwii/nfc/MainActivity.java'
j=java.read_text()
old='''        boolean home = "home".equals(page);\n        boolean mainNavPage = home || "tools".equals(page) || "history".equals(page) || "settings".equals(page);\n        if (headerBar != null) headerBar.setVisibility(home ? View.VISIBLE : View.GONE);\n        if (bottomNav != null) bottomNav.setVisibility(mainNavPage ? View.VISIBLE : View.GONE);\n        if (home) { screenTitle.setText("PiiWii NFC"); screenSubtitle.setText("Votre outil NFC complet et intuitif"); }\n        updateBottomNav(page);'''
new='''        boolean home = "home".equals(page);\n        boolean history = "history".equals(page);\n        boolean mainNavPage = home || "tools".equals(page) || history || "settings".equals(page);\n        if (headerBar != null) headerBar.setVisibility(home ? View.VISIBLE : View.GONE);\n        if (bottomNav != null) {\n            bottomNav.setVisibility(mainNavPage ? View.VISIBLE : View.GONE);\n            bottomNav.setWeightSum(history ? 4f : 3f);\n        }\n        if (navHistoryButton != null) navHistoryButton.setVisibility(history ? View.VISIBLE : View.GONE);\n        if (home) { screenTitle.setText("PiiWii NFC"); screenSubtitle.setText("Votre outil NFC complet et intuitif"); }\n        updateBottomNav(page);'''
if old not in j: raise SystemExit('showPage block not found')
j=j.replace(old,new,1)
java.write_text(j)

build=root/'app/build.gradle'
b=build.read_text().replace("applicationId 'ch.piiwii.nfc.v122'","applicationId 'ch.piiwii.nfc.v123'").replace('versionCode 23','versionCode 24').replace("versionName '1.0.22'","versionName '1.0.23'")
build.write_text(b)
chg=root/'CHANGELOG.md'
chg.write_text(chg.read_text()+'''\n\n## 1.0.23\n- Finition visuelle globale alignée sur la maquette de référence.\n- Navigation basse à 3 entrées sur Accueil/Outils/Paramètres et 4 entrées dans Historique.\n- Suppression du statut redondant sous le hero Accueil.\n- État vide Historique plus visuel et compact.\n- Paramètres resserrés pour réduire l’effet de gros blocs Android.\n- Harmonisation finale des espacements et proportions.\n''')
