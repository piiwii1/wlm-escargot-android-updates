from pathlib import Path
root=Path('.')
layout=root/'app/src/main/res/layout/activity_main.xml'
s=layout.read_text()
repls=[
('android:layout_height="64dp"\n        android:gravity="center_vertical"','android:layout_height="60dp"\n        android:gravity="center_vertical"'),
('android:layout_width="42dp"\n            android:layout_height="42dp"','android:layout_width="40dp"\n            android:layout_height="40dp"'),
('android:textSize="22sp"\n                android:textStyle="bold"','android:textSize="21sp"\n                android:textStyle="bold"'),
('android:layout_height="156dp"','android:layout_height="146dp"'),
('android:layout_width="64dp"\n                        android:layout_height="64dp"','android:layout_width="58dp"\n                        android:layout_height="58dp"'),
('android:textSize="18sp" />','android:textSize="17sp" />'),
('android:layout_height="38dp"','android:layout_height="36dp"'),
('android:layout_height="92dp"','android:layout_height="86dp"'),
('android:text="Lire\nLe contenu d’un tag NFC"','android:text="Lire\nLire le contenu"'),
('android:text="Écrire\nUn contenu sur un tag NFC"','android:text="Écrire\nCréer un contenu"'),
('android:text="Copier\nDupliquer un tag NFC"','android:text="Copier\nDupliquer un tag"'),
('android:text="Effacer\nSupprimer le contenu"','android:text="Effacer\nVider le contenu"'),
('android:layout_height="58dp"\n                    android:layout_marginTop="10dp"','android:layout_height="54dp"\n                    android:layout_marginTop="9dp"'),
('android:minHeight="128dp"','android:minHeight="116dp"'),
('android:textSize="22sp" android:textStyle="bold"','android:textSize="21sp" android:textStyle="bold"'),
('android:textSize="16sp" android:textStyle="bold"','android:textSize="15sp" android:textStyle="bold"'),
('android:layout_height="58dp"\n        android:orientation="horizontal"','android:layout_height="56dp"\n        android:orientation="horizontal"'),
('android:layout_height="54dp" android:layout_weight="1"','android:layout_height="52dp" android:layout_weight="1"'),
('PiiWii NFC 1.0.23\nLecture, écriture, copie et gestion de tags NDEF.','PiiWii NFC 1.0.24\nLecture, écriture, copie et gestion de tags NDEF.')
]
for old,new in repls:
    s=s.replace(old,new)
layout.write_text(s)
build=root/'app/build.gradle'
b=build.read_text().replace("applicationId 'ch.piiwii.nfc.v123'","applicationId 'ch.piiwii.nfc.v124'").replace('versionCode 24','versionCode 25').replace("versionName '1.0.23'","versionName '1.0.24'")
build.write_text(b)
chg=root/'CHANGELOG.md'
chg.write_text(chg.read_text()+'''\n\n## 1.0.24\n- Micro-finition visuelle globale.\n- Header et hero Accueil légèrement allégés.\n- Tuiles Lire/Écrire/Copier/Effacer plus compactes et textes raccourcis.\n- Historique vide plus fin.\n- Titres Paramètres/Historique légèrement réduits.\n- Barre basse ramenée à 56 dp avec boutons plus discrets.\n- Aucun changement du moteur NFC.\n''')
