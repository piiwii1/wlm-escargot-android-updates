from pathlib import Path
root=Path('.')
layout=root/'app/src/main/res/layout/activity_main.xml'
s=layout.read_text()
s=s.replace('android:orientation="vertical"\n                android:padding="18dp">','android:orientation="vertical"\n                android:padding="16dp">',1)
s=s.replace('android:paddingBottom="14dp">','android:paddingBottom="10dp">',1)
s=s.replace('android:textSize="24sp" android:textStyle="bold" />\n                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="2dp" android:text="Choisissez ce que vous voulez enregistrer"', 'android:textSize="22sp" android:textStyle="bold" />\n                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="2dp" android:text="Choisissez le contenu à enregistrer"',1)
s=s.replace('android:text="Type de contenu" android:textColor="@color/text" android:textSize="16sp"', 'android:text="Type de contenu" android:textColor="@color/text" android:textSize="14sp"',1)
for old,new in [('102dp','94dp'),('82dp','70dp'),('72dp','70dp')]:
    pre, marker, post=s.partition('\n\n                <LinearLayout\n                    android:layout_width="match_parent"')
    pre=pre.replace(old,new)
    s=pre+marker+post
s=s.replace('android:layout_marginTop="14dp"\n                    android:orientation="vertical"\n                    android:padding="18dp"', 'android:layout_marginTop="12dp"\n                    android:orientation="vertical"\n                    android:padding="16dp"',1)
s=s.replace('android:textSize="20sp" android:textStyle="bold"', 'android:textSize="18sp" android:textStyle="bold"',1)
s=s.replace('android:layout_height="150dp"', 'android:layout_height="128dp"',1)
s=s.replace('android:layout_height="110dp"', 'android:layout_height="96dp"',1)
s=s.replace('android:layout_height="58dp"\n                    android:layout_marginTop="12dp"\n                    android:text="⚙   OPTIONS AVANCÉES                                      ›"', 'android:layout_height="50dp"\n                    android:layout_marginTop="10dp"\n                    android:text="⚙   Options avancées                                      ›"',1)
s=s.replace('android:layout_height="86dp"\n                    android:layout_marginTop="14dp"', 'android:layout_height="72dp"\n                    android:layout_marginTop="12dp"',1)
s=s.replace('android:text=")))   PRÊT À ÉCRIRE\nApprochez un tag NFC pour commencer"', 'android:text="PRÊT À ÉCRIRE\nApprochez un tag NFC"',1)
s=s.replace('android:textSize="16sp"\n                    android:background="@drawable/bg_blue"', 'android:textSize="15sp"\n                    android:drawableStart="@drawable/ic_nfc"\n                    android:drawablePadding="10dp"\n                    android:background="@drawable/bg_blue"',1)
start=s.index('            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:padding="18dp">', s.index('android:id="@+id/copyPage"'))
end=s.index('            </LinearLayout>\n        </ScrollView>', start)+len('            </LinearLayout>')
new='''            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:padding="16dp">
                <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="center_vertical" android:orientation="horizontal" android:paddingBottom="12dp">
                    <Button android:id="@+id/copyBackButton" android:layout_width="44dp" android:layout_height="44dp" android:text="‹" android:textSize="26sp" android:textColor="@color/text" android:background="@drawable/bg_card_soft" android:stateListAnimator="@null" />
                    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:layout_marginStart="12dp" android:orientation="vertical">
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Copier un tag" android:textColor="@color/text" android:textSize="22sp" android:textStyle="bold" />
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="2dp" android:text="Source → Cible" android:textColor="@color/muted" android:textSize="12sp" />
                    </LinearLayout>
                </LinearLayout>
                <TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:padding="12dp" android:text="1   Lisez le tag source   →   2   Approchez le tag cible" android:textColor="@color/muted" android:textSize="12sp" android:gravity="center" android:background="@drawable/bg_card_soft" />
                <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="12dp" android:orientation="vertical" android:padding="16dp" android:background="@drawable/bg_purple">
                    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="center_vertical" android:orientation="horizontal">
                        <ImageView android:layout_width="28dp" android:layout_height="28dp" android:src="@drawable/ic_copy" android:contentDescription="Source" />
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginStart="10dp" android:text="SOURCE" android:textColor="@color/text" android:textSize="14sp" android:textStyle="bold" />
                    </LinearLayout>
                    <TextView android:id="@+id/copySourceStatus" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="8dp" android:text="En attente — approchez le tag à copier" android:textColor="@color/text" android:textSize="14sp" android:lineSpacingExtra="4dp" />
                </LinearLayout>
                <TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="6dp" android:text="↓" android:textColor="@color/blue" android:textSize="22sp" android:gravity="center" />
                <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="2dp" android:orientation="vertical" android:padding="16dp" android:background="@drawable/bg_card">
                    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="center_vertical" android:orientation="horizontal">
                        <ImageView android:layout_width="28dp" android:layout_height="28dp" android:src="@drawable/ic_nfc" android:contentDescription="Cible" />
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginStart="10dp" android:text="CIBLE" android:textColor="@color/text" android:textSize="14sp" android:textStyle="bold" />
                    </LinearLayout>
                    <TextView android:id="@+id/copyTargetStatus" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="8dp" android:text="En attente de la source" android:textColor="@color/muted" android:textSize="14sp" android:lineSpacingExtra="4dp" />
                </LinearLayout>
                <TextView android:id="@+id/copySummaryText" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="12dp" android:padding="13dp" android:text="La copie reproduit uniquement le contenu NDEF, jamais l’UID ni des clés de sécurité." android:textColor="@color/muted" android:textSize="12sp" android:background="@drawable/bg_card_soft" />
                <Button android:id="@+id/copyRestartButton" android:layout_width="match_parent" android:layout_height="52dp" android:layout_marginTop="12dp" android:text="Recommencer" android:textAllCaps="false" android:textColor="@color/text" android:background="@drawable/bg_blue" android:stateListAnimator="@null" />
            </LinearLayout>'''
s=s[:start]+new+s[end:]
layout.write_text(s)
java=root/'app/src/main/java/ch/piiwii/nfc/MainActivity.java'
j=java.read_text()
j=j.replace('Button copyRestartButton = findViewById(R.id.copyRestartButton);', 'Button copyRestartButton = findViewById(R.id.copyRestartButton);\n        Button copyBackButton = findViewById(R.id.copyBackButton);',1)
j=j.replace('copyRestartButton.setOnClickListener(v -> startCopyFlow());', 'copyRestartButton.setOnClickListener(v -> startCopyFlow());\n        copyBackButton.setOnClickListener(v -> showPage("tools"));',1)
java.write_text(j)
build=root/'app/build.gradle'
b=build.read_text().replace("applicationId 'ch.piiwii.nfc.v121'","applicationId 'ch.piiwii.nfc.v122'").replace('versionCode 22','versionCode 23').replace("versionName '1.0.21'","versionName '1.0.22'")
build.write_text(b)
(root/'CHANGELOG.md').write_text((root/'CHANGELOG.md').read_text()+'''\n\n## 1.0.22\n- Finition visuelle stricte sur les écrans Écriture et Copie.\n- Sélecteurs et formulaire Écriture plus compacts.\n- Bouton principal Écriture redimensionné et harmonisé.\n- Copie Source → Cible restructurée en deux cartes visuelles reliées.\n- Navigation retour dédiée sur l’écran de copie.\n''')