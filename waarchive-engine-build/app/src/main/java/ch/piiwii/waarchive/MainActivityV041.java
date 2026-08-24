package ch.piiwii.waarchive;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivityV041 extends MainActivity {
    private static final int PICK_REAL_ARCHIVE = 1001;
    private static final String DB_NAME = "archive.db";

    private static final Pattern P1 = Pattern.compile("^\\[?(\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4})[, ]+(?:à\\s*)?(\\d{1,2}:\\d{2}(?::\\d{2})?(?:\\s*[AP]M)?)\\]?\\s*[-–—]\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern P2 = Pattern.compile("^\\[(\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4})[, ]+(?:à\\s*)?(\\d{1,2}:\\d{2}(?::\\d{2})?(?:\\s*[AP]M)?)\\]\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern P3 = Pattern.compile("^(\\d{4}[./-]\\d{1,2}[./-]\\d{1,2})[, ]+(\\d{1,2}:\\d{2}(?::\\d{2})?(?:\\s*[AP]M)?)\\s*[-–—]\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern P4 = Pattern.compile("^(\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4})\\s+(\\d{1,2}:\\d{2}(?::\\d{2})?(?:\\s*[AP]M)?)\\s*[-–—]\\s*(.*)$", Pattern.CASE_INSENSITIVE);

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        replaceImportButton(getWindow().getDecorView());
    }

    private boolean replaceImportButton(View v) {
        if (v instanceof Button) {
            Button b = (Button) v;
            CharSequence t = b.getText();
            if (t != null && t.toString().toLowerCase(Locale.ROOT).contains("ouvrir mon export")) {
                b.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) { chooseAnyArchive(); }
                });
                return true;
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) if (replaceImportButton(g.getChildAt(i))) return true;
        }
        return false;
    }

    private void chooseAnyArchive() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/zip", "application/x-zip-compressed", "application/octet-stream", "text/plain"
        });
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, PICK_REAL_ARCHIVE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != PICK_REAL_ARCHIVE) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;

        final Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}

        Toast.makeText(this, "Analyse de l’export WhatsApp…", Toast.LENGTH_LONG).show();
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    long n = robustImport(uri);
                    if (n <= 0) throw new IOException("Aucun message WhatsApp reconnu.");
                    selectOwnerAutomatically();
                    runOnUiThread(new Runnable() {
                        @Override public void run() { recreate(); }
                    });
                } catch (final Exception e) {
                    deleteDatabase(DB_NAME);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            Toast.makeText(MainActivityV041.this, "Import impossible : " + readable(e), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private String readable(Exception e) {
        String m = e.getMessage();
        return (m == null || m.trim().isEmpty()) ? e.getClass().getSimpleName() : m;
    }

    private long robustImport(Uri uri) throws Exception {
        InputStream probe = getContentResolver().openInputStream(uri);
        if (probe == null) throw new IOException("Android ne donne pas accès au fichier sélectionné.");
        BufferedInputStream bin = new BufferedInputStream(probe, 8192);
        bin.mark(8);
        int a = bin.read();
        int b = bin.read();
        int c = bin.read();
        int d = bin.read();
        bin.close();
        boolean zip = a == 'P' && b == 'K' && ((c == 3 && d == 4) || (c == 5 && d == 6) || (c == 7 && d == 8));

        File chosen;
        if (zip) chosen = extractBestChatText(uri);
        else {
            chosen = new File(getCacheDir(), "wa_selected_chat.txt");
            InputStream in = getContentResolver().openInputStream(uri);
            if (in == null) throw new IOException("Fichier texte inaccessible.");
            copy(in, chosen, 300L * 1024L * 1024L);
            in.close();
        }
        if (chosen == null || !chosen.exists() || chosen.length() == 0) throw new IOException("Aucun fichier de discussion lisible trouvé.");
        long count = buildDatabase(chosen);
        chosen.delete();
        return count;
    }

    private File extractBestChatText(Uri uri) throws Exception {
        InputStream raw = getContentResolver().openInputStream(uri);
        if (raw == null) throw new IOException("ZIP inaccessible.");
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(raw, 128 * 1024));
        File best = null;
        int bestScore = -1;
        int textFiles = 0;
        try {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                if (e.isDirectory()) continue;
                String base = new File(e.getName()).getName();
                String low = base.toLowerCase(Locale.ROOT);
                if (!low.endsWith(".txt")) continue;
                textFiles++;
                File candidate = new File(getCacheDir(), "wa_txt_" + textFiles + ".txt");
                copy(zis, candidate, 300L * 1024L * 1024L);
                int score = scoreText(candidate, base);
                if (score > bestScore) {
                    if (best != null) best.delete();
                    best = candidate;
                    bestScore = score;
                } else candidate.delete();
            }
        } finally {
            try { zis.close(); } catch (Exception ignored) {}
        }
        if (textFiles == 0) throw new IOException("Le ZIP ne contient aucun fichier .txt.");
        if (best == null || bestScore <= 0) throw new IOException("Fichier texte trouvé, mais son format WhatsApp n’est pas reconnu.");
        return best;
    }

    private int scoreText(File file, String name) {
        int score = 0;
        String low = name.toLowerCase(Locale.ROOT);
        if (low.equals("_chat.txt")) score += 5000;
        if (low.contains("whatsapp")) score += 2000;
        if (low.contains("discussion") || low.contains("chat")) score += 1000;
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8), 64 * 1024);
            String line;
            int lines = 0;
            while ((line = r.readLine()) != null && lines < 800) {
                lines++;
                if (parseStart(normalize(line)) != null) score += 10;
            }
        } catch (Exception ignored) {
        } finally {
            try { if (r != null) r.close(); } catch (Exception ignored) {}
        }
        return score;
    }

    private void copy(InputStream in, File out, long max) throws IOException {
        FileOutputStream fos = new FileOutputStream(out);
        byte[] buf = new byte[128 * 1024];
        long total = 0;
        try {
            int n;
            while ((n = in.read(buf)) != -1) {
                total += n;
                if (total > max) throw new IOException("Le fichier texte dépasse 300 Mo, ce qui est anormal pour un export WhatsApp.");
                fos.write(buf, 0, n);
            }
        } finally { fos.close(); }
    }

    private long buildDatabase(File chat) throws Exception {
        deleteDatabase(DB_NAME);
        SQLiteDatabase db = openOrCreateDatabase(DB_NAME, MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE messages (id INTEGER PRIMARY KEY AUTOINCREMENT, mdate TEXT, mtime TEXT, sender TEXT, body TEXT NOT NULL, system INTEGER NOT NULL DEFAULT 0, show_date INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX idx_messages_sender ON messages(sender)");
        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(chat), StandardCharsets.UTF_8), 128 * 1024);
        db.beginTransaction();
        long count = 0;
        String date = null, time = null, sender = null, lastDate = null;
        boolean system = false;
        StringBuilder body = null;
        try {
            String line;
            while ((line = r.readLine()) != null) {
                line = normalize(line);
                Start s = parseStart(line);
                if (s != null) {
                    if (body != null) {
                        insert(db, date, time, sender, body.toString(), system, lastDate == null || !lastDate.equals(date));
                        lastDate = date;
                        count++;
                    }
                    date = s.date;
                    time = s.time;
                    sender = s.sender;
                    system = s.system;
                    body = new StringBuilder(s.body);
                } else if (body != null) body.append('\n').append(line);
            }
            if (body != null) {
                insert(db, date, time, sender, body.toString(), system, lastDate == null || !lastDate.equals(date));
                count++;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
            r.close();
        }
        if (count == 0) throw new IOException("Le texte est lisible mais aucune ligne de message n’a été reconnue.");
        return count;
    }

    private void insert(SQLiteDatabase db, String date, String time, String sender, String body, boolean system, boolean showDate) {
        ContentValues v = new ContentValues();
        v.put("mdate", date == null ? "" : date);
        v.put("mtime", time == null ? "" : time);
        if (sender == null) v.putNull("sender"); else v.put("sender", sender);
        v.put("body", body == null ? "" : body);
        v.put("system", system ? 1 : 0);
        v.put("show_date", showDate ? 1 : 0);
        db.insertOrThrow("messages", null, v);
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.replace('\u202f', ' ').replace('\u00a0', ' ').replace("\u200e", "").replace("\u200f", "").replace("\ufeff", "").trim();
    }

    private Start parseStart(String line) {
        Matcher m = P1.matcher(line);
        if (!m.matches()) { m = P2.matcher(line); }
        if (!m.matches()) { m = P3.matcher(line); }
        if (!m.matches()) { m = P4.matcher(line); }
        if (!m.matches()) return null;
        String date = m.group(1).trim();
        String time = m.group(2).trim().toUpperCase(Locale.ROOT);
        String rest = m.group(3).trim();
        String sender = null;
        String body = rest;
        boolean system = true;
        int sep = rest.indexOf(": ");
        if (sep < 0) sep = rest.indexOf(" : ");
        if (sep > 0 && sep < 160) {
            sender = rest.substring(0, sep).trim();
            int advance = rest.startsWith(" : ", sep) ? 3 : 2;
            body = rest.substring(Math.min(rest.length(), sep + advance)).trim();
            system = sender.isEmpty();
        }
        return new Start(date, time, sender, body, system);
    }

    private void selectOwnerAutomatically() {
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = SQLiteDatabase.openDatabase(getDatabasePath(DB_NAME).getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
            c = db.rawQuery("SELECT sender, COUNT(*) n FROM messages WHERE sender IS NOT NULL AND TRIM(sender)<>'' GROUP BY sender ORDER BY n DESC", null);
            ArrayList<String> names = new ArrayList<>();
            while (c.moveToNext()) names.add(c.getString(0));
            String owner = "";
            for (String n : names) {
                String low = n.toLowerCase(Locale.ROOT);
                if (low.equals("thierry") || low.startsWith("thierry ") || low.contains("piiwii")) { owner = n; break; }
            }
            SharedPreferences p = getSharedPreferences("archive_prefs", MODE_PRIVATE);
            p.edit().putString("owner", owner).putInt("last_pos", -1).apply();
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
    }

    private static class Start {
        final String date, time, sender, body;
        final boolean system;
        Start(String d, String t, String s, String b, boolean y) { date=d; time=t; sender=s; body=b; system=y; }
    }
}
