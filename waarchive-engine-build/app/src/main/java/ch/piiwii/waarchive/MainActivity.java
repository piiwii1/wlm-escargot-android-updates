package ch.piiwii.waarchive;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends Activity {
    private static final int PICK_ZIP = 1001;
    private static final String DB_NAME = "archive.db";
    private static final String PREFS = "archive_prefs";
    private static final String PREF_OWNER = "owner";
    private static final String PREF_LAST_POS = "last_pos";

    private static final int TEXT = Color.rgb(17, 27, 33);
    private static final int META = Color.rgb(102, 119, 129);
    private static final int GREEN = Color.rgb(0, 128, 105);
    private static final int DARK_GREEN = Color.rgb(0, 105, 92);
    private static final int SENT = Color.rgb(217, 253, 211);
    private static final int RECEIVED = Color.WHITE;
    private static final int WALLPAPER = Color.rgb(239, 234, 226);

    private final Pattern androidPattern = Pattern.compile(
            "^\\[?(\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4})[, ]+(?:à\\s*)?(\\d{1,2}:\\d{2}(?::\\d{2})?)\\]?\\s*[-–]\\s*(.*)$");
    private final Pattern iosPattern = Pattern.compile(
            "^\\[(\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4})[, ]+(?:à\\s*)?(\\d{1,2}:\\d{2}(?::\\d{2})?)\\]\\s*(.*)$");

    private SharedPreferences prefs;
    private SQLiteDatabase db;
    private ListView messageList;
    private Cursor messageCursor;
    private MessageCursorAdapter adapter;
    private TextView headerTitle;
    private TextView headerSubtitle;
    private LinearLayout header;
    private LinearLayout composer;
    private volatile boolean importing = false;

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private TextView text(String value, float sp, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setIncludeFontPadding(false);
        return v;
    }

    private GradientDrawable rounded(int color, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private GradientDrawable circle(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        Window w = getWindow();
        w.setStatusBarColor(DARK_GREEN);
        w.setNavigationBarColor(WALLPAPER);

        if (hasImportedArchive()) {
            openDatabase();
            showConversation();
        } else {
            showImportScreen(null);
        }
    }

    private void openDatabase() {
        if (db == null || !db.isOpen()) {
            db = openOrCreateDatabase(DB_NAME, MODE_PRIVATE, null);
        }
    }

    private boolean hasImportedArchive() {
        File f = getDatabasePath(DB_NAME);
        if (!f.exists() || f.length() == 0) return false;
        SQLiteDatabase check = null;
        Cursor c = null;
        try {
            check = SQLiteDatabase.openDatabase(f.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
            c = check.rawQuery("SELECT COUNT(*) FROM messages", null);
            return c.moveToFirst() && c.getLong(0) > 0;
        } catch (Exception ignored) {
            return false;
        } finally {
            if (c != null) c.close();
            if (check != null) check.close();
        }
    }

    private void applyInsets(final View root, final View top, final View bottom) {
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                int topInset = insets.getSystemWindowInsetTop();
                int bottomInset = insets.getSystemWindowInsetBottom();
                if (top != null) {
                    top.setPadding(top.getPaddingLeft(), topInset, top.getPaddingRight(), top.getPaddingBottom());
                }
                if (bottom != null) {
                    bottom.setPadding(bottom.getPaddingLeft(), bottom.getPaddingTop(), bottom.getPaddingRight(), bottomInset + dp(5));
                }
                return insets;
            }
        });
        root.requestApplyInsets();
    }

    private void showImportScreen(String error) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(24), dp(28), dp(24));
        root.setBackgroundColor(Color.rgb(248, 249, 250));

        TextView logo = text("WA", 28, Color.WHITE);
        logo.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(circle(GREEN));
        root.addView(logo, new LinearLayout.LayoutParams(dp(78), dp(78)));

        TextView title = text("Archive WhatsApp", 25, TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.topMargin = dp(22);
        root.addView(title, titleLp);

        TextView info = text("Ouvre directement l’export ZIP de ta conversation WhatsApp. Le fichier reste privé sur ce téléphone.", 15, META);
        info.setGravity(Gravity.CENTER);
        info.setLineSpacing(0, 1.12f);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        infoLp.topMargin = dp(12);
        root.addView(info, infoLp);

        if (error != null && !error.isEmpty()) {
            TextView err = text(error, 13.5f, Color.rgb(180, 45, 45));
            err.setGravity(Gravity.CENTER);
            err.setPadding(dp(12), dp(10), dp(12), dp(10));
            err.setBackground(rounded(Color.rgb(255, 235, 235), 10));
            LinearLayout.LayoutParams errLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            errLp.topMargin = dp(16);
            root.addView(err, errLp);
        }

        Button pick = new Button(this);
        pick.setText("Ouvrir mon export WhatsApp");
        pick.setTextSize(14);
        pick.setTextColor(Color.WHITE);
        pick.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        pick.setAllCaps(false);
        pick.setBackground(rounded(GREEN, 24));
        pick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseZip();
            }
        });
        LinearLayout.LayoutParams pickLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        pickLp.topMargin = dp(24);
        root.addView(pick, pickLp);

        TextView hint = text("Formats reconnus : export WhatsApp Android/iPhone (.zip).\nAucune permission générale de stockage n’est demandée.", 12.5f, META);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintLp.topMargin = dp(17);
        root.addView(hint, hintLp);

        setContentView(root);
        applyInsets(root, null, root);
    }

    private void chooseZip() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/zip", "application/x-zip-compressed", "application/octet-stream"});
        startActivityForResult(intent, PICK_ZIP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_ZIP && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(uri, flags & Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {
            }
            importZip(uri);
        }
    }

    private void showImportProgress(String fileName) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(24), dp(28), dp(24));
        root.setBackgroundColor(Color.rgb(248, 249, 250));

        ProgressBar progress = new ProgressBar(this);
        root.addView(progress, new LinearLayout.LayoutParams(dp(56), dp(56)));

        TextView title = text("Ouverture de la vraie discussion…", 20, TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.topMargin = dp(22);
        root.addView(title, titleLp);

        TextView file = text(fileName == null ? "Export WhatsApp" : fileName, 13.5f, META);
        file.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams fileLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fileLp.topMargin = dp(8);
        root.addView(file, fileLp);

        final TextView status = text("Recherche du fichier de discussion dans le ZIP…", 14, META);
        status.setGravity(Gravity.CENTER);
        status.setTag("status");
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusLp.topMargin = dp(18);
        root.addView(status, statusLp);

        setContentView(root);
        applyInsets(root, null, root);
    }

    private TextView findStatusView() {
        View decor = getWindow().getDecorView();
        View v = decor.findViewWithTag("status");
        return v instanceof TextView ? (TextView) v : null;
    }

    private String displayName(Uri uri) {
        Cursor c = null;
        try {
            c = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (c != null && c.moveToFirst()) return c.getString(0);
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        return "Export WhatsApp.zip";
    }

    private void importZip(final Uri uri) {
        if (importing) return;
        importing = true;
        final String name = displayName(uri);
        showImportProgress(name);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    long count = importChatFromZip(uri);
                    importing = false;
                    if (count <= 0) throw new IOException("Aucun message WhatsApp reconnu dans ce ZIP.");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            openDatabase();
                            chooseOwnerIfNeeded();
                        }
                    });
                } catch (final Exception e) {
                    importing = false;
                    deleteDatabase(DB_NAME);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showImportScreen("Impossible d’ouvrir cet export : " + safeMessage(e));
                        }
                    });
                }
            }
        }).start();
    }

    private String safeMessage(Exception e) {
        String m = e.getMessage();
        return m == null || m.trim().isEmpty() ? e.getClass().getSimpleName() : m;
    }

    private long importChatFromZip(Uri uri) throws Exception {
        if (db != null && db.isOpen()) {
            db.close();
            db = null;
        }
        deleteDatabase(DB_NAME);
        openDatabase();
        db.execSQL("CREATE TABLE messages (id INTEGER PRIMARY KEY AUTOINCREMENT, mdate TEXT, mtime TEXT, sender TEXT, body TEXT NOT NULL, system INTEGER NOT NULL DEFAULT 0, show_date INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX idx_messages_sender ON messages(sender)");

        InputStream raw = getContentResolver().openInputStream(uri);
        if (raw == null) throw new IOException("Le fichier sélectionné est inaccessible.");
        ZipInputStream zip = new ZipInputStream(raw, StandardCharsets.UTF_8);
        File fallback = null;
        boolean parsed = false;
        long count = 0;
        try {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String base = new File(entry.getName()).getName();
                String lower = base.toLowerCase(Locale.ROOT);
                if (!lower.endsWith(".txt")) continue;

                if (lower.equals("_chat.txt") || lower.contains("whatsapp chat") || lower.contains("discussion whatsapp")) {
                    updateImportStatus("Analyse de " + base + "…");
                    count = parseChatStream(zip);
                    parsed = true;
                    break;
                }

                if (fallback == null) {
                    fallback = new File(getCacheDir(), "wa_fallback_chat.txt");
                    copyCurrentZipEntry(zip, fallback);
                }
            }
        } finally {
            try { zip.close(); } catch (Exception ignored) {}
        }

        if (!parsed && fallback != null && fallback.exists()) {
            updateImportStatus("Analyse du fichier texte trouvé…");
            InputStream in = new java.io.FileInputStream(fallback);
            try {
                count = parseChatInput(in);
            } finally {
                in.close();
                fallback.delete();
            }
        }

        if (count <= 0) throw new IOException("Le ZIP ne contient pas de _chat.txt WhatsApp lisible.");
        return count;
    }

    private void copyCurrentZipEntry(ZipInputStream zip, File out) throws IOException {
        FileOutputStream fos = new FileOutputStream(out);
        byte[] buffer = new byte[64 * 1024];
        int n;
        try {
            while ((n = zip.read(buffer)) != -1) fos.write(buffer, 0, n);
        } finally {
            fos.close();
        }
    }

    private long parseChatStream(ZipInputStream zip) throws Exception {
        return parseChatReader(new BufferedReader(new InputStreamReader(zip, StandardCharsets.UTF_8), 64 * 1024));
    }

    private long parseChatInput(InputStream in) throws Exception {
        return parseChatReader(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8), 64 * 1024));
    }

    private long parseChatReader(BufferedReader reader) throws Exception {
        db.beginTransaction();
        long count = 0;
        String currentDate = null;
        String currentTime = null;
        String currentSender = null;
        boolean currentSystem = false;
        StringBuilder currentBody = null;
        String lastInsertedDate = null;

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                line = normalizeLine(line);
                ParsedStart start = parseStart(line);
                if (start != null) {
                    if (currentBody != null) {
                        boolean showDate = lastInsertedDate == null || !lastInsertedDate.equals(currentDate);
                        insertMessage(currentDate, currentTime, currentSender, currentBody.toString(), currentSystem, showDate);
                        lastInsertedDate = currentDate;
                        count++;
                        if (count % 1000 == 0) updateImportStatus(count + " messages analysés…");
                    }
                    currentDate = start.date;
                    currentTime = start.time;
                    currentSender = start.sender;
                    currentSystem = start.system;
                    currentBody = new StringBuilder(start.body);
                } else if (currentBody != null) {
                    currentBody.append('\n').append(line);
                }
            }

            if (currentBody != null) {
                boolean showDate = lastInsertedDate == null || !lastInsertedDate.equals(currentDate);
                insertMessage(currentDate, currentTime, currentSender, currentBody.toString(), currentSystem, showDate);
                count++;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        updateImportStatus(count + " messages importés.");
        return count;
    }

    private String normalizeLine(String line) {
        if (line == null) return "";
        return line.replace('\u202f', ' ').replace('\u00a0', ' ').replace("\u200e", "").replace("\ufeff", "");
    }

    private ParsedStart parseStart(String line) {
        Matcher m = androidPattern.matcher(line);
        if (!m.matches()) {
            m = iosPattern.matcher(line);
            if (!m.matches()) return null;
        }
        String date = m.group(1);
        String time = m.group(2);
        String rest = m.group(3);
        String sender = null;
        String body = rest;
        boolean system = true;

        int sep = rest.indexOf(": ");
        if (sep < 0) sep = rest.indexOf(" : ");
        if (sep > 0 && sep < 100) {
            sender = rest.substring(0, sep).trim();
            body = rest.substring(sep + (rest.startsWith(" : ", sep) ? 3 : 2)).trim();
            if (!sender.isEmpty()) system = false;
        }
        return new ParsedStart(date, time, sender, body, system);
    }

    private void insertMessage(String date, String time, String sender, String body, boolean system, boolean showDate) {
        ContentValues cv = new ContentValues();
        cv.put("mdate", date == null ? "" : date);
        cv.put("mtime", time == null ? "" : time);
        if (sender == null) cv.putNull("sender"); else cv.put("sender", sender);
        cv.put("body", body == null ? "" : body);
        cv.put("system", system ? 1 : 0);
        cv.put("show_date", showDate ? 1 : 0);
        db.insertOrThrow("messages", null, cv);
    }

    private void updateImportStatus(final String value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView status = findStatusView();
                if (status != null) status.setText(value);
            }
        });
    }

    private ArrayList<String> participants() {
        ArrayList<String> result = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT sender, COUNT(*) AS n FROM messages WHERE sender IS NOT NULL AND TRIM(sender) <> '' GROUP BY sender ORDER BY n DESC", null);
        try {
            while (c.moveToNext()) result.add(c.getString(0));
        } finally {
            c.close();
        }
        return result;
    }

    private void chooseOwnerIfNeeded() {
        final ArrayList<String> people = participants();
        if (people.isEmpty()) {
            prefs.edit().putString(PREF_OWNER, "").apply();
            showConversation();
            return;
        }

        for (String p : people) {
            if (p.equalsIgnoreCase("Thierry") || p.toLowerCase(Locale.ROOT).startsWith("thierry ")) {
                prefs.edit().putString(PREF_OWNER, p).putInt(PREF_LAST_POS, -1).apply();
                showConversation();
                return;
            }
        }

        final String[] items = people.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Qui es-tu dans cette discussion ?")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        prefs.edit().putString(PREF_OWNER, items[which]).putInt(PREF_LAST_POS, -1).apply();
                        showConversation();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private String conversationName() {
        String owner = prefs.getString(PREF_OWNER, "");
        ArrayList<String> people = participants();
        for (String p : people) {
            if (!p.equals(owner)) return p;
        }
        return people.isEmpty() ? "Archive WhatsApp" : people.get(0);
    }

    private void showConversation() {
        openDatabase();
        if (messageCursor != null) messageCursor.close();
        messageCursor = db.rawQuery("SELECT id AS _id, mdate, mtime, sender, body, system, show_date FROM messages ORDER BY id ASC", null);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(WALLPAPER);

        header = buildHeader();
        root.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        FrameLayout conversation = new FrameLayout(this);
        conversation.setBackgroundColor(WALLPAPER);
        conversation.addView(new WallpaperView(this), new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        messageList = new ListView(this);
        messageList.setDivider(null);
        messageList.setDividerHeight(0);
        messageList.setSelector(android.R.color.transparent);
        messageList.setCacheColorHint(Color.TRANSPARENT);
        messageList.setBackgroundColor(Color.TRANSPARENT);
        messageList.setClipToPadding(false);
        messageList.setPadding(0, dp(7), 0, dp(8));
        adapter = new MessageCursorAdapter(messageCursor);
        messageList.setAdapter(adapter);
        conversation.addView(messageList, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(conversation, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        composer = buildReadOnlyComposer();
        root.addView(composer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
        applyInsets(root, header, composer);

        int saved = prefs.getInt(PREF_LAST_POS, -1);
        final int target = saved >= 0 && saved < adapter.getCount() ? saved : Math.max(0, adapter.getCount() - 1);
        messageList.post(new Runnable() {
            @Override
            public void run() {
                messageList.setSelection(target);
            }
        });
    }

    private LinearLayout buildHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setGravity(Gravity.BOTTOM | Gravity.CENTER_VERTICAL);
        h.setPadding(dp(4), 0, dp(4), dp(5));
        h.setBackgroundColor(GREEN);
        h.setMinimumHeight(dp(56));

        IconView back = new IconView(this, IconView.BACK, Color.WHITE);
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        h.addView(back, new LinearLayout.LayoutParams(dp(40), dp(52)));

        TextView avatar = text(initial(conversationName()), 18, Color.WHITE);
        avatar.setGravity(Gravity.CENTER);
        avatar.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        avatar.setBackground(circle(Color.rgb(113, 156, 169)));
        LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(dp(40), dp(40));
        avatarLp.rightMargin = dp(10);
        avatarLp.gravity = Gravity.CENTER_VERTICAL;
        h.addView(avatar, avatarLp);

        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        names.setGravity(Gravity.CENTER_VERTICAL);
        headerTitle = text(conversationName(), 17.5f, Color.WHITE);
        headerTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        headerSubtitle = text("archive • lecture seule", 11.5f, Color.rgb(216, 238, 235));
        names.addView(headerTitle);
        names.addView(headerSubtitle);
        h.addView(names, new LinearLayout.LayoutParams(0, dp(52), 1f));

        h.addView(new IconView(this, IconView.VIDEO, Color.WHITE), new LinearLayout.LayoutParams(dp(39), dp(52)));
        h.addView(new IconView(this, IconView.PHONE, Color.WHITE), new LinearLayout.LayoutParams(dp(39), dp(52)));
        IconView menu = new IconView(this, IconView.MENU, Color.WHITE);
        menu.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showArchiveMenu(); }
        });
        h.addView(menu, new LinearLayout.LayoutParams(dp(38), dp(52)));
        return h;
    }

    private String initial(String value) {
        if (value == null || value.trim().isEmpty()) return "?";
        return value.trim().substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private void showArchiveMenu() {
        final String[] items = {"Changer d’archive", "Changer qui je suis", "À propos"};
        new AlertDialog.Builder(this).setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) confirmChangeArchive();
                else if (which == 1) chooseOwnerManually();
                else Toast.makeText(MainActivity.this, "Archive WhatsApp 0.4.0 • données locales uniquement", Toast.LENGTH_LONG).show();
            }
        }).show();
    }

    private void confirmChangeArchive() {
        new AlertDialog.Builder(this)
                .setTitle("Changer d’archive ?")
                .setMessage("La discussion importée dans l’application sera remplacée. Ton ZIP original ne sera pas modifié.")
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Changer", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) { chooseZip(); }
                }).show();
    }

    private void chooseOwnerManually() {
        final ArrayList<String> people = participants();
        final String[] items = people.toArray(new String[0]);
        new AlertDialog.Builder(this).setTitle("Qui es-tu ?").setItems(items, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                prefs.edit().putString(PREF_OWNER, items[which]).apply();
                showConversation();
            }
        }).show();
    }

    private LinearLayout buildReadOnlyComposer() {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.TOP | Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(7), dp(5), dp(7), dp(5));
        bar.setBackgroundColor(WALLPAPER);

        LinearLayout field = new LinearLayout(this);
        field.setGravity(Gravity.CENTER_VERTICAL);
        field.setPadding(dp(3), 0, dp(2), 0);
        field.setBackground(rounded(Color.WHITE, 23));
        field.addView(new IconView(this, IconView.SMILE, Color.rgb(84, 101, 111)), new LinearLayout.LayoutParams(dp(40), dp(46)));
        TextView hint = text("Archive en lecture seule", 14.5f, Color.rgb(134, 150, 160));
        field.addView(hint, new LinearLayout.LayoutParams(0, dp(46), 1f));
        field.addView(new IconView(this, IconView.ATTACH, Color.rgb(84, 101, 111)), new LinearLayout.LayoutParams(dp(38), dp(46)));
        field.addView(new IconView(this, IconView.CAMERA, Color.rgb(84, 101, 111)), new LinearLayout.LayoutParams(dp(38), dp(46)));
        bar.addView(field, new LinearLayout.LayoutParams(0, dp(48), 1f));

        FrameLayout lockCircle = new FrameLayout(this);
        lockCircle.setBackground(circle(Color.rgb(92, 170, 154)));
        lockCircle.setAlpha(0.75f);
        lockCircle.addView(new IconView(this, IconView.LOCK, Color.WHITE), new FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(48), dp(48));
        lp.leftMargin = dp(6);
        bar.addView(lockCircle, lp);
        return bar;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (messageList != null) prefs.edit().putInt(PREF_LAST_POS, messageList.getFirstVisiblePosition()).apply();
    }

    @Override
    protected void onDestroy() {
        if (messageCursor != null) messageCursor.close();
        if (db != null && db.isOpen()) db.close();
        super.onDestroy();
    }

    private boolean isOwner(String sender) {
        String owner = prefs.getString(PREF_OWNER, "");
        return sender != null && !owner.isEmpty() && sender.equals(owner);
    }

    private LinearLayout createBubble(boolean sent) {
        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(dp(9), dp(6), dp(8), dp(5));
        bubble.setBackground(bubbleBackground(sent));
        bubble.setElevation(dp(0.5f));
        return bubble;
    }

    private GradientDrawable bubbleBackground(boolean sent) {
        float r = dp(8);
        float s = dp(2.5f);
        GradientDrawable d = new GradientDrawable();
        d.setColor(sent ? SENT : RECEIVED);
        if (sent) d.setCornerRadii(new float[]{r,r,s,s,r,r,r,r});
        else d.setCornerRadii(new float[]{s,s,r,r,r,r,r,r});
        return d;
    }

    private View buildMessageView(String date, String time, String sender, String body, boolean system, boolean showDate) {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(0, 0, 0, dp(1));

        if (showDate) {
            LinearLayout dateRow = new LinearLayout(this);
            dateRow.setGravity(Gravity.CENTER);
            TextView chip = text(date == null ? "" : date, 11.3f, Color.rgb(84,101,111));
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(9), dp(5), dp(9), dp(5));
            chip.setBackground(rounded(Color.rgb(248, 252, 253), 8));
            dateRow.addView(chip);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dlp.topMargin = dp(4);
            dlp.bottomMargin = dp(6);
            outer.addView(dateRow, dlp);
        }

        if (system) {
            LinearLayout sysRow = new LinearLayout(this);
            sysRow.setGravity(Gravity.CENTER);
            TextView sys = text(body, 11.5f, Color.rgb(84,101,111));
            sys.setGravity(Gravity.CENTER);
            sys.setMaxWidth((int)(getResources().getDisplayMetrics().widthPixels * 0.86f));
            sys.setPadding(dp(9), dp(6), dp(9), dp(6));
            sys.setBackground(rounded(Color.rgb(255,244,199), 8));
            sysRow.addView(sys);
            outer.addView(sysRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return outer;
        }

        boolean sent = isOwner(sender);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(sent ? Gravity.END : Gravity.START);
        row.setPadding(dp(7), dp(1), dp(7), dp(1));

        FrameLayout bubbleWrap = new FrameLayout(this);
        BubbleTail tail = new BubbleTail(this, sent, sent ? SENT : RECEIVED);
        FrameLayout.LayoutParams tailLp = new FrameLayout.LayoutParams(dp(9), dp(12), sent ? Gravity.TOP | Gravity.RIGHT : Gravity.TOP | Gravity.LEFT);
        tailLp.topMargin = dp(1);
        bubbleWrap.addView(tail, tailLp);

        LinearLayout bubble = createBubble(sent);
        FrameLayout.LayoutParams bubbleLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (sent) bubbleLp.rightMargin = dp(5); else bubbleLp.leftMargin = dp(5);

        AttachmentType type = attachmentType(body);
        if (type == AttachmentType.AUDIO) {
            bubble.addView(buildAudioPlaceholder(body, time, sent));
        } else if (type == AttachmentType.IMAGE) {
            bubble.addView(buildMediaPlaceholder(body, "PHOTO", time, sent, Color.rgb(201,225,221)));
        } else if (type == AttachmentType.VIDEO) {
            bubble.addView(buildMediaPlaceholder(body, "VIDÉO", time, sent, Color.rgb(211,220,230)));
        } else if (type == AttachmentType.DOCUMENT) {
            bubble.addView(buildDocumentPlaceholder(body, time, sent));
        } else {
            TextView message = text(body, 15.2f, TEXT);
            message.setMaxWidth((int)(getResources().getDisplayMetrics().widthPixels * 0.76f));
            message.setLineSpacing(0, 1.03f);
            bubble.addView(message);
            addMeta(bubble, time, sent);
        }
        bubbleWrap.addView(bubble, bubbleLp);

        LinearLayout.LayoutParams wrapLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wrapLp.leftMargin = sent ? dp(48) : 0;
        wrapLp.rightMargin = sent ? 0 : dp(48);
        row.addView(bubbleWrap, wrapLp);
        outer.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return outer;
    }

    private void addMeta(LinearLayout bubble, String time, boolean sent) {
        TextView meta = text((time == null ? "" : time) + (sent ? "  ✓✓" : ""), 10.3f, META);
        meta.setGravity(Gravity.END);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.END;
        lp.leftMargin = dp(26);
        lp.topMargin = dp(1);
        bubble.addView(meta, lp);
    }

    private View buildAudioPlaceholder(String body, String time, boolean sent) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        LinearLayout line = new LinearLayout(this);
        line.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout play = new FrameLayout(this);
        play.setBackground(circle(GREEN));
        play.addView(new IconView(this, IconView.PLAY, Color.WHITE), new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
        line.addView(play, new LinearLayout.LayoutParams(dp(38),dp(38)));
        Waveform wave = new Waveform(this);
        LinearLayout.LayoutParams waveLp = new LinearLayout.LayoutParams(dp(188), dp(32));
        waveLp.leftMargin = dp(8);
        line.addView(wave, waveLp);
        box.addView(line);
        TextView filename = text(shortAttachmentName(body), 10.2f, META);
        LinearLayout.LayoutParams fnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fnLp.leftMargin = dp(46);
        box.addView(filename, fnLp);
        addMeta(box, time, sent);
        return box;
    }

    private View buildMediaPlaceholder(String body, String label, String time, boolean sent, int color) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView media = text(label + "\n" + shortAttachmentName(body), 12.5f, META);
        media.setGravity(Gravity.CENTER);
        media.setBackground(rounded(color, 7));
        media.setPadding(dp(12),dp(20),dp(12),dp(20));
        box.addView(media, new LinearLayout.LayoutParams(dp(235), dp(135)));
        addMeta(box, time, sent);
        return box;
    }

    private View buildDocumentPlaceholder(String body, String time, boolean sent) {
        LinearLayout box = new LinearLayout(this);
        LinearLayout doc = new LinearLayout(this);
        doc.setGravity(Gravity.CENTER_VERTICAL);
        doc.setPadding(dp(8),dp(8),dp(8),dp(8));
        doc.setBackground(rounded(Color.rgb(238,242,244), 7));
        doc.addView(new IconView(this, IconView.DOCUMENT, META), new LinearLayout.LayoutParams(dp(34),dp(34)));
        TextView name = text(shortAttachmentName(body), 12.5f, TEXT);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(dp(190), ViewGroup.LayoutParams.WRAP_CONTENT);
        nlp.leftMargin = dp(6);
        doc.addView(name,nlp);
        box.addView(doc);
        addMeta(box,time,sent);
        return box;
    }

    private enum AttachmentType { NONE, IMAGE, AUDIO, VIDEO, DOCUMENT }

    private AttachmentType attachmentType(String body) {
        String s = body == null ? "" : body.toLowerCase(Locale.ROOT);
        if (containsExt(s, ".jpg", ".jpeg", ".png", ".webp", ".gif", ".heic")) return AttachmentType.IMAGE;
        if (containsExt(s, ".opus", ".ogg", ".m4a", ".mp3", ".aac", ".amr", ".wav")) return AttachmentType.AUDIO;
        if (containsExt(s, ".mp4", ".mov", ".m4v", ".3gp", ".webm")) return AttachmentType.VIDEO;
        if (containsExt(s, ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".zip")) return AttachmentType.DOCUMENT;
        return AttachmentType.NONE;
    }

    private boolean containsExt(String s, String... exts) {
        for (String ext : exts) if (s.contains(ext)) return true;
        return false;
    }

    private String shortAttachmentName(String body) {
        if (body == null) return "Pièce jointe";
        String s = body.replace("<Pièce jointe :", "").replace("<attached:", "").replace(">", "").trim();
        if (s.length() > 48) s = s.substring(0, 45) + "…";
        return s;
    }

    private class MessageCursorAdapter extends CursorAdapter {
        MessageCursorAdapter(Cursor cursor) { super(MainActivity.this, cursor, 0); }

        @Override
        public View newView(android.content.Context context, Cursor cursor, ViewGroup parent) {
            return new LinearLayout(MainActivity.this);
        }

        @Override
        public void bindView(View view, android.content.Context context, Cursor cursor) {
            LinearLayout holder = (LinearLayout) view;
            holder.removeAllViews();
            holder.setOrientation(LinearLayout.VERTICAL);
            String date = cursor.getString(cursor.getColumnIndexOrThrow("mdate"));
            String time = cursor.getString(cursor.getColumnIndexOrThrow("mtime"));
            String sender = cursor.isNull(cursor.getColumnIndexOrThrow("sender")) ? null : cursor.getString(cursor.getColumnIndexOrThrow("sender"));
            String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
            boolean system = cursor.getInt(cursor.getColumnIndexOrThrow("system")) != 0;
            boolean showDate = cursor.getInt(cursor.getColumnIndexOrThrow("show_date")) != 0;
            holder.addView(buildMessageView(date,time,sender,body,system,showDate), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    private static class ParsedStart {
        final String date, time, sender, body;
        final boolean system;
        ParsedStart(String date, String time, String sender, String body, boolean system) {
            this.date = date; this.time = time; this.sender = sender; this.body = body; this.system = system;
        }
    }

    private class WallpaperView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        WallpaperView(Activity c) {
            super(c);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(0.8f));
            paint.setColor(Color.argb(18, 90, 97, 101));
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int step = dp(60);
            for (int y = dp(16); y < getHeight(); y += step) {
                for (int x = dp(15); x < getWidth(); x += step) {
                    int shift = ((y / step) % 2) * dp(23);
                    float cx = x + shift;
                    canvas.drawCircle(cx,y,dp(4.5f),paint);
                    canvas.drawLine(cx+dp(11),y-dp(3),cx+dp(18),y+dp(4),paint);
                    canvas.drawCircle(cx+dp(32),y+dp(17),dp(2),paint);
                }
            }
        }
    }

    private class BubbleTail extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final boolean sent;
        BubbleTail(Activity c, boolean sent, int color) { super(c); this.sent = sent; paint.setColor(color); paint.setStyle(Paint.Style.FILL); }
        @Override protected void onDraw(Canvas canvas) {
            Path p = new Path();
            if (sent) { p.moveTo(0,0); p.lineTo(getWidth(),0); p.lineTo(0,getHeight()); }
            else { p.moveTo(0,0); p.lineTo(getWidth(),0); p.lineTo(getWidth(),getHeight()); }
            p.close(); canvas.drawPath(p,paint);
        }
    }

    private class Waveform extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int[] bars = {4,8,12,7,15,10,6,13,17,8,5,12,9,16,11,7,14,18,10,5,12,7,15,9,6,13,8,17,11,7,14,9};
        Waveform(Activity c) { super(c); paint.setColor(Color.rgb(111,132,142)); paint.setStrokeWidth(dp(1.6f)); paint.setStrokeCap(Paint.Cap.ROUND); }
        @Override protected void onDraw(Canvas canvas) {
            float gap = getWidth()/(float)bars.length, mid=getHeight()/2f;
            for(int i=0;i<bars.length;i++){ float h=dp(bars[i]*0.48f); float x=gap*i+gap/2f; canvas.drawLine(x,mid-h/2f,x,mid+h/2f,paint); }
        }
    }

    private class IconView extends View {
        static final int BACK=1, VIDEO=2, PHONE=3, MENU=4, SMILE=5, ATTACH=6, CAMERA=7, LOCK=8, PLAY=9, DOCUMENT=10;
        private final int type;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        IconView(Activity c, int type, int color) { super(c); this.type=type; p.setColor(color); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(2)); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND); }
        @Override protected void onDraw(Canvas c) {
            float w=getWidth(),h=getHeight(),cx=w/2f,cy=h/2f;
            switch(type){
                case BACK: c.drawLine(cx+dp(5),cy-dp(10),cx-dp(5),cy,p); c.drawLine(cx-dp(5),cy,cx+dp(5),cy+dp(10),p); break;
                case VIDEO: c.drawRect(cx-dp(9),cy-dp(7),cx+dp(5),cy+dp(7),p); Path vp=new Path(); vp.moveTo(cx+dp(5),cy-dp(4));vp.lineTo(cx+dp(10),cy-dp(7));vp.lineTo(cx+dp(10),cy+dp(7));vp.lineTo(cx+dp(5),cy+dp(4));c.drawPath(vp,p);break;
                case PHONE: c.drawArc(cx-dp(9),cy-dp(9),cx+dp(9),cy+dp(9),135,90,false,p); c.drawLine(cx-dp(7),cy+dp(6),cx-dp(10),cy+dp(3),p); c.drawLine(cx+dp(7),cy-dp(6),cx+dp(10),cy-dp(3),p); break;
                case MENU: p.setStyle(Paint.Style.FILL); c.drawCircle(cx,cy-dp(7),dp(1.7f),p);c.drawCircle(cx,cy,dp(1.7f),p);c.drawCircle(cx,cy+dp(7),dp(1.7f),p);p.setStyle(Paint.Style.STROKE);break;
                case SMILE: c.drawCircle(cx,cy,dp(9),p);p.setStyle(Paint.Style.FILL);c.drawCircle(cx-dp(3),cy-dp(2),dp(1),p);c.drawCircle(cx+dp(3),cy-dp(2),dp(1),p);p.setStyle(Paint.Style.STROKE);c.drawArc(cx-dp(4),cy, cx+dp(4),cy+dp(5),15,150,false,p);break;
                case ATTACH: c.drawOval(cx-dp(5),cy-dp(10),cx+dp(5),cy+dp(7),p); c.drawOval(cx-dp(2),cy-dp(7),cx+dp(2),cy+dp(4),p);break;
                case CAMERA: c.drawRoundRect(cx-dp(10),cy-dp(7),cx+dp(10),cy+dp(7),dp(2),dp(2),p);c.drawCircle(cx,cy,dp(4),p);c.drawLine(cx-dp(5),cy-dp(7),cx-dp(2),cy-dp(10),p);c.drawLine(cx-dp(2),cy-dp(10),cx+dp(3),cy-dp(10),p);c.drawLine(cx+dp(3),cy-dp(10),cx+dp(6),cy-dp(7),p);break;
                case LOCK: c.drawRoundRect(cx-dp(7),cy-dp(1),cx+dp(7),cy+dp(9),dp(2),dp(2),p);c.drawArc(cx-dp(5),cy-dp(9),cx+dp(5),cy+dp(3),180,-180,false,p);break;
                case PLAY: p.setStyle(Paint.Style.FILL);Path pp=new Path();pp.moveTo(cx-dp(4),cy-dp(7));pp.lineTo(cx+dp(7),cy);pp.lineTo(cx-dp(4),cy+dp(7));pp.close();c.drawPath(pp,p);p.setStyle(Paint.Style.STROKE);break;
                case DOCUMENT: c.drawRect(cx-dp(7),cy-dp(10),cx+dp(7),cy+dp(10),p);c.drawLine(cx-dp(4),cy-dp(4),cx+dp(4),cy-dp(4),p);c.drawLine(cx-dp(4),cy,cx+dp(4),cy,p);c.drawLine(cx-dp(4),cy+dp(4),cx+dp(2),cy+dp(4),p);break;
            }
        }
    }
}
