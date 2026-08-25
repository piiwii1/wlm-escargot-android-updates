package ch.piiwii.waarchive;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * WA Archive 0.4.8
 * - écran de démarrage protégé par code (000000 par défaut)
 * - code modifiable depuis Paramètres
 * - flèche retour masquée : une seule discussion dans l'application
 * - prise en charge des documents (PDF, Office, TXT, archives, vCard, calendrier...)
 * - conserve recherche WhatsApp, calendrier, photos et vocaux des versions précédentes
 */
public class MainActivityV048 extends MainActivityV047 {
    private static final String PREFS = "archive_prefs";
    private static final String PREF_PIN_HASH = "access_pin_hash";
    private static final String PREF_DOCS_URI = "docs_archive_uri";
    private static final String DEFAULT_PIN = "000000";
    private static final String DOC_DIR = "wa_docs";
    private static final String DOC_TMP = "wa_docs_tmp";

    private static final int GREEN = Color.rgb(0, 128, 105);
    private static final int DARK_GREEN = Color.rgb(0, 105, 92);
    private static final int WALLPAPER = Color.rgb(239, 234, 226);
    private static final int TEXT = Color.rgb(17, 27, 33);
    private static final int META = Color.rgb(102, 119, 129);
    private static final int SENT = Color.rgb(217, 253, 211);
    private static final int RECEIVED = Color.WHITE;

    private static final Pattern DOC_REF = Pattern.compile(
            "(?i)([\\p{L}\\p{N}][\\p{L}\\p{N}_() .+@'’#%&\\-]{0,200}\\.(?:pdf|doc|docx|xls|xlsx|ppt|pptx|txt|rtf|csv|zip|rar|7z|vcf|ics|odt|ods|odp|epub))");

    private static boolean sessionUnlocked = false;
    private boolean docsBusy = false;
    private boolean docsAttempted = false;

    private int d(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!sessionUnlocked) {
            showPinScreen();
            return;
        }
        patchUnlockedUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionUnlocked) patchUnlockedUi();
    }

    private void patchUnlockedUi() {
        hideBackArrow();
        patchMenu();
        installDocumentWrapper();
        recoverDocuments();
    }

    private LinearLayout header() {
        try {
            Field f = MainActivity.class.getDeclaredField("header");
            f.setAccessible(true);
            Object value = f.get(this);
            return value instanceof LinearLayout ? (LinearLayout) value : null;
        } catch (Exception e) {
            return null;
        }
    }

    private ListView list() {
        try {
            Field f = MainActivity.class.getDeclaredField("messageList");
            f.setAccessible(true);
            Object value = f.get(this);
            return value instanceof ListView ? (ListView) value : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void hideBackArrow() {
        LinearLayout h = header();
        if (h == null || h.getChildCount() < 1) return;
        View back = h.getChildAt(0);
        back.setVisibility(View.GONE);
        ViewGroup.LayoutParams raw = back.getLayoutParams();
        if (raw instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) raw;
            lp.width = 0;
            lp.height = d(52);
            back.setLayoutParams(lp);
        }
        if (h.getChildCount() > 1) {
            View avatar = h.getChildAt(1);
            ViewGroup.LayoutParams avRaw = avatar.getLayoutParams();
            if (avRaw instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams av = (LinearLayout.LayoutParams) avRaw;
                av.leftMargin = d(8);
                avatar.setLayoutParams(av);
            }
        }
    }

    private void patchMenu() {
        LinearLayout h = header();
        if (h == null || h.getChildCount() != 6) return; // ne touche pas au header pendant une recherche active
        View menu = h.getChildAt(5);
        menu.setOnClickListener(v -> showMainMenu());
    }

    private void showMainMenu() {
        final String[] items = {"Changer d’archive", "Changer qui je suis", "Paramètres", "À propos"};
        new AlertDialog.Builder(this)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) invokeMainMethod("chooseZip");
                        else if (which == 1) invokeMainMethod("chooseOwnerManually");
                        else if (which == 2) showSettings();
                        else Toast.makeText(MainActivityV048.this,
                                    "Archive WhatsApp 0.4.8 • archive locale en lecture seule",
                                    Toast.LENGTH_LONG).show();
                    }
                }).show();
    }

    private void invokeMainMethod(String name) {
        try {
            Method m = MainActivity.class.getDeclaredMethod(name);
            m.setAccessible(true);
            m.invoke(this);
        } catch (Exception e) {
            Toast.makeText(this, "Action indisponible.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSettings() {
        final String[] items = {"Modifier le code d’accès"};
        new AlertDialog.Builder(this)
                .setTitle("Paramètres")
                .setItems(items, (dialog, which) -> changePinStepCurrent())
                .setNegativeButton("Fermer", null)
                .show();
    }

    private EditText pinInput() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        input.setSingleLine(true);
        input.setGravity(Gravity.CENTER);
        input.setTextSize(22);
        input.setLetterSpacing(0.18f);
        return input;
    }

    private void changePinStepCurrent() {
        final EditText input = pinInput();
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Code actuel")
                .setMessage("Entre le code actuel à 6 chiffres.")
                .setView(input)
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Continuer", null)
                .create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String value = input.getText() == null ? "" : input.getText().toString();
            if (!validPin(value)) {
                input.setError("Code incorrect");
                input.selectAll();
                return;
            }
            dialog.dismiss();
            changePinStepNew();
        }));
        dialog.show();
        focusKeyboard(input);
    }

    private void changePinStepNew() {
        final EditText input = pinInput();
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Nouveau code")
                .setMessage("Choisis exactement 6 chiffres.")
                .setView(input)
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Continuer", null)
                .create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String value = input.getText() == null ? "" : input.getText().toString();
            if (!isSixDigits(value)) {
                input.setError("6 chiffres requis");
                return;
            }
            dialog.dismiss();
            changePinStepConfirm(value);
        }));
        dialog.show();
        focusKeyboard(input);
    }

    private void changePinStepConfirm(final String first) {
        final EditText input = pinInput();
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Confirmer le nouveau code")
                .setMessage("Retape les 6 chiffres.")
                .setView(input)
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Enregistrer", null)
                .create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String value = input.getText() == null ? "" : input.getText().toString();
            if (!first.equals(value)) {
                input.setError("Les deux codes ne correspondent pas");
                input.selectAll();
                return;
            }
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(PREF_PIN_HASH, hashPin(value)).apply();
            dialog.dismiss();
            Toast.makeText(this, "Code d’accès modifié.", Toast.LENGTH_LONG).show();
        }));
        dialog.show();
        focusKeyboard(input);
    }

    private void focusKeyboard(final EditText input) {
        input.requestFocus();
        input.postDelayed(() -> {
            try {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            } catch (Exception ignored) {}
        }, 160);
    }

    private boolean isSixDigits(String value) {
        return value != null && value.matches("\\d{6}");
    }

    private boolean validPin(String value) {
        if (!isSixDigits(value)) return false;
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        String saved = p.getString(PREF_PIN_HASH, "");
        String expected = saved == null || saved.isEmpty() ? hashPin(DEFAULT_PIN) : saved;
        return expected.equals(hashPin(value));
    }

    private String hashPin(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(("waarchive-pin-v1:" + value).getBytes("UTF-8"));
            StringBuilder out = new StringBuilder();
            for (byte b : bytes) out.append(String.format(Locale.ROOT, "%02x", b & 0xff));
            return out.toString();
        } catch (Exception e) {
            return value == null ? "" : value;
        }
    }

    private GradientDrawable rounded(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(d(radius));
        return g;
    }

    private void showPinScreen() {
        getWindow().setStatusBarColor(DARK_GREEN);
        getWindow().setNavigationBarColor(Color.BLACK);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(d(30), d(46), d(30), d(28));
        root.setBackgroundColor(Color.rgb(248, 249, 250));

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.mipmap.ic_launcher);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(icon, new LinearLayout.LayoutParams(d(96), d(96)));

        TextView title = new TextView(this);
        title.setText("Archive WhatsApp");
        title.setTextColor(TEXT);
        title.setTextSize(25);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = d(20);
        root.addView(title, tlp);

        TextView info = new TextView(this);
        info.setText("Entre ton code à 6 chiffres pour ouvrir la discussion.");
        info.setTextColor(META);
        info.setTextSize(14.5f);
        info.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ilp.topMargin = d(8);
        root.addView(info, ilp);

        final EditText code = pinInput();
        code.setHint("••••••");
        code.setBackground(rounded(Color.WHITE, 16));
        code.setPadding(d(12), d(10), d(12), d(10));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, d(58));
        clp.topMargin = d(28);
        root.addView(code, clp);

        Button open = new Button(this);
        open.setText("Ouvrir");
        open.setAllCaps(false);
        open.setTextColor(Color.WHITE);
        open.setTextSize(15);
        open.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        open.setBackground(rounded(GREEN, 26));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, d(52));
        blp.topMargin = d(14);
        root.addView(open, blp);

        TextView hint = new TextView(this);
        hint.setText("Code initial : 000000");
        hint.setTextColor(Color.rgb(132, 146, 154));
        hint.setTextSize(11.5f);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = d(13);
        root.addView(hint, hlp);

        View.OnClickListener submit = v -> {
            String value = code.getText() == null ? "" : code.getText().toString();
            if (!validPin(value)) {
                code.setError("Code incorrect");
                code.selectAll();
                return;
            }
            sessionUnlocked = true;
            try {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(code.getWindowToken(), 0);
            } catch (Exception ignored) {}
            recreate();
        };
        open.setOnClickListener(submit);
        code.setOnEditorActionListener((v, actionId, event) -> {
            if (code.length() == 6) { submit.onClick(code); return true; }
            return false;
        });

        setContentView(root);
        focusKeyboard(code);
    }

    @Override
    public void onBackPressed() {
        if (!sessionUnlocked) {
            finish();
            return;
        }
        super.onBackPressed();
    }

    // ---------------- Documents ----------------

    private boolean isDocument(String name) {
        if (name == null) return false;
        String s = name.toLowerCase(Locale.ROOT);
        if (s.equals("_chat.txt") || s.contains("whatsapp chat") || s.contains("discussion whatsapp")) return false;
        return s.endsWith(".pdf") || s.endsWith(".doc") || s.endsWith(".docx")
                || s.endsWith(".xls") || s.endsWith(".xlsx") || s.endsWith(".ppt") || s.endsWith(".pptx")
                || s.endsWith(".txt") || s.endsWith(".rtf") || s.endsWith(".csv")
                || s.endsWith(".zip") || s.endsWith(".rar") || s.endsWith(".7z")
                || s.endsWith(".vcf") || s.endsWith(".ics") || s.endsWith(".odt")
                || s.endsWith(".ods") || s.endsWith(".odp") || s.endsWith(".epub");
    }

    private String documentName(String body) {
        if (body == null) return null;
        String clean = body.replace("\u200e", "").replace("\u200f", "");
        Matcher m = DOC_REF.matcher(clean);
        String found = null;
        while (m.find()) found = m.group(1).trim();
        if (found == null) return null;
        found = new File(found).getName().trim();
        return isDocument(found) ? found : null;
    }

    private String key(String base) {
        String lower = base == null ? "" : base.toLowerCase(Locale.ROOT).trim();
        String safe = lower.replaceAll("[^a-z0-9._-]", "_");
        if (safe.length() > 90) safe = safe.substring(safe.length() - 90);
        return Integer.toHexString(lower.hashCode()) + "_" + safe;
    }

    private File documentFile(String body) {
        String name = documentName(body);
        if (name == null) return null;
        File file = new File(new File(getFilesDir(), DOC_DIR), key(name));
        return file.exists() && file.length() > 0 ? file : null;
    }

    private void recoverDocuments() {
        if (docsBusy || docsAttempted) return;
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        String raw = p.getString("last_archive_uri", p.getString("recent_uri_0", ""));
        if (raw == null || raw.isEmpty()) return;
        String done = p.getString(PREF_DOCS_URI, "");
        if (raw.equals(done)) return;

        docsAttempted = true;
        docsBusy = true;
        final Uri uri = Uri.parse(raw);
        new Thread(() -> {
            try {
                final int count = extractDocuments(uri);
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(PREF_DOCS_URI, raw).apply();
                runOnUiThread(() -> {
                    docsBusy = false;
                    installDocumentWrapper();
                    ListView l = list();
                    if (l != null) l.invalidateViews();
                    if (count > 0) Toast.makeText(this, count + " document" + (count > 1 ? "s" : "") + " récupéré" + (count > 1 ? "s" : ""), Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    docsBusy = false;
                    Toast.makeText(this, "Réimporte le ZIP une fois pour activer les documents.", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private int extractDocuments(Uri uri) throws Exception {
        InputStream raw = getContentResolver().openInputStream(uri);
        if (raw == null) throw new IOException("Archive inaccessible");
        File tmp = new File(getFilesDir(), DOC_TMP);
        deleteTree(tmp);
        if (!tmp.mkdirs() && !tmp.isDirectory()) throw new IOException("Impossible de créer le cache documents");

        int count = 0;
        ZipInputStream zip = new ZipInputStream(new BufferedInputStream(raw, 128 * 1024));
        byte[] buffer = new byte[128 * 1024];
        try {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String base = new File(entry.getName()).getName();
                if (!isDocument(base)) continue;
                File outFile = new File(tmp, key(base));
                FileOutputStream out = new FileOutputStream(outFile);
                long size = 0;
                boolean tooLarge = false;
                try {
                    int n;
                    while ((n = zip.read(buffer)) != -1) {
                        size += n;
                        if (size > 500L * 1024L * 1024L) { tooLarge = true; break; }
                        out.write(buffer, 0, n);
                    }
                } finally { out.close(); }
                if (tooLarge) {
                    outFile.delete();
                    try { zip.closeEntry(); } catch (Exception ignored) {}
                    continue;
                }
                count++;
            }
        } finally { try { zip.close(); } catch (Exception ignored) {} }

        File live = new File(getFilesDir(), DOC_DIR);
        deleteTree(live);
        if (!tmp.renameTo(live)) {
            if (!live.mkdirs() && !live.isDirectory()) throw new IOException("Impossible d’activer les documents");
            File[] files = tmp.listFiles();
            if (files != null) {
                for (File file : files) copyFile(file, new File(live, file.getName()));
            }
            deleteTree(tmp);
        }
        return count;
    }

    private void copyFile(File from, File to) throws IOException {
        java.io.FileInputStream in = new java.io.FileInputStream(from);
        FileOutputStream out = new FileOutputStream(to);
        byte[] b = new byte[128 * 1024];
        try {
            int n;
            while ((n = in.read(b)) != -1) out.write(b, 0, n);
        } finally {
            try { in.close(); } catch (Exception ignored) {}
            try { out.close(); } catch (Exception ignored) {}
        }
    }

    private void deleteTree(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteTree(child);
        }
        try { file.delete(); } catch (Exception ignored) {}
    }

    private void installDocumentWrapper() {
        ListView l = list();
        if (l == null) return;
        ListAdapter current = l.getAdapter();
        if (current == null || current instanceof DocumentAdapter) return;
        l.setAdapter(new DocumentAdapter(current));
    }

    private boolean mine(String sender) {
        String owner = getSharedPreferences(PREFS, MODE_PRIVATE).getString("owner", "");
        return sender != null && !owner.isEmpty() && sender.equals(owner);
    }

    private GradientDrawable bubble(boolean sent) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(sent ? SENT : RECEIVED);
        float r = d(8), s = d(2.5f);
        if (sent) g.setCornerRadii(new float[]{r,r,s,s,r,r,r,r});
        else g.setCornerRadii(new float[]{s,s,r,r,r,r,r,r});
        return g;
    }

    private View documentRow(String date, String time, String sender, String body, boolean system, boolean showDate) {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);

        if (showDate) {
            LinearLayout dr = new LinearLayout(this);
            dr.setGravity(Gravity.CENTER);
            TextView chip = smallText(date, 11.3f, Color.rgb(84,101,111));
            chip.setPadding(d(9), d(5), d(9), d(5));
            chip.setBackground(rounded(Color.rgb(248,252,253), 8));
            dr.addView(chip);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = d(4); lp.bottomMargin = d(6);
            outer.addView(dr, lp);
        }

        if (system) return outer;
        final boolean sent = mine(sender);
        final String name = documentName(body);
        final File file = documentFile(body);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(sent ? Gravity.END : Gravity.START);
        row.setPadding(d(7), d(1), d(7), d(1));

        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(d(7), d(6), d(7), d(5));
        bubble.setBackground(bubble(sent));

        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(d(7), d(7), d(8), d(7));
        card.setBackground(rounded(sent ? Color.rgb(205, 244, 200) : Color.rgb(242, 245, 246), 7));

        DocIcon icon = new DocIcon(extension(name));
        card.addView(icon, new LinearLayout.LayoutParams(d(46), d(50)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(d(8), 0, 0, 0);
        TextView filename = smallText(name == null ? "Document" : name, 13.5f, TEXT);
        filename.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        filename.setMaxLines(2);
        filename.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(filename, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        String detail = extension(name).toUpperCase(Locale.ROOT);
        if (file != null) detail += " • " + humanSize(file.length());
        else detail += " • document à récupérer";
        TextView meta = smallText(detail, 10.5f, META);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mlp.topMargin = d(3);
        info.addView(meta, mlp);
        card.addView(info, new LinearLayout.LayoutParams(d(220), ViewGroup.LayoutParams.WRAP_CONTENT));
        bubble.addView(card, new LinearLayout.LayoutParams(d(286), ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView timeView = smallText((time == null ? "" : time) + (sent ? "  ✓✓" : ""), 10.3f, META);
        timeView.setGravity(Gravity.END);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.gravity = Gravity.END; tlp.topMargin = d(2);
        bubble.addView(timeView, tlp);

        View.OnClickListener open = v -> {
            if (file == null) {
                Toast.makeText(this, "Document non extrait : réimporte le ZIP une fois.", Toast.LENGTH_LONG).show();
                return;
            }
            openDocument(file, name);
        };
        card.setOnClickListener(open);
        bubble.setOnClickListener(open);

        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.leftMargin = sent ? d(32) : 0;
        bp.rightMargin = sent ? 0 : d(32);
        row.addView(bubble, bp);
        outer.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return outer;
    }

    private TextView smallText(String value, float sp, int color) {
        TextView t = new TextView(this);
        t.setText(value == null ? "" : value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        return t;
    }

    private String extension(String name) {
        if (name == null) return "file";
        int dot = name.lastIndexOf('.');
        return dot >= 0 && dot < name.length() - 1 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "file";
    }

    private String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " o";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.ROOT, "%.1f Ko", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(Locale.ROOT, "%.1f Mo", mb);
        return String.format(Locale.ROOT, "%.2f Go", mb / 1024.0);
    }

    private String mime(String name) {
        String ext = extension(name);
        if ("pdf".equals(ext)) return "application/pdf";
        if ("doc".equals(ext)) return "application/msword";
        if ("docx".equals(ext)) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if ("xls".equals(ext)) return "application/vnd.ms-excel";
        if ("xlsx".equals(ext)) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if ("ppt".equals(ext)) return "application/vnd.ms-powerpoint";
        if ("pptx".equals(ext)) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        if ("txt".equals(ext)) return "text/plain";
        if ("rtf".equals(ext)) return "application/rtf";
        if ("csv".equals(ext)) return "text/csv";
        if ("vcf".equals(ext)) return "text/vcard";
        if ("ics".equals(ext)) return "text/calendar";
        if ("zip".equals(ext)) return "application/zip";
        if ("rar".equals(ext)) return "application/vnd.rar";
        if ("7z".equals(ext)) return "application/x-7z-compressed";
        if ("epub".equals(ext)) return "application/epub+zip";
        return "application/octet-stream";
    }

    private void openDocument(File file, String name) {
        Uri uri = new Uri.Builder()
                .scheme("content")
                .authority(ArchiveFileProvider.AUTHORITY)
                .appendPath(file.getName())
                .appendQueryParameter("name", name == null ? file.getName() : name)
                .build();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mime(name));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(intent, "Ouvrir le document"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Aucune application installée ne sait ouvrir ce document.", Toast.LENGTH_LONG).show();
        }
    }

    private class DocumentAdapter extends BaseAdapter {
        private final ListAdapter base;
        DocumentAdapter(ListAdapter base) { this.base = base; }
        @Override public int getCount() { return base.getCount(); }
        @Override public Object getItem(int position) { return base.getItem(position); }
        @Override public long getItemId(int position) { return base.getItemId(position); }
        @Override public boolean hasStableIds() { return base.hasStableIds(); }
        @Override public boolean areAllItemsEnabled() { return base.areAllItemsEnabled(); }
        @Override public boolean isEnabled(int position) { return base.isEnabled(position); }
        @Override public int getViewTypeCount() { return Math.max(1, base.getViewTypeCount() + 1); }
        @Override public int getItemViewType(int position) {
            Object item = base.getItem(position);
            if (item instanceof Cursor) {
                Cursor c = (Cursor) item;
                String body = c.getString(c.getColumnIndexOrThrow("body"));
                if (documentName(body) != null) return base.getViewTypeCount();
            }
            return base.getItemViewType(position);
        }
        @Override public View getView(int position, View convertView, ViewGroup parent) {
            Object item = base.getItem(position);
            if (item instanceof Cursor) {
                Cursor c = (Cursor) item;
                String body = c.getString(c.getColumnIndexOrThrow("body"));
                if (documentName(body) != null) {
                    String date = c.getString(c.getColumnIndexOrThrow("mdate"));
                    String time = c.getString(c.getColumnIndexOrThrow("mtime"));
                    String sender = c.isNull(c.getColumnIndexOrThrow("sender")) ? null : c.getString(c.getColumnIndexOrThrow("sender"));
                    boolean system = c.getInt(c.getColumnIndexOrThrow("system")) != 0;
                    boolean showDate = c.getInt(c.getColumnIndexOrThrow("show_date")) != 0;
                    return documentRow(date, time, sender, body, system, showDate);
                }
            }
            return base.getView(position, convertView, parent);
        }
    }

    private class DocIcon extends View {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final String ext;
        DocIcon(String ext) {
            super(MainActivityV048.this);
            this.ext = ext == null ? "FILE" : ext.toUpperCase(Locale.ROOT);
            int color = Color.rgb(92, 107, 115);
            if ("PDF".equals(this.ext)) color = Color.rgb(215, 65, 63);
            else if ("DOC".equals(this.ext) || "DOCX".equals(this.ext)) color = Color.rgb(61, 111, 184);
            else if ("XLS".equals(this.ext) || "XLSX".equals(this.ext) || "CSV".equals(this.ext)) color = Color.rgb(46, 139, 87);
            else if ("PPT".equals(this.ext) || "PPTX".equals(this.ext)) color = Color.rgb(220, 101, 52);
            fill.setColor(color);
            text.setColor(Color.WHITE);
            text.setTextAlign(Paint.Align.CENTER);
            text.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        }
        @Override protected void onDraw(Canvas c) {
            float pad = d(3);
            c.drawRoundRect(pad, pad, getWidth() - pad, getHeight() - pad, d(6), d(6), fill);
            text.setTextSize(ext.length() > 4 ? d(8) : d(9.5f));
            Paint.FontMetrics fm = text.getFontMetrics();
            float y = getHeight()/2f - (fm.ascent + fm.descent)/2f;
            String shown = ext.length() > 5 ? ext.substring(0, 5) : ext;
            c.drawText(shown, getWidth()/2f, y, text);
        }
    }

    @Override
    protected void onDestroy() {
        if (isFinishing() && !isChangingConfigurations()) sessionUnlocked = false;
        super.onDestroy();
    }
}
