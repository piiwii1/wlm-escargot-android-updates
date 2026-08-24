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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivityV042 extends MainActivity {
    private static final int PICK_REAL_ARCHIVE = 1001;
    private static final String DB_NAME = "archive.db";

    private static final Pattern DATE_ANY = Pattern.compile("(?:\\d{1,4}[./-]\\d{1,2}[./-]\\d{1,4})");
    private static final Pattern TIME_ANY = Pattern.compile("(?:\\d{1,2}(?::|h|\\.)\\d{2}(?::\\d{2})?(?:\\s*(?:[AaPp]\\.?\\s*[Mm]\\.?))?)");
    private static final Pattern PREFIX_FALLBACK = Pattern.compile(
            "^\\s*\\[?\\s*(\\d{1,4}[./-]\\d{1,2}[./-]\\d{1,4})\\s*(?:,|à|at)?\\s*(\\d{1,2}(?::|h|\\.)\\d{2}(?::\\d{2})?(?:\\s*(?:[AaPp]\\.?\\s*[Mm]\\.?))?)\\s*\\]?\\s*(?:[-–—:]\\s*)?(.*)$",
            Pattern.CASE_INSENSITIVE);

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
            for (int i = 0; i < g.getChildCount(); i++) {
                if (replaceImportButton(g.getChildAt(i))) return true;
            }
        }
        return false;
    }

    private void chooseAnyArchive() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
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
                            Toast.makeText(MainActivityV042.this, "Import impossible : " + readable(e), Toast.LENGTH_LONG).show();
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
        int a = bin.read(), b = bin.read(), c = bin.read(), d = bin.read();
        bin.close();
        boolean zip = a == 'P' && b == 'K' && ((c == 3 && d == 4) || (c == 5 && d == 6) || (c == 7 && d == 8));

        File chosen;
        if (zip) chosen = extractBestChatText(uri);
        else {
            chosen = new File(getCacheDir(), "wa_selected_chat.txt");
            InputStream in = getContentResolver().openInputStream(uri);
            if (in == null) throw new IOException("Fichier texte inaccessible.");
            try { copy(in, chosen, 400L * 1024L * 1024L); }
            finally { in.close(); }
        }
        if (chosen == null || !chosen.exists() || chosen.length() == 0) {
            throw new IOException("Aucun fichier de discussion lisible trouvé.");
        }
        long count = buildDatabase(chosen);
        chosen.delete();
        return count;
    }

    private File extractBestChatText(Uri uri) throws Exception {
        InputStream raw = getContentResolver().openInputStream(uri);
        if (raw == null) throw new IOException("ZIP inaccessible.");
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(raw, 128 * 1024));
        File best = null;
        long bestRank = Long.MIN_VALUE;
        int textFiles = 0;
        try {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                if (e.isDirectory()) continue;
                String base = new File(e.getName()).getName();
                String low = base.toLowerCase(Locale.ROOT);
                if (!(low.endsWith(".txt") || low.endsWith(".text") || low.equals("_chat"))) continue;
                textFiles++;
                File candidate = new File(getCacheDir(), "wa_txt_" + textFiles + ".txt");
                copy(zis, candidate, 400L * 1024L * 1024L);
                long rank = rankText(candidate, base);
                if (rank > bestRank) {
                    if (best != null) best.delete();
                    best = candidate;
                    bestRank = rank;
                } else {
                    candidate.delete();
                }
            }
        } finally {
            try { zis.close(); } catch (Exception ignored) {}
        }
        if (textFiles == 0) throw new IOException("Le ZIP ne contient aucun fichier texte.");
        if (best == null) throw new IOException("Impossible de choisir le fichier de discussion.");
        return best;
    }

    private long rankText(File file, String name) {
        long rank = Math.min(file.length(), 200L * 1024L * 1024L) / 1024L;
        String low = name.toLowerCase(Locale.ROOT);
        if (low.equals("_chat.txt")) rank += 20_000_000L;
        if (low.contains("whatsapp")) rank += 10_000_000L;
        if (low.contains("discussion") || low.contains("chat")) rank += 5_000_000L;

        BufferedReader r = null;
        try {
            r = openTextReader(file);
            String line;
            int lines = 0;
            int hits = 0;
            while ((line = r.readLine()) != null && lines < 1500) {
                lines++;
                if (parseStart(normalize(line)) != null) hits++;
            }
            rank += hits * 100_000L;
        } catch (Exception ignored) {
        } finally {
            try { if (r != null) r.close(); } catch (Exception ignored) {}
        }
        return rank;
    }

    private void copy(InputStream in, File out, long max) throws IOException {
        FileOutputStream fos = new FileOutputStream(out);
        byte[] buf = new byte[128 * 1024];
        long total = 0;
        try {
            int n;
            while ((n = in.read(buf)) != -1) {
                total += n;
                if (total > max) throw new IOException("Le fichier texte dépasse 400 Mo.");
                fos.write(buf, 0, n);
            }
        } finally { fos.close(); }
    }

    private BufferedReader openTextReader(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        PushbackInputStream in = new PushbackInputStream(fis, 4);
        byte[] bom = new byte[4];
        int n = in.read(bom);
        Charset cs = StandardCharsets.UTF_8;
        int skip = 0;
        if (n >= 3 && (bom[0] & 0xFF) == 0xEF && (bom[1] & 0xFF) == 0xBB && (bom[2] & 0xFF) == 0xBF) {
            cs = StandardCharsets.UTF_8; skip = 3;
        } else if (n >= 2 && (bom[0] & 0xFF) == 0xFF && (bom[1] & 0xFF) == 0xFE) {
            cs = StandardCharsets.UTF_16LE; skip = 2;
        } else if (n >= 2 && (bom[0] & 0xFF) == 0xFE && (bom[1] & 0xFF) == 0xFF) {
            cs = StandardCharsets.UTF_16BE; skip = 2;
        } else if (n >= 4 && bom[1] == 0 && bom[3] == 0) {
            cs = StandardCharsets.UTF_16LE;
        } else if (n >= 4 && bom[0] == 0 && bom[2] == 0) {
            cs = StandardCharsets.UTF_16BE;
        }
        if (n > skip) in.unread(bom, skip, n - skip);
        return new BufferedReader(new InputStreamReader(in, cs), 128 * 1024);
    }

    private long buildDatabase(File chat) throws Exception {
        deleteDatabase(DB_NAME);
        SQLiteDatabase db = openOrCreateDatabase(DB_NAME, MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE messages (id INTEGER PRIMARY KEY AUTOINCREMENT, mdate TEXT, mtime TEXT, sender TEXT, body TEXT NOT NULL, system INTEGER NOT NULL DEFAULT 0, show_date INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX idx_messages_sender ON messages(sender)");
        BufferedReader r = openTextReader(chat);
        db.beginTransaction();
        long count = 0;
        String date = null, time = null, sender = null, lastDate = null;
        boolean system = false;
        StringBuilder body = null;
        ArrayList<String> sample = new ArrayList<>();
        try {
            String line;
            while ((line = r.readLine()) != null) {
                line = normalize(line);
                if (!line.isEmpty() && sample.size() < 4) sample.add(line);
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
                } else if (body != null) {
                    body.append('\n').append(line);
                }
            }
            if (body != null) {
                insert(db, date, time, sender, body.toString(), system, lastDate == null || !lastDate.equals(date));
                count++;
            }
            if (count > 0) db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
            r.close();
        }
        if (count == 0) {
            String preview = sample.isEmpty() ? "fichier vide" : shorten(sample.get(0));
            throw new IOException("Texte trouvé mais timestamp non reconnu. Début : " + preview);
        }
        return count;
    }

    private String shorten(String s) {
        s = s.replace('\n', ' ').replace('\r', ' ').trim();
        return s.length() > 90 ? s.substring(0, 90) + "…" : s;
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
        return s.replace("\u202f", " ")
                .replace("\u00a0", " ")
                .replace("\u200e", "")
                .replace("\u200f", "")
                .replace("\u202a", "")
                .replace("\u202b", "")
                .replace("\u202c", "")
                .replace("\u202d", "")
                .replace("\u202e", "")
                .replace("\u2066", "")
                .replace("\u2067", "")
                .replace("\u2068", "")
                .replace("\u2069", "")
                .replace("\ufeff", "")
                .trim();
    }

    private Start parseStart(String line) {
        if (line == null || line.isEmpty()) return null;

        String stamp = null;
        String rest = null;

        if (line.startsWith("[")) {
            int close = line.indexOf(']');
            if (close > 6 && close < 90) {
                String candidate = line.substring(1, close).trim();
                if (looksLikeTimestamp(candidate)) {
                    stamp = candidate;
                    rest = line.substring(close + 1).trim();
                    rest = stripLeadingSeparator(rest);
                }
            }
        }

        if (stamp == null) {
            int cut = findTimestampSeparator(line);
            if (cut > 6) {
                String candidate = line.substring(0, cut).trim();
                if (looksLikeTimestamp(candidate)) {
                    stamp = candidate;
                    rest = line.substring(skipSeparator(line, cut)).trim();
                }
            }
        }

        if (stamp == null) {
            Matcher fallback = PREFIX_FALLBACK.matcher(line);
            if (fallback.matches()) {
                stamp = fallback.group(1) + " " + fallback.group(2);
                rest = fallback.group(3).trim();
            }
        }

        if (stamp == null || rest == null) return null;

        Matcher dm = DATE_ANY.matcher(stamp);
        Matcher tm = TIME_ANY.matcher(stamp);
        if (!dm.find() || !tm.find()) return null;
        String date = dm.group().trim();
        String time = tm.group().trim().replaceAll("(?i)\\s+", " ");

        String sender = null;
        String body = rest;
        boolean system = true;
        int sep = senderSeparator(rest);
        if (sep > 0 && sep < 180) {
            String possible = rest.substring(0, sep).trim();
            if (!possible.isEmpty() && !possible.startsWith("http://") && !possible.startsWith("https://")) {
                sender = possible;
                body = rest.substring(sep + 1).trim();
                system = false;
            }
        }
        return new Start(date, time, sender, body, system);
    }

    private boolean looksLikeTimestamp(String s) {
        return DATE_ANY.matcher(s).find() && TIME_ANY.matcher(s).find();
    }

    private int findTimestampSeparator(String line) {
        String[] seps = {" - ", " – ", " — ", " – ", " — "};
        int best = -1;
        for (String sep : seps) {
            int p = line.indexOf(sep);
            if (p >= 0 && p < 100 && (best < 0 || p < best)) best = p;
        }
        return best;
    }

    private int skipSeparator(String line, int cut) {
        int i = cut;
        while (i < line.length() && Character.isWhitespace(line.charAt(i))) i++;
        if (i < line.length() && (line.charAt(i) == '-' || line.charAt(i) == '–' || line.charAt(i) == '—')) i++;
        while (i < line.length() && Character.isWhitespace(line.charAt(i))) i++;
        return i;
    }

    private String stripLeadingSeparator(String s) {
        int i = 0;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        if (i < s.length() && (s.charAt(i) == '-' || s.charAt(i) == '–' || s.charAt(i) == '—')) i++;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        return s.substring(i);
    }

    private int senderSeparator(String rest) {
        int p = rest.indexOf(':');
        if (p <= 0) return -1;
        return p;
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
        Start(String d, String t, String s, String b, boolean y) {
            date = d; time = t; sender = s; body = b; system = y;
        }
    }
}
