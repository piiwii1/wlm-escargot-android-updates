from pathlib import Path

root = Path('/tmp/piiwii-nfc/PiiWii-NFC-1.0.4')
p = root / 'app/src/main/java/ch/piiwii/nfc/MainActivity.java'
s = p.read_text()

s = s.replace('import android.content.Intent;\n', 'import android.content.Intent;\nimport android.net.Uri;\n')
s = s.replace('import java.io.ByteArrayOutputStream;\n', 'import java.io.ByteArrayOutputStream;\nimport java.io.InputStream;\nimport java.io.OutputStream;\n')
s = s.replace('    private byte[] copySourceUid;\n', '    private byte[] copySourceUid;\n    private NdefMessage lastReadMessage;\n    private String lastReadUid = "";\n    private static final int REQUEST_EXPORT_NDEF = 4101;\n    private static final int REQUEST_IMPORT_NDEF = 4102;\n')
s = s.replace('        Button clearHistoryButton = findViewById(R.id.clearHistoryButton);\n', '        Button clearHistoryButton = findViewById(R.id.clearHistoryButton);\n        Button reuseLastButton = findViewById(R.id.reuseLastButton);\n        Button exportNdefButton = findViewById(R.id.exportNdefButton);\n        Button importNdefButton = findViewById(R.id.importNdefButton);\n')

anchor = '''        eraseButton.setOnClickListener(v -> new AlertDialog.Builder(this)\n                .setTitle("Effacer le tag ?")\n                .setMessage("Le contenu NDEF du prochain tag compatible sera effacé.")\n                .setNegativeButton("Annuler", null)\n                .setPositiveButton("Continuer", (d, w) -> setMode(Mode.ERASE, "Approche le tag à effacer."))\n                .show());\n\n'''
insert = anchor + '''        reuseLastButton.setOnClickListener(v -> reuseLastReadInEditor());\n        exportNdefButton.setOnClickListener(v -> exportLastNdef());\n        importNdefButton.setOnClickListener(v -> importNdef());\n\n'''
if anchor not in s:
    raise SystemExit('listener anchor missing')
s = s.replace(anchor, insert, 1)

needle = '''            NdefMessage message = ndef.getNdefMessage();\n            String decoded = decodeMessage(message);\n'''
replacement = '''            NdefMessage message = ndef.getNdefMessage();\n            if (message != null) {\n                lastReadMessage = cloneMessage(message);\n                lastReadUid = toHex(tag.getId());\n            }\n            String decoded = decodeMessage(message);\n'''
if needle not in s:
    raise SystemExit('read anchor missing')
s = s.replace(needle, replacement, 1)

needle = '''            copyBuffer = cloneMessage(message);\n            copySourceUid = tag.getId() == null ? null : tag.getId().clone();\n'''
replacement = '''            copyBuffer = cloneMessage(message);\n            lastReadMessage = cloneMessage(message);\n            lastReadUid = toHex(tag.getId());\n            copySourceUid = tag.getId() == null ? null : tag.getId().clone();\n'''
if needle not in s:
    raise SystemExit('copy anchor missing')
s = s.replace(needle, replacement, 1)

marker = '\n\n    private NdefMessage buildCurrentMessage(String value) {\n'
methods = r'''

    private void exportLastNdef() {
        if (lastReadMessage == null) {
            Toast.makeText(this, "Lis d’abord un tag NDEF à sauvegarder.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        String suffix = TextUtils.isEmpty(lastReadUid) ? "tag" : lastReadUid.replace(":", "-");
        intent.putExtra(Intent.EXTRA_TITLE, "PiiWii-NFC-" + suffix + ".ndef");
        startActivityForResult(intent, REQUEST_EXPORT_NDEF);
    }

    private void importNdef() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_IMPORT_NDEF);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == REQUEST_EXPORT_NDEF) {
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null) throw new IllegalStateException("Flux de sortie indisponible");
                out.write(lastReadMessage.toByteArray());
                out.flush();
                statusText.setText("Sauvegarde .ndef créée avec succès.");
                addHistory("Export NDEF", lastReadUid, lastReadMessage.toByteArray().length + " octets");
            } catch (Exception e) {
                statusText.setText("Échec de l’export : " + safeMessage(e));
            }
        } else if (requestCode == REQUEST_IMPORT_NDEF) {
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) throw new IllegalStateException("Fichier inaccessible");
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] chunk = new byte[4096];
                int read;
                while ((read = in.read(chunk)) != -1) {
                    buffer.write(chunk, 0, read);
                    if (buffer.size() > 1024 * 1024) throw new IllegalArgumentException("Fichier trop volumineux");
                }
                byte[] bytes = buffer.toByteArray();
                NdefMessage imported = new NdefMessage(bytes);
                if (imported.getRecords().length == 0) throw new IllegalArgumentException("Aucun enregistrement NDEF");
                copyBuffer = cloneMessage(imported);
                copySourceUid = null;
                lastReadMessage = cloneMessage(imported);
                lastReadUid = "FICHIER";
                mode = Mode.COPY_TARGET;
                tagInfoText.setText("Fichier NDEF chargé\nEnregistrements : " + imported.getRecords().length
                        + "\nTaille : " + bytes.length + " octets\n\nContenu :\n" + decodeMessage(imported));
                statusText.setText("Fichier .ndef chargé. Approche le tag CIBLE pour l’écrire.");
                updateCancelVisibility();
                addHistory("Import NDEF", "FICHIER", summarize(decodeMessage(imported)));
            } catch (Exception e) {
                statusText.setText("Fichier NDEF invalide : " + safeMessage(e));
            }
        }
    }

    private void reuseLastReadInEditor() {
        if (lastReadMessage == null || lastReadMessage.getRecords().length == 0) {
            Toast.makeText(this, "Aucun contenu NDEF récent à réutiliser.", Toast.LENGTH_SHORT).show();
            return;
        }
        NdefRecord r = lastReadMessage.getRecords()[0];
        try {
            if (r.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(r.getType(), NdefRecord.RTD_TEXT)) {
                typeGroup.check(R.id.typeText);
                payloadInput.setText(decodeTextValue(r));
            } else if (r.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(r.getType(), NdefRecord.RTD_URI)) {
                String uri = String.valueOf(r.toUri());
                if (uri.startsWith("tel:")) {
                    typeGroup.check(R.id.typePhone);
                    payloadInput.setText(uri.substring(4));
                } else if (uri.startsWith("mailto:")) {
                    typeGroup.check(R.id.typeEmail);
                    payloadInput.setText(uri.substring(7));
                } else if (uri.startsWith("sms:") || uri.startsWith("smsto:")) {
                    typeGroup.check(R.id.typeSms);
                    payloadInput.setText(uri.substring(uri.indexOf(':') + 1));
                } else {
                    typeGroup.check(R.id.typeUrl);
                    payloadInput.setText(uri);
                }
            } else {
                Toast.makeText(this, "Le premier enregistrement ne peut pas être converti dans l’éditeur simple.", Toast.LENGTH_LONG).show();
                return;
            }
            payloadInput.requestFocus();
            payloadInput.setSelection(payloadInput.getText().length());
            statusText.setText("Dernier contenu chargé dans l’éditeur. Tu peux le modifier avant écriture.");
        } catch (Exception e) {
            statusText.setText("Impossible de réutiliser ce contenu : " + safeMessage(e));
        }
    }

    private String decodeTextValue(NdefRecord r) {
        byte[] payload = r.getPayload();
        if (payload == null || payload.length == 0) return "";
        boolean utf16 = (payload[0] & 0x80) != 0;
        int langLen = payload[0] & 0x3F;
        int start = 1 + langLen;
        if (start > payload.length) return "";
        Charset cs = utf16 ? StandardCharsets.UTF_16 : StandardCharsets.UTF_8;
        return new String(payload, start, payload.length - start, cs);
    }
'''
if marker not in s:
    raise SystemExit('method marker missing')
s = s.replace(marker, methods + marker, 1)
p.write_text(s)

x = root / 'app/src/main/res/layout/activity_main.xml'
xs = x.read_text()
anchor = '''        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="24dp"
            android:text="Dernier tag"
'''
block = '''        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="20dp"
            android:text="Sauvegarde / reprise"
            android:textColor="@color/text"
            android:textSize="18sp"
            android:textStyle="bold" />

        <Button
            android:id="@+id/reuseLastButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:text="RÉUTILISER LE DERNIER CONTENU" />

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:orientation="horizontal"
            android:weightSum="2">

            <Button
                android:id="@+id/exportNdefButton"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="EXPORTER .NDEF" />

            <Button
                android:id="@+id/importNdefButton"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginStart="10dp"
                android:layout_weight="1"
                android:text="IMPORTER .NDEF" />
        </LinearLayout>

'''
if anchor not in xs:
    raise SystemExit('xml anchor missing')
xs = xs.replace(anchor, block + anchor, 1)
xs = xs.replace('PiiWii NFC 1.0.3 •', 'PiiWii NFC 1.0.4 •')
x.write_text(xs)

b = root / 'app/build.gradle'
bs = b.read_text().replace('versionCode 4', 'versionCode 5').replace("versionName '1.0.3'", "versionName '1.0.4'")
b.write_text(bs)

r = root / 'README.md'
rs = r.read_text().replace('# PiiWii NFC 1.0.3', '# PiiWii NFC 1.0.4').replace('- versionName: 1.0.3', '- versionName: 1.0.4').replace('- versionCode: 4', '- versionCode: 5')
r.write_text(rs)

c = root / 'CHANGELOG.md'
cs = c.read_text()
cs += "\n## 1.0.4\n- Export du dernier message NDEF vers un fichier .ndef.\n- Import d'un fichier .ndef puis écriture directe sur un tag cible.\n- Réutilisation du dernier contenu lu dans l'éditeur.\n- Garde-fou de taille (1 Mo) lors de l'import.\n"
c.write_text(cs)
