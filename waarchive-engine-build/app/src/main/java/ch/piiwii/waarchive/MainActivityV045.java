package ch.piiwii.waarchive;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.LruCache;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 0.4.5
 * - conserve le moteur de parsing 0.4.4
 * - extrait réellement images + audios du ZIP
 * - lecture locale des messages vocaux
 * - affichage des vraies images
 * - corrige la mesure des bulles de texte qui devenaient anormalement étroites
 */
public class MainActivityV045 extends MainActivityV043 {
    private static final int PICK_045 = 4501;
    private static final String DB_NAME = "archive.db";
    private static final String DB_BACKUP = "archive_before_045.db";
    private static final String PREFS = "archive_prefs";
    private static final String MEDIA_DIR = "wa_media";
    private static final String MEDIA_TMP = "wa_media_tmp";

    private static final int TEXT = Color.rgb(17, 27, 33);
    private static final int META = Color.rgb(102, 119, 129);
    private static final int GREEN = Color.rgb(0, 128, 105);
    private static final int DARK_GREEN = Color.rgb(0, 105, 92);
    private static final int SENT = Color.rgb(217, 253, 211);
    private static final int RECEIVED = Color.WHITE;
    private static final int WALLPAPER = Color.rgb(239, 234, 226);

    private static final Pattern MEDIA_FILE = Pattern.compile(
            "(?i)([\\p{L}\\p{N}_() .+@'’#%&\\-]+\\.(?:jpg|jpeg|png|webp|gif|heic|opus|ogg|m4a|mp3|aac|amr|wav))");

    private SQLiteDatabase mediaDb;
    private Cursor mediaCursor;
    private MediaAdapter mediaAdapter;
    private MediaPlayer activePlayer;
    private PlayGlyph activeGlyph;
    private String activeAudioPath;
    private boolean importing045;
    private boolean autoMediaAttempted;
    private TextView importStatus045;

    private final LruCache<String, Bitmap> bitmapCache = new LruCache<String, Bitmap>(16 * 1024) {
        @Override protected int sizeOf(String key, Bitmap value) {
            return Math.max(1, value.getByteCount() / 1024);
        }
    };

    private int d(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private TextView label(String value, float sp, int color) {
        TextView t = new TextView(this);
        t.setText(value == null ? "" : value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        return t;
    }

    private GradientDrawable rounded(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(d(radius));
        return g;
    }

    private GradientDrawable bubbleBackground(boolean sent) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(sent ? SENT : RECEIVED);
        float r = d(8);
        float s = d(2.5f);
        if (sent) g.setCornerRadii(new float[]{r,r,s,s,r,r,r,r});
        else g.setCornerRadii(new float[]{s,s,r,r,r,r,r,r});
        return g;
    }

    @Override
    protected void onCreate(Bundle state) {
        Uri incoming = incomingUri(getIntent());
        if (incoming != null) setIntent(new Intent());
        super.onCreate(state);
        patchPickers();
        patchConversationAdapter();
        if (incoming != null) {
            startImport045(incoming, displayName(incoming), false);
        } else {
            ensureMediaForExistingArchive();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Uri incoming = incomingUri(intent);
        super.onNewIntent(new Intent());
        if (incoming != null) startImport045(incoming, displayName(incoming), false);
    }

    private Uri incomingUri(Intent intent) {
        if (intent == null) return null;
        try {
            if (Intent.ACTION_SEND.equals(intent.getAction())) {
                Object value = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (value instanceof Uri) return (Uri) value;
            }
            if (Intent.ACTION_VIEW.equals(intent.getAction())) return intent.getData();
        } catch (Exception ignored) {}
        return null;
    }

    private void patchPickers() {
        patchButtonRecursive(getWindow().getDecorView());
    }

    private void patchButtonRecursive(View v) {
        if (v instanceof android.widget.Button) {
            android.widget.Button b = (android.widget.Button) v;
            String s = String.valueOf(b.getText()).toLowerCase(Locale.ROOT);
            if (s.contains("rechercher un export") || s.contains("ouvrir mon export")) {
                b.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) { choose045(false); }
                });
            } else if (s.contains("téléchargements")) {
                b.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) { choose045(true); }
                });
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) patchButtonRecursive(g.getChildAt(i));
        }
    }

    private void choose045(boolean downloads) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        if (downloads && android.os.Build.VERSION.SDK_INT >= 26) {
            i.putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI,
                    Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload"));
        }
        startActivityForResult(i, PICK_045);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_045 || requestCode == 4301 || requestCode == 1001) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                Uri uri = data.getData();
                boolean persisted = persist(uri, data.getFlags());
                startImport045(uri, displayName(uri), persisted);
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private boolean persist(Uri uri, int flags) {
        try {
            int read = flags & Intent.FLAG_GRANT_READ_URI_PERMISSION;
            getContentResolver().takePersistableUriPermission(uri,
                    read == 0 ? Intent.FLAG_GRANT_READ_URI_PERMISSION : read);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String displayName(Uri uri) {
        Cursor c = null;
        try {
            c = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (c != null && c.moveToFirst()) {
                String n = c.getString(0);
                if (n != null && !n.trim().isEmpty()) return n;
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        return "Export WhatsApp";
    }

    private void showImport045(String name) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(d(28), d(28), d(28), d(28));
        root.setBackgroundColor(Color.rgb(248, 249, 250));

        ProgressBar progress = new ProgressBar(this);
        root.addView(progress, new LinearLayout.LayoutParams(d(56), d(56)));

        TextView title = label("Ouverture de la vraie discussion…", 20, TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = d(20);
        root.addView(title, tlp);

        TextView file = label(name, 13.5f, META);
        file.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        flp.topMargin = d(8);
        root.addView(file, flp);

        importStatus045 = label("Analyse des messages…", 14, META);
        importStatus045.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = d(18);
        root.addView(importStatus045, slp);

        TextView hint = label("Les photos et messages vocaux sont maintenant copiés dans l’application pour pouvoir être affichés et lus hors ligne.", 12, META);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = d(12);
        root.addView(hint, hlp);

        getWindow().setStatusBarColor(DARK_GREEN);
        setContentView(root);
    }

    private void status(final String text) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                if (importStatus045 != null) importStatus045.setText(text);
            }
        });
    }

    private void startImport045(final Uri uri, final String name, final boolean persistable) {
        if (uri == null || importing045) return;
        importing045 = true;
        showImport045(name);

        new Thread(new Runnable() {
            @Override public void run() {
                File backup = getDatabasePath(DB_BACKUP);
                boolean hadOld = false;
                try {
                    closeBaseDatabase();
                    closeMediaCursor();
                    File current = getDatabasePath(DB_NAME);
                    if (backup.exists()) backup.delete();
                    if (current.exists() && current.length() > 0) {
                        copyFile(current, backup);
                        hadOld = true;
                    }

                    status("Analyse et découpage des messages…");
                    long reported = invokeRobustImport(uri);
                    long actual = countMessages();
                    if (reported <= 0 || actual <= 0) {
                        throw new IOException("Aucun message n’a été enregistré.");
                    }

                    status(actual + " messages reconnus. Extraction des photos et vocaux…");
                    MediaStats stats = extractMediaAtomically(uri);
                    invokeOwnerSelection();
                    SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
                    p.edit().putInt("last_pos", -1).apply();
                    if (persistable) rememberRecent(uri, name);
                    if (backup.exists()) backup.delete();

                    final long total = actual;
                    final MediaStats result = stats;
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            importing045 = false;
                            Toast.makeText(MainActivityV045.this,
                                    total + " messages • " + result.images + " images • " + result.audio + " vocaux",
                                    Toast.LENGTH_LONG).show();
                            recreate();
                        }
                    });
                } catch (final Exception e) {
                    try {
                        deleteDatabase(DB_NAME);
                        if (hadOld && backup.exists()) copyFile(backup, getDatabasePath(DB_NAME));
                    } catch (Exception ignored) {}
                    if (backup.exists()) backup.delete();
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            importing045 = false;
                            new AlertDialog.Builder(MainActivityV045.this)
                                    .setTitle("Import impossible")
                                    .setMessage(readable(e))
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override public void onClick(DialogInterface dialog, int which) { recreate(); }
                                    }).show();
                        }
                    });
                }
            }
        }).start();
    }

    private String readable(Exception e) {
        String m = e.getMessage();
        return m == null || m.trim().isEmpty() ? e.getClass().getSimpleName() : m;
    }

    private long invokeRobustImport(Uri uri) throws Exception {
        try {
            Method m = MainActivityV042.class.getDeclaredMethod("robustImport", Uri.class);
            m.setAccessible(true);
            Object r = m.invoke(this, uri);
            return r instanceof Number ? ((Number) r).longValue() : 0;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) throw (Exception) cause;
            throw new IOException(cause == null ? "Erreur d’import." : cause.toString());
        }
    }

    private void invokeOwnerSelection() {
        try {
            Method m = MainActivityV042.class.getDeclaredMethod("selectOwnerAutomatically");
            m.setAccessible(true);
            m.invoke(this);
        } catch (Exception ignored) {}
    }

    private long countMessages() {
        File f = getDatabasePath(DB_NAME);
        if (!f.exists() || f.length() == 0) return 0;
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = SQLiteDatabase.openDatabase(f.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
            c = db.rawQuery("SELECT COUNT(*) FROM messages", null);
            return c.moveToFirst() ? c.getLong(0) : 0;
        } catch (Exception e) {
            return 0;
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
    }

    private void closeBaseDatabase() {
        try {
            Field cf = MainActivity.class.getDeclaredField("messageCursor");
            cf.setAccessible(true);
            Object c = cf.get(this);
            if (c instanceof Cursor) ((Cursor) c).close();
            cf.set(this, null);
        } catch (Exception ignored) {}
        try {
            Field df = MainActivity.class.getDeclaredField("db");
            df.setAccessible(true);
            Object d = df.get(this);
            if (d instanceof SQLiteDatabase && ((SQLiteDatabase) d).isOpen()) ((SQLiteDatabase) d).close();
            df.set(this, null);
        } catch (Exception ignored) {}
    }

    private void copyFile(File from, File to) throws IOException {
        FileInputStream in = new FileInputStream(from);
        FileOutputStream out = new FileOutputStream(to);
        byte[] buf = new byte[128 * 1024];
        try {
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        } finally {
            try { in.close(); } catch (Exception ignored) {}
            try { out.close(); } catch (Exception ignored) {}
        }
    }

    private void rememberRecent(Uri uri, String name) {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        String u = uri.toString();
        String old0 = p.getString("recent_uri_0", "");
        String old0n = p.getString("recent_name_0", "");
        String old1 = p.getString("recent_uri_1", "");
        String old1n = p.getString("recent_name_1", "");
        SharedPreferences.Editor e = p.edit();
        if (!u.equals(old0)) {
            e.putString("recent_uri_2", old1).putString("recent_name_2", old1n);
            e.putString("recent_uri_1", old0).putString("recent_name_1", old0n);
        }
        e.putString("recent_uri_0", u).putString("recent_name_0", name == null ? "Export WhatsApp" : name);
        e.putString("last_archive_uri", u);
        e.apply();
    }

    private MediaStats extractMediaAtomically(Uri uri) throws Exception {
        InputStream probe = getContentResolver().openInputStream(uri);
        if (probe == null) return new MediaStats();
        BufferedInputStream pb = new BufferedInputStream(probe, 8 * 1024);
        int a = pb.read(), b = pb.read();
        pb.close();
        if (a != 'P' || b != 'K') return new MediaStats();

        File tmp = new File(getFilesDir(), MEDIA_TMP);
        deleteTree(tmp);
        if (!tmp.mkdirs() && !tmp.isDirectory()) throw new IOException("Impossible de créer le cache média.");

        MediaStats stats = new MediaStats();
        InputStream raw = getContentResolver().openInputStream(uri);
        if (raw == null) throw new IOException("Le ZIP n’est plus accessible pour extraire les médias.");
        ZipInputStream zip = new ZipInputStream(new BufferedInputStream(raw, 128 * 1024));
        byte[] buf = new byte[128 * 1024];
        try {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String base = new File(entry.getName()).getName();
                MediaKind kind = kindForName(base);
                if (kind == MediaKind.NONE) continue;
                File out = new File(tmp, mediaKey(base));
                FileOutputStream fos = new FileOutputStream(out);
                long total = 0;
                try {
                    int n;
                    while ((n = zip.read(buf)) != -1) {
                        total += n;
                        if (total > 250L * 1024L * 1024L) throw new IOException("Média trop volumineux : " + base);
                        fos.write(buf, 0, n);
                    }
                } finally {
                    fos.close();
                }
                if (kind == MediaKind.IMAGE) stats.images++;
                else if (kind == MediaKind.AUDIO) stats.audio++;
                if ((stats.images + stats.audio) % 40 == 0) {
                    status("Extraction des médias… " + stats.images + " images, " + stats.audio + " vocaux");
                }
            }
        } finally {
            try { zip.close(); } catch (Exception ignored) {}
        }

        File live = new File(getFilesDir(), MEDIA_DIR);
        deleteTree(live);
        if (!tmp.renameTo(live)) {
            if (!live.mkdirs() && !live.isDirectory()) throw new IOException("Impossible d’activer le cache média.");
            File[] files = tmp.listFiles();
            if (files != null) for (File f : files) copyFile(f, new File(live, f.getName()));
            deleteTree(tmp);
        }
        bitmapCache.evictAll();
        return stats;
    }

    private void ensureMediaForExistingArchive() {
        if (autoMediaAttempted || countMessages() <= 0) return;
        File media = new File(getFilesDir(), MEDIA_DIR);
        File[] files = media.listFiles();
        if (files != null && files.length > 0) return;
        final String raw = getSharedPreferences(PREFS, MODE_PRIVATE).getString("last_archive_uri",
                getSharedPreferences(PREFS, MODE_PRIVATE).getString("recent_uri_0", ""));
        if (raw == null || raw.isEmpty()) return;
        autoMediaAttempted = true;
        final Uri uri = Uri.parse(raw);
        Toast.makeText(this, "Récupération des photos et vocaux de l’archive…", Toast.LENGTH_LONG).show();
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    final MediaStats s = extractMediaAtomically(uri);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            Toast.makeText(MainActivityV045.this,
                                    s.images + " images et " + s.audio + " vocaux récupérés", Toast.LENGTH_LONG).show();
                            patchConversationAdapter();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            Toast.makeText(MainActivityV045.this,
                                    "Réimporte le ZIP une fois pour activer les médias.", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void deleteTree(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File c : children) deleteTree(c);
        }
        try { file.delete(); } catch (Exception ignored) {}
    }

    private enum MediaKind { NONE, IMAGE, AUDIO }

    private MediaKind kindForName(String name) {
        String s = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (s.endsWith(".jpg") || s.endsWith(".jpeg") || s.endsWith(".png") || s.endsWith(".webp") || s.endsWith(".gif") || s.endsWith(".heic")) return MediaKind.IMAGE;
        if (s.endsWith(".opus") || s.endsWith(".ogg") || s.endsWith(".m4a") || s.endsWith(".mp3") || s.endsWith(".aac") || s.endsWith(".amr") || s.endsWith(".wav")) return MediaKind.AUDIO;
        return MediaKind.NONE;
    }

    private String mediaKey(String base) {
        String lower = base == null ? "" : base.toLowerCase(Locale.ROOT).trim();
        String safe = lower.replaceAll("[^a-z0-9._-]", "_");
        if (safe.length() > 90) safe = safe.substring(safe.length() - 90);
        return Integer.toHexString(lower.hashCode()) + "_" + safe;
    }

    private String attachmentName(String body) {
        if (body == null) return null;
        String normalized = body.replace("\u200e", "").replace("\u200f", "");
        Matcher m = MEDIA_FILE.matcher(normalized);
        String last = null;
        while (m.find()) last = m.group(1).trim();
        if (last == null) return null;
        last = new File(last).getName().trim();
        return last;
    }

    private File mediaFile(String body) {
        String base = attachmentName(body);
        if (base == null) return null;
        File f = new File(new File(getFilesDir(), MEDIA_DIR), mediaKey(base));
        return f.exists() && f.length() > 0 ? f : null;
    }

    private void patchConversationAdapter() {
        closeMediaCursor();
        try {
            Field field = MainActivity.class.getDeclaredField("messageList");
            field.setAccessible(true);
            Object value = field.get(this);
            if (!(value instanceof ListView)) return;
            ListView list = (ListView) value;
            File dbFile = getDatabasePath(DB_NAME);
            if (!dbFile.exists()) return;
            mediaDb = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
            mediaCursor = mediaDb.rawQuery("SELECT id AS _id, mdate, mtime, sender, body, system, show_date FROM messages ORDER BY id ASC", null);
            mediaAdapter = new MediaAdapter(mediaCursor);
            list.setAdapter(mediaAdapter);
            int saved = getSharedPreferences(PREFS, MODE_PRIVATE).getInt("last_pos", -1);
            int target = saved >= 0 && saved < mediaAdapter.getCount() ? saved : Math.max(0, mediaAdapter.getCount() - 1);
            list.setSelection(target);
        } catch (Exception ignored) {}
    }

    private void closeMediaCursor() {
        if (mediaCursor != null) {
            try { mediaCursor.close(); } catch (Exception ignored) {}
            mediaCursor = null;
        }
        if (mediaDb != null) {
            try { mediaDb.close(); } catch (Exception ignored) {}
            mediaDb = null;
        }
    }

    private boolean mine(String sender) {
        String owner = getSharedPreferences(PREFS, MODE_PRIVATE).getString("owner", "");
        return sender != null && owner != null && !owner.isEmpty() && sender.equals(owner);
    }

    private View messageView(String date, String time, String sender, String body, boolean system, boolean showDate) {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(0, 0, 0, d(1));

        if (showDate) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER);
            TextView chip = label(date, 11.3f, Color.rgb(84,101,111));
            chip.setPadding(d(9), d(5), d(9), d(5));
            chip.setBackground(rounded(Color.rgb(248,252,253), 8));
            row.addView(chip);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = d(4); lp.bottomMargin = d(6);
            outer.addView(row, lp);
        }

        if (system) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER);
            TextView sys = label(body, 11.5f, Color.rgb(84,101,111));
            sys.setGravity(Gravity.CENTER);
            sys.setMaxWidth((int)(getResources().getDisplayMetrics().widthPixels * 0.86f));
            sys.setPadding(d(9),d(6),d(9),d(6));
            sys.setBackground(rounded(Color.rgb(255,244,199),8));
            row.addView(sys);
            outer.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return outer;
        }

        final boolean sent = mine(sender);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(sent ? Gravity.END : Gravity.START);
        row.setPadding(d(7), d(1), d(7), d(1));

        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(d(9), d(6), d(8), d(5));
        bubble.setBackground(bubbleBackground(sent));
        bubble.setElevation(d(0.5f));

        MediaKind kind = kindForName(attachmentName(body));
        if (kind == MediaKind.IMAGE) {
            bubble.addView(imageContent(body, time, sent));
        } else if (kind == MediaKind.AUDIO) {
            bubble.addView(audioContent(body, time, sent));
        } else {
            TextView message = label(body, 15.2f, TEXT);
            message.setLineSpacing(0, 1.03f);
            int max = (int)(getResources().getDisplayMetrics().widthPixels * 0.72f);
            int desired = desiredTextWidth(message, body, max);
            bubble.addView(message, new LinearLayout.LayoutParams(desired, ViewGroup.LayoutParams.WRAP_CONTENT));
            addMeta(bubble, time, sent);
        }

        int maxBubble = (int)(getResources().getDisplayMetrics().widthPixels * 0.82f);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.leftMargin = sent ? d(42) : 0;
        blp.rightMargin = sent ? 0 : d(42);
        bubble.setMaximumWidth(maxBubble);
        row.addView(bubble, blp);
        outer.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return outer;
    }

    private int desiredTextWidth(TextView view, String body, int max) {
        if (body == null || body.isEmpty()) return d(28);
        String[] lines = body.split("\\n", -1);
        float widest = 0;
        for (String line : lines) widest = Math.max(widest, view.getPaint().measureText(line));
        int natural = (int)Math.ceil(widest) + d(4);
        if (body.length() > 28 || natural > max) return max;
        return Math.max(d(24), Math.min(max, natural));
    }

    private void addMeta(LinearLayout box, String time, boolean sent) {
        TextView meta = label((time == null ? "" : time) + (sent ? "  ✓✓" : ""), 10.3f, META);
        meta.setGravity(Gravity.END);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.END;
        lp.leftMargin = d(24);
        lp.topMargin = d(1);
        box.addView(meta, lp);
    }

    private View imageContent(final String body, String time, boolean sent) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        final File file = mediaFile(body);
        if (file == null) {
            TextView missing = label("Image non extraite\n" + safeName(body), 12.5f, META);
            missing.setGravity(Gravity.CENTER);
            missing.setBackground(rounded(Color.rgb(201,225,221),7));
            box.addView(missing, new LinearLayout.LayoutParams(d(248), d(145)));
        } else {
            final Bitmap bitmap = bitmap(file);
            if (bitmap == null) {
                TextView bad = label("Image illisible\n" + safeName(body), 12.5f, META);
                bad.setGravity(Gravity.CENTER);
                bad.setBackground(rounded(Color.rgb(201,225,221),7));
                box.addView(bad, new LinearLayout.LayoutParams(d(248), d(145)));
            } else {
                ImageView image = new ImageView(this);
                image.setImageBitmap(bitmap);
                image.setAdjustViewBounds(true);
                image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                image.setBackgroundColor(Color.rgb(235,235,235));
                int width = d(248);
                int height = Math.round(width * (bitmap.getHeight() / (float)Math.max(1, bitmap.getWidth())));
                height = Math.max(d(110), Math.min(d(350), height));
                box.addView(image, new LinearLayout.LayoutParams(width, height));
                image.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) { showImage(bitmap); }
                });
            }
        }
        addMeta(box, time, sent);
        return box;
    }

    private Bitmap bitmap(File file) {
        String key = file.getAbsolutePath();
        Bitmap cached = bitmapCache.get(key);
        if (cached != null && !cached.isRecycled()) return cached;
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(key, bounds);
        int sample = 1;
        while (bounds.outWidth / sample > 1400 || bounds.outHeight / sample > 1400) sample *= 2;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = Math.max(1, sample);
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap decoded = BitmapFactory.decodeFile(key, opts);
        if (decoded != null) bitmapCache.put(key, decoded);
        return decoded;
    }

    private void showImage(Bitmap bitmap) {
        ImageView image = new ImageView(this);
        image.setImageBitmap(bitmap);
        image.setAdjustViewBounds(true);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setPadding(d(6),d(6),d(6),d(6));
        new AlertDialog.Builder(this).setView(image).setPositiveButton("Fermer", null).show();
    }

    private View audioContent(String body, String time, boolean sent) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        final File file = mediaFile(body);

        LinearLayout line = new LinearLayout(this);
        line.setGravity(Gravity.CENTER_VERTICAL);
        final PlayGlyph play = new PlayGlyph();
        play.setBackground(rounded(GREEN, 22));
        line.addView(play, new LinearLayout.LayoutParams(d(42),d(42)));
        WaveView wave = new WaveView();
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(d(180),d(34));
        wlp.leftMargin = d(8);
        line.addView(wave, wlp);
        box.addView(line, new LinearLayout.LayoutParams(d(232), ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView detail = label(file == null ? "Audio non extrait" : duration(file), 10.3f, META);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp.leftMargin = d(50);
        box.addView(detail, dlp);
        addMeta(box, time, sent);

        View.OnClickListener click = new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (file == null) {
                    Toast.makeText(MainActivityV045.this, "Audio introuvable : réimporte le ZIP avec la 0.4.5.", Toast.LENGTH_LONG).show();
                    return;
                }
                toggleAudio(file, play);
            }
        };
        play.setOnClickListener(click);
        line.setOnClickListener(click);
        return box;
    }

    private String duration(File file) {
        MediaMetadataRetriever r = new MediaMetadataRetriever();
        try {
            r.setDataSource(file.getAbsolutePath());
            String raw = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long ms = raw == null ? 0 : Long.parseLong(raw);
            long sec = Math.max(0, ms / 1000L);
            return String.format(Locale.ROOT, "%d:%02d", sec / 60, sec % 60);
        } catch (Exception e) {
            return safeName(file.getName());
        } finally {
            try { r.release(); } catch (Exception ignored) {}
        }
    }

    private void toggleAudio(final File file, final PlayGlyph glyph) {
        try {
            if (activePlayer != null && file.getAbsolutePath().equals(activeAudioPath)) {
                if (activePlayer.isPlaying()) {
                    activePlayer.pause();
                    glyph.setPlaying(false);
                } else {
                    activePlayer.start();
                    glyph.setPlaying(true);
                }
                return;
            }
            releasePlayer();
            activePlayer = new MediaPlayer();
            activePlayer.setDataSource(file.getAbsolutePath());
            activePlayer.prepare();
            activeAudioPath = file.getAbsolutePath();
            activeGlyph = glyph;
            activePlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override public void onCompletion(MediaPlayer mp) {
                    if (activeGlyph != null) activeGlyph.setPlaying(false);
                    try { mp.seekTo(0); } catch (Exception ignored) {}
                }
            });
            activePlayer.start();
            glyph.setPlaying(true);
        } catch (Exception e) {
            releasePlayer();
            Toast.makeText(this, "Impossible de lire ce vocal : " + readable(e), Toast.LENGTH_LONG).show();
        }
    }

    private void releasePlayer() {
        if (activeGlyph != null) activeGlyph.setPlaying(false);
        if (activePlayer != null) {
            try { activePlayer.stop(); } catch (Exception ignored) {}
            try { activePlayer.release(); } catch (Exception ignored) {}
        }
        activePlayer = null;
        activeGlyph = null;
        activeAudioPath = null;
    }

    private String safeName(String value) {
        String n = attachmentName(value);
        if (n == null || n.isEmpty()) n = value == null ? "Média" : value.trim();
        if (n.length() > 52) n = n.substring(0, 49) + "…";
        return n;
    }

    private class MediaAdapter extends CursorAdapter {
        MediaAdapter(Cursor cursor) { super(MainActivityV045.this, cursor, 0); }
        @Override public View newView(android.content.Context context, Cursor cursor, ViewGroup parent) {
            return new LinearLayout(MainActivityV045.this);
        }
        @Override public void bindView(View view, android.content.Context context, Cursor c) {
            LinearLayout holder = (LinearLayout) view;
            holder.removeAllViews();
            holder.setOrientation(LinearLayout.VERTICAL);
            String date = c.getString(c.getColumnIndexOrThrow("mdate"));
            String time = c.getString(c.getColumnIndexOrThrow("mtime"));
            String sender = c.isNull(c.getColumnIndexOrThrow("sender")) ? null : c.getString(c.getColumnIndexOrThrow("sender"));
            String body = c.getString(c.getColumnIndexOrThrow("body"));
            boolean system = c.getInt(c.getColumnIndexOrThrow("system")) != 0;
            boolean showDate = c.getInt(c.getColumnIndexOrThrow("show_date")) != 0;
            holder.addView(messageView(date,time,sender,body,system,showDate),
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    private class PlayGlyph extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean playing;
        PlayGlyph() {
            super(MainActivityV045.this);
            p.setColor(Color.WHITE);
            p.setStyle(Paint.Style.FILL);
        }
        void setPlaying(boolean value) { playing = value; invalidate(); }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float cx = getWidth()/2f, cy=getHeight()/2f;
            if (playing) {
                c.drawRoundRect(cx-d(5),cy-d(8),cx-d(1),cy+d(8),d(1),d(1),p);
                c.drawRoundRect(cx+d(2),cy-d(8),cx+d(6),cy+d(8),d(1),d(1),p);
            } else {
                Path path = new Path();
                path.moveTo(cx-d(5),cy-d(9));
                path.lineTo(cx+d(8),cy);
                path.lineTo(cx-d(5),cy+d(9));
                path.close();
                c.drawPath(path,p);
            }
        }
    }

    private class WaveView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int[] bars = {4,8,12,7,15,10,6,13,17,8,5,12,9,16,11,7,14,18,10,5,12,7,15,9,6,13,8,17,11,7,14,9};
        WaveView() {
            super(MainActivityV045.this);
            p.setColor(Color.rgb(111,132,142));
            p.setStrokeWidth(d(1.6f));
            p.setStrokeCap(Paint.Cap.ROUND);
        }
        @Override protected void onDraw(Canvas c) {
            float gap = getWidth()/(float)bars.length;
            float mid = getHeight()/2f;
            for (int i=0;i<bars.length;i++) {
                float h=d(bars[i]*0.48f), x=gap*i+gap/2f;
                c.drawLine(x,mid-h/2f,x,mid+h/2f,p);
            }
        }
    }

    private static class MediaStats {
        int images;
        int audio;
    }

    @Override
    protected void onDestroy() {
        releasePlayer();
        closeMediaCursor();
        bitmapCache.evictAll();
        super.onDestroy();
    }
}
