from pathlib import Path
import re

root = Path('/tmp/piiwii-nfc/PiiWii-NFC-1.0.18')
res = root/'app/src/main/res'
layout = res/'layout/activity_main.xml'
java = root/'app/src/main/java/ch/piiwii/nfc/MainActivity.java'
build = root/'app/build.gradle'

s = build.read_text()
s = s.replace("applicationId 'ch.piiwii.nfc.v118'", "applicationId 'ch.piiwii.nfc.v119'")
s = s.replace('versionCode 19', 'versionCode 20').replace("versionName '1.0.18'", "versionName '1.0.19'")
build.write_text(s)

icons = {
'ic_read':'M9,2A7,7 0 0,0 2,9c0,4.42 3.58,8 8,8c1.57,0 3.03,-0.46 4.26,-1.26L20.52,22 22,20.52l-6.26,-6.26A7.96,7.96 0 0,0 18,9A7,7 0 0,0 11,2M11,4A5,5 0 0,1 16,9A5,5 0 0,1 11,14A5,5 0 0,1 6,9A5,5 0 0,1 11,4Z',
'ic_write':'M3,17.25V21h3.75L17.81,9.94l-3.75,-3.75L3,17.25M20.71,7.04c0.39,-0.39 0.39,-1.03 0,-1.42l-2.34,-2.34a0.9959,0.9959 0 0,0 -1.41,0l-1.83,1.83l3.75,3.75l1.83,-1.82Z',
'ic_copy':'M16,1H4c-1.1,0 -2,0.9 -2,2v14h2V3h12V1M19,5H8c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h11c1.1,0 2,-0.9 2,-2V7c0,-1.1 -0.9,-2 -2,-2M19,21H8V7h11v14Z',
'ic_erase':'M6,19c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7H6v12M8,9h8v10H8V9M15.5,4l-1,-1h-5l-1,1H5v2h14V4Z',
'ic_home':'M10,20v-6h4v6h5v-8h3L12,3L2,12h3v8Z',
'ic_tools':'M22.7,19l-9.1,-9.1c0.9,-2.3 0.4,-5 -1.5,-6.9c-2,-2 -5,-2.4 -7.3,-1.3L9,5.9L5.9,9L1.6,4.7C0.4,7 0.9,10 2.9,12c1.9,1.9 4.6,2.4 6.9,1.5l9.1,9.1c0.4,0.4 1,0.4 1.4,0l2.3,-2.3c0.5,-0.3 0.5,-0.9 0.1,-1.3Z',
'ic_history':'M13,3a9,9 0 1,0 8.95,10H20a7,7 0 1,1 -2.05,-4.95L15,11h7V4l-2.63,2.63A8.96,8.96 0 0,0 13,3M12,7v6l5,3l1,-1.73l-4,-2.27V7Z',
'ic_settings':'M19.43,12.98c0.04,-0.32 0.07,-0.65 0.07,-0.98s-0.03,-0.66 -0.08,-0.98l2.11,-1.65c0.19,-0.15 0.24,-0.42 0.12,-0.64l-2,-3.46c-0.12,-0.22 -0.37,-0.31 -0.6,-0.22l-2.49,1c-0.52,-0.4 -1.08,-0.73 -1.69,-0.98L14.5,2.42A0.49,0.49 0 0,0 14,2h-4c-0.25,0 -0.46,0.18 -0.5,0.42L9.12,5.07c-0.61,0.25 -1.18,0.59 -1.69,0.98l-2.49,-1c-0.23,-0.08 -0.48,0 -0.6,0.22l-2,3.46c-0.13,0.22 -0.07,0.49 0.12,0.64l2.11,1.65c-0.04,0.32 -0.08,0.66 -0.08,0.98s0.03,0.66 0.08,0.98l-2.11,1.65c-0.19,0.15 -0.25,0.42 -0.12,0.64l2,3.46c0.12,0.22 0.37,0.31 0.6,0.22l2.49,-1c0.52,0.4 1.08,0.73 1.69,0.98l0.38,2.65c0.04,0.24 0.25,0.42 0.5,0.42h4c0.25,0 0.46,-0.18 0.5,-0.42l0.38,-2.65c0.61,-0.25 1.18,-0.58 1.69,-0.98l2.49,1c0.23,0.08 0.48,0 0.6,-0.22l2,-3.46c0.12,-0.22 0.07,-0.49 -0.12,-0.64l-2.11,-1.65M12,15.5A3.5,3.5 0 1,1 12,8a3.5,3.5 0 0,1 0,7.5Z',
'ic_text':'M5,4v3h5.5v13h3V7H19V4Z',
'ic_url':'M10.59,13.41a1.996,1.996 0 0,0 2.82,0l4,-4a2,2 0 0,0 -2.82,-2.82l-1.17,1.17l-1.41,-1.41l1.17,-1.17a4,4 0 0,1 5.66,5.66l-4,4a4,4 0 0,1 -5.66,0l-0.59,-0.59l1.41,-1.41l0.59,0.57M13.41,10.59a1.996,1.996 0 0,0 -2.82,0l-4,4a2,2 0 0,0 2.82,2.82l1.17,-1.17l1.41,1.41l-1.17,1.17a4,4 0 0,1 -5.66,-5.66l4,-4a4,4 0 0,1 5.66,0l0.59,0.59L14,11.16l-0.59,-0.57Z',
'ic_phone':'M6.62,10.79a15.46,15.46 0 0,0 6.59,6.59l2.2,-2.2c0.27,-0.27 0.67,-0.36 1.02,-0.24c1.12,0.37 2.33,0.57 3.57,0.57c0.55,0 1,0.45 1,1V20c0,0.55 -0.45,1 -1,1C10.61,21 3,13.39 3,4c0,-0.55 0.45,-1 1,-1h3.5c0.55,0 1,0.45 1,1c0,1.25 0.2,2.45 0.57,3.57c0.11,0.35 0.03,0.74 -0.25,1.02l-2.2,2.2Z',
'ic_email':'M20,4H4c-1.1,0 -1.99,0.9 -1.99,2L2,18c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V6c0,-1.1 -0.9,-2 -2,-2M20,8l-8,5l-8,-5V6l8,5l8,-5Z',
'ic_sms':'M20,2H4c-1.1,0 -2,0.9 -2,2v18l4,-4h14c1.1,0 2,-0.9 2,-2V4c0,-1.1 -0.9,-2 -2,-2M6,9h12v2H6V9m8,5H6v-2h8v2m4,-6H6V6h12v2Z',
'ic_contact':'M12,12c2.21,0 4,-1.79 4,-4s-1.79,-4 -4,-4s-4,1.79 -4,4s1.79,4 4,4m0,2c-2.67,0 -8,1.34 -8,4v2h16v-2c0,-2.66 -5.33,-4 -8,-4Z',
'ic_search':'M9.5,3A6.5,6.5 0 1,0 13.6,14.55L19.05,20L20.46,18.59L15,13.14A6.5,6.5 0 0,0 9.5,3m0,2A4.5,4.5 0 1,1 9.5,14A4.5,4.5 0 0,1 9.5,5Z'
}
for name,path in icons.items():
    (res/'drawable'/f'{name}.xml').write_text(f'<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24"><path android:fillColor="#F4F8FF" android:pathData="{path}"/></vector>')
for name,start,stroke in [('bg_setting_blue','#142A48','#2B78FF'),('bg_setting_purple','#241B45','#8A66FF'),('bg_setting_info','#153038','#26D6C8')]:
    (res/'drawable'/f'{name}.xml').write_text(f'<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle"><corners android:radius="20dp"/><gradient android:angle="0" android:startColor="{start}" android:endColor="#0D1727"/><stroke android:width="1dp" android:color="{stroke}"/></shape>')

s = layout.read_text()
s = s.replace('PiiWii NFC 1.0.18\\nLecture, écriture, copie et gestion de tags NDEF.','PiiWii NFC 1.0.19\\nLecture, écriture, copie et gestion de tags NDEF.')
s = s.replace('android:text="⚙"\n            android:textSize="18sp"','android:text=""\n            android:drawableTop="@drawable/ic_settings"')
for old,new,icon in [('⌕   ›\\n\\nLIRE\\nLire le contenu\\nd’un tag NFC','LIRE\\nLire le contenu d’un tag NFC','ic_read'),('✎   ›\\n\\nÉCRIRE\\nÉcrire un contenu\\nsur un tag NFC','ÉCRIRE\\nÉcrire un contenu sur un tag NFC','ic_write'),('▣   ›\\n\\nCOPIER\\nDupliquer le contenu\\nd’un tag NFC','COPIER\\nDupliquer le contenu d’un tag NFC','ic_copy'),('⌫   ›\\n\\nEFFACER\\nSupprimer le contenu\\nd’un tag NFC','EFFACER\\nSupprimer le contenu d’un tag NFC','ic_erase')]:
    s=s.replace(f'android:text="{old}"',f'android:text="{new}"\n                        android:drawableTop="@drawable/{icon}"\n                        android:drawablePadding="10dp"')
for rid,label,icon in [('typeText','Texte','ic_text'),('typeUrl','URL','ic_url'),('typePhone','Téléphone','ic_phone'),('typeEmail','E-mail','ic_email'),('typeSms','SMS','ic_sms'),('typeContact','Contact','ic_contact')]:
    s=re.sub(rf'(<RadioButton android:id="@\+id/{rid}"[^>]*?)android:text="[^"]*"',rf'\1android:text="{label}" android:drawableTop="@drawable/{icon}" android:drawablePadding="8dp"',s)
needle='                <HorizontalScrollView android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="12dp" android:scrollbars="none">'
search='''                <EditText android:id="@+id/historySearchInput" android:layout_width="match_parent" android:layout_height="54dp" android:layout_marginTop="14dp" android:hint="Rechercher dans l’historique" android:singleLine="true" android:drawableStart="@drawable/ic_search" android:drawablePadding="10dp" android:paddingStart="15dp" android:paddingEnd="15dp" android:textColor="@color/text" android:textColorHint="@color/muted" android:background="@drawable/bg_input" />\n'''+needle
idx=s.find('<!-- HISTORIQUE -->')
pos=s.find(needle,idx)
if pos<0: raise SystemExit('history filters marker missing')
s=s[:pos]+search+s[pos+len(needle):]
s=s.replace('<TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Paramètres" android:textColor="@color/text" android:textSize="24sp" android:textStyle="bold" />','<TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Paramètres" android:textColor="@color/text" android:textSize="28sp" android:textStyle="bold" />\n                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="3dp" android:text="Personnalisez le comportement de PiiWii NFC" android:textColor="@color/muted" android:textSize="13sp" />')
idx=s.find('<!-- PARAMÈTRES -->'); part=s[idx:]
for bg in ['bg_setting_blue','bg_setting_purple','bg_setting_info']: part=part.replace('android:background="@drawable/bg_card"',f'android:background="@drawable/{bg}"',1)
s=s[:idx]+part
for rid,label,icon in [('navHome','Accueil','ic_home'),('navTools','Outils','ic_tools'),('navHistory','Historique','ic_history'),('navSettings','Paramètres','ic_settings')]:
    s=re.sub(rf'(<Button android:id="@\+id/{rid}"[^>]*?)android:text="[^"]*"',rf'\1android:text="{label}" android:drawableTop="@drawable/{icon}" android:drawablePadding="4dp"',s)
layout.write_text(s)

s=java.read_text()
s=s.replace('private TextView historyCountText;\n','private TextView historyCountText;\n    private EditText historySearchInput;\n')
s=s.replace('historyCountText = findViewById(R.id.historyCountText);\n','historyCountText = findViewById(R.id.historyCountText);\n        historySearchInput = findViewById(R.id.historySearchInput);\n')
s=s.replace('        historyAll.setOnClickListener(v -> { historyFilter = "Tous"; renderHistory(); });\n','''        historySearchInput.addTextChangedListener(new TextWatcher() {\n            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }\n            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderHistory(); }\n            @Override public void afterTextChanged(Editable s) { }\n        });\n        historyAll.setOnClickListener(v -> { historyFilter = "Tous"; renderHistory(); });\n''')
s=s.replace('''        for (String line : lines) {\n            if (!"Tous".equals(historyFilter) && !line.contains(historyFilter)) continue;\n            addHistoryCard(parseHistoryEntry(line));\n            shown++;\n        }\n''','''        String query = historySearchInput == null ? "" : historySearchInput.getText().toString().trim().toLowerCase(Locale.ROOT);\n        for (String line : lines) {\n            if (!"Tous".equals(historyFilter) && !line.contains(historyFilter)) continue;\n            if (!query.isEmpty() && !line.toLowerCase(Locale.ROOT).contains(query)) continue;\n            addHistoryCard(parseHistoryEntry(line));\n            shown++;\n        }\n''')
s=s.replace('historyText.setText("Aucune opération dans ce filtre.");','historyText.setText((historySearchInput != null && historySearchInput.getText().length() > 0) ? "Aucun résultat pour cette recherche." : "Aucune opération dans ce filtre.");')
s=s.replace('card.setPadding(dp(16), dp(14), dp(16), dp(14));','card.setPadding(dp(14), dp(12), dp(14), dp(12));').replace('cardLp.bottomMargin = dp(10);','cardLp.bottomMargin = dp(8);').replace('header.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));','header.addView(icon, new LinearLayout.LayoutParams(dp(42), dp(42)));').replace('title.setTextSize(16);','title.setTextSize(15);').replace('details.setMaxLines(4);','details.setMaxLines(3);')
java.write_text(s)
