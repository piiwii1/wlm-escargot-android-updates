package ch.piiwii.waarchive;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivityV043 extends MainActivityV042 {
    private static final int PICK_ARCHIVE_043 = 4301;
    private static final String DB_NAME = "archive.db";
    private static final String BACKUP_DB = "archive_before_import.db";
    private static final String PREFS = "archive_prefs";
    private static final int GREEN = Color.rgb(0, 128, 105);
    private static final int DARK_GREEN = Color.rgb(0, 105, 92);
    private static final int TEXT = Color.rgb(17, 27, 33);
    private static final int META = Color.rgb(102, 119, 129);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView importStatus;
    private long importStartedAt;
    private boolean importRunning;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        decorateImportScreen();
        handleIncomingIntent(getIntent(), false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent, true);
    }

    private int d(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private TextView label(String value, float sp, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        return t;
    }

    private Button findImportButton(View v) {
        if (v instanceof Button) {
            Button b = (Button) v;
            CharSequence cs = b.getText();
            if (cs != null) {
                String s = cs.toString().toLowerCase(Locale.ROOT);
                if (s.contains("ouvrir mon export") || s.contains("rechercher un export")) return b;
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                Button found = findImportButton(g.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private void decorateImportScreen() {
        final Button main = findImportButton(getWindow().getDecorView());
        if (main == null) return;
        main.setText("Rechercher un export sur le téléphone");
        main.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { chooseArchive(false); }
        });

        ViewGroup parent = (ViewGroup) main.getParent();
        if (!(parent instanceof LinearLayout)) return;
        LinearLayout box = (LinearLayout) parent;
        if (box.findViewWithTag("v043_extra") != null) return;

        LinearLayout extra = new LinearLayout(this);
        extra.setTag("v043_extra");
        extra.setOrientation(LinearLayout.VERTICAL);
        extra.setPadding(0, d(10), 0, 0);

        Button downloads = new Button(this);
        downloads.setAllCaps(false);
        downloads.setText("Ouvrir directement Téléchargements");
        downloads.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { chooseArchive(true); }
        });
        extra.addView(downloads, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, d(50)));

        TextView whatsapp = label(
                "Ton autre ZIP est encore dans WhatsApp ? Ouvre le document dans la discussion, puis fais Partager → Archive WhatsApp. L’application peut maintenant recevoir le fichier directement.",
                12.5f, META);
        whatsapp.setGravity(Gravity.CENTER);
        whatsapp.setPadding(d(5), d(14), d(5), d(8));
        extra.addView(whatsapp, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        addRecentSources(extra);
        box.addView(extra, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void addRecentSources(LinearLayout extra) {
        final SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean titleAdded = false;
        for (int i = 0; i < 3; i++) {
            final String uri = p.getString("recent_uri_" + i, "");
            String name = p.getString("recent_name_" + i, "");
            if (uri == null || uri.isEmpty()) continue;
            if (!titleAdded) {
                TextView title = label("Exports récents", 13.5f, TEXT);
                title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                title.setPadding(0, d(10), 0, d(4));
                extra.addView(title);
                titleAdded = true;
            }
            Button recent = new Button(this);
            recent.setAllCaps(false);
            recent.setText(name == null || name.isEmpty() ? "Rouvrir un export récent" : name);
            recent.setTag(uri);
            recent.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    String raw = String.valueOf(v.getTag());
                    startImport(Uri.parse(raw), "Export récent", false);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, d(46));
            lp.topMargin = d(4);
            extra.addView(recent, lp);
        }
    }

    private void chooseArchive(boolean downloads) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        if (downloads && android.os.Build.VERSION.SDK_INT >= 26) {
            i.putExtra(DocumentsContract.EXTRA_INITIAL_URI,
                    Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload"));
        }
        startActivityForResult(i, PICK_ARCHIVE_043);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == PICK_ARCHIVE_043 || requestCode == 1001)
                && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            boolean persisted = tryPersist(uri, data.getFlags());
            startImport(uri, displayName(uri), persisted);
            return;
        }
        if (requestCode == PICK_ARCHIVE_043 || requestCode == 1001) return;
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void handleIncomingIntent(Intent intent, boolean fromNewIntent) {
        if (intent == null || importRunning) return;
        String action = intent.getAction();
        Uri uri = null;
        if (Intent.ACTION_SEND.equals(action)) {
            try { uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM); } catch (Exception ignored) {}
        } else if (Intent.ACTION_VIEW.equals(action)) {
            uri = intent.getData();
        }
        if (uri == null) return;

        final Uri incoming = uri;
        setIntent(new Intent());
        if (hasUsableArchive()) {
            new AlertDialog.Builder(this)
                    .setTitle("Ouvrir cet export WhatsApp ?")
                    .setMessage("L’archive actuellement affichée sera conservée en sécurité jusqu’à ce que le nouvel import soit validé.")
                    .setNegativeButton("Annuler", null)
                    .setPositiveButton("Ouvrir", new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {
                            startImport(incoming, displayName(incoming), false);
                        }
                    }).show();
        } else {
            startImport(incoming, displayName(incoming), false);
        }
    }

    private boolean tryPersist(Uri uri, int flags) {
        try {
            int read = flags & Intent.FLAG_GRANT_READ_URI_PERMISSION;
            getContentResolver().takePersistableUriPermission(uri, read == 0 ? Intent.FLAG_GRANT_READ_URI_PERMISSION : read);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String displayName(Uri uri) {
        Cursor c = null;
        try {
            c = getContentResolver().query(uri, new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (c != null && c.moveToFirst()) {
                String s = c.getString(0);
                if (s != null && !s.trim().isEmpty()) return s;
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        String last = uri == null ? null : uri.getLastPathSegment();
        return last == null || last.isEmpty() ? "Export WhatsApp" : last;
    }

    private void showProgress(String name) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(d(30), d(30), d(30), d(30));
        root.setBackgroundColor(Color.rgb(248, 249, 250));

        ProgressBar bar = new ProgressBar(this);
        root.addView(bar, new LinearLayout.LayoutParams(d(56), d(56)));

        TextView title = label("Ouverture de la vraie discussion…", 20, TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = d(20);
        root.addView(title, tlp);

        TextView file = label(name == null ? "Export WhatsApp" : name, 13.5f, META);
        file.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        flp.topMargin = d(8);
        root.addView(file, flp);

        importStatus = label("Analyse du ZIP et du fichier texte…", 14, META);
        importStatus.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = d(18);
        root.addView(importStatus, slp);

        TextView hint = label("Sur une grosse archive, cette étape peut prendre un moment. L’application reste active pendant l’analyse.", 12, META);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = d(12);
        root.addView(hint, hlp);

        getWindow().setStatusBarColor(DARK_GREEN);
        setContentView(root);
        importStartedAt = SystemClock.elapsedRealtime();
        handler.post(ticker);
    }

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (!importRunning || importStatus == null) return;
            long seconds = Math.max(0, (SystemClock.elapsedRealtime() - importStartedAt) / 1000L);
            importStatus.setText("Analyse en cours… " + seconds + " s");
            handler.postDelayed(this, 1000L);
        }
    };

    private void startImport(final Uri uri, final String name, final boolean persistable) {
        if (uri == null || importRunning) return;
        importRunning = true;
        showProgress(name);

        new Thread(new Runnable() {
            @Override public void run() {
                File backup = getDatabasePath(BACKUP_DB);
                boolean hadBackup = false;
                try {
                    closeBaseDatabase();
                    File current = getDatabasePath(DB_NAME);
                    if (backup.exists()) backup.delete();
                    if (current.exists() && current.length() > 0) {
                        copyFile(current, backup);
                        hadBackup = true;
                    }

                    long reported = invokeRobustImport(uri);
                    long actual = countMessages();
                    if (reported <= 0 || actual <= 0) {
                        throw new IOException("L’analyse s’est terminée mais aucun message n’a été enregistré.");
                    }
                    invokeOwnerSelection();
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt("last_pos", -1).apply();
                    if (persistable) rememberRecent(uri, name);
                    if (backup.exists()) backup.delete();

                    final long total = actual;
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            importRunning = false;
                            handler.removeCallbacks(ticker);
                            if (importStatus != null) {
                                importStatus.setText(total + " messages reconnus. Ouverture…");
                            }
                            Toast.makeText(MainActivityV043.this,
                                    total + " messages importés", Toast.LENGTH_LONG).show();
                            handler.postDelayed(new Runnable() {
                                @Override public void run() { recreate(); }
                            }, 500L);
                        }
                    });
                } catch (final Exception e) {
                    try {
                        deleteDatabase(DB_NAME);
                        if (hadBackup && backup.exists()) copyFile(backup, getDatabasePath(DB_NAME));
                    } catch (Exception ignored) {}
                    if (backup.exists()) backup.delete();
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            importRunning = false;
                            handler.removeCallbacks(ticker);
                            new AlertDialog.Builder(MainActivityV043.this)
                                    .setTitle("Import impossible")
                                    .setMessage(readable(e))
                                    .setCancelable(false)
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override public void onClick(DialogInterface dialog, int which) { recreate(); }
                                    }).show();
                        }
                    });
                }
            }
        }).start();
    }

    private long invokeRobustImport(Uri uri) throws Exception {
        try {
            Method m = MainActivityV042.class.getDeclaredMethod("robustImport", Uri.class);
            m.setAccessible(true);
            Object result = m.invoke(this, uri);
            return result instanceof Number ? ((Number) result).longValue() : 0L;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) throw (Exception) cause;
            throw new IOException(cause == null ? "Erreur inconnue pendant l’import." : cause.toString());
        }
    }

    private void invokeOwnerSelection() {
        try {
            Method m = MainActivityV042.class.getDeclaredMethod("selectOwnerAutomatically");
            m.setAccessible(true);
            m.invoke(this);
        } catch (Exception ignored) {
        }
    }

    private void closeBaseDatabase() {
        try {
            Field cursorField = MainActivity.class.getDeclaredField("messageCursor");
            cursorField.setAccessible(true);
            Object cursor = cursorField.get(this);
            if (cursor instanceof Cursor) ((Cursor) cursor).close();
            cursorField.set(this, null);
        } catch (Exception ignored) {}
        try {
            Field dbField = MainActivity.class.getDeclaredField("db");
            dbField.setAccessible(true);
            Object value = dbField.get(this);
            if (value instanceof SQLiteDatabase) {
                SQLiteDatabase db = (SQLiteDatabase) value;
                if (db.isOpen()) db.close();
            }
            dbField.set(this, null);
        } catch (Exception ignored) {}
    }

    private boolean hasUsableArchive() {
        return countMessages() > 0;
    }

    private long countMessages() {
        File f = getDatabasePath(DB_NAME);
        if (!f.exists() || f.length() <= 0) return 0;
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = SQLiteDatabase.openDatabase(f.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
            c = db.rawQuery("SELECT COUNT(*) FROM messages", null);
            return c.moveToFirst() ? c.getLong(0) : 0;
        } catch (Exception ignored) {
            return 0;
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
    }

    private void rememberRecent(Uri uri, String name) {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        String raw = uri.toString();
        ArrayList<String> uris = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        uris.add(raw);
        names.add(name == null ? "Export WhatsApp" : name);
        for (int i = 0; i < 3 && uris.size() < 3; i++) {
            String old = p.getString("recent_uri_" + i, "");
            if (old == null || old.isEmpty() || old.equals(raw)) continue;
            uris.add(old);
            names.add(p.getString("recent_name_" + i, "Export WhatsApp"));
        }
        SharedPreferences.Editor e = p.edit();
        for (int i = 0; i < 3; i++) {
            if (i < uris.size()) {
                e.putString("recent_uri_" + i, uris.get(i));
                e.putString("recent_name_" + i, names.get(i));
            } else {
                e.remove("recent_uri_" + i);
                e.remove("recent_name_" + i);
            }
        }
        e.apply();
    }

    private void copyFile(File from, File to) throws IOException {
        File parent = to.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        FileInputStream in = new FileInputStream(from);
        FileOutputStream out = new FileOutputStream(to);
        byte[] buffer = new byte[256 * 1024];
        try {
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            out.getFD().sync();
        } finally {
            try { in.close(); } catch (Exception ignored) {}
            try { out.close(); } catch (Exception ignored) {}
        }
    }

    private String readable(Exception e) {
        String m = e.getMessage();
        return m == null || m.trim().isEmpty() ? e.getClass().getSimpleName() : m;
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(ticker);
        super.onDestroy();
    }
}
