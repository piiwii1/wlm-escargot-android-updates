package ch.piiwii.waarchive;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

/**
 * 0.4.6
 * - conserve intégralement la 0.4.5 (médias + rendu)
 * - rend la barre de navigation Android lisible
 * - remplace les icônes appel/vidéo par recherche + calendrier
 * - recherche dans le texte des messages et navigation vers le résultat choisi
 * - sélection d'une date et saut vers le premier message de cette journée
 */
public class MainActivityV046 extends MainActivityV045Entry {
    private static final String DB_NAME = "archive.db";
    private String lastSearch = "";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        fixAndroidNavigationBar();
        patchHeaderActions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fixAndroidNavigationBar();
        patchHeaderActions();
    }

    private int d(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void fixAndroidNavigationBar() {
        try {
            Window w = getWindow();
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.setNavigationBarColor(Color.BLACK);
            if (Build.VERSION.SDK_INT >= 28) w.setNavigationBarDividerColor(Color.BLACK);
            if (Build.VERSION.SDK_INT >= 29) w.setNavigationBarContrastEnforced(true);
            if (Build.VERSION.SDK_INT >= 26) {
                View decor = w.getDecorView();
                int flags = decor.getSystemUiVisibility();
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                decor.setSystemUiVisibility(flags);
            }
        } catch (Exception ignored) {
        }
    }

    private void patchHeaderActions() {
        try {
            Field f = MainActivity.class.getDeclaredField("header");
            f.setAccessible(true);
            Object value = f.get(this);
            if (!(value instanceof LinearLayout)) return;
            LinearLayout header = (LinearLayout) value;
            if (header.getChildCount() < 6) return;
            if (header.getChildAt(3) instanceof SearchGlyph) return;

            header.removeViewAt(3);
            header.removeViewAt(3);

            SearchGlyph search = new SearchGlyph(this);
            search.setContentDescription("Rechercher un message");
            search.setOnClickListener(v -> showSearchDialog());

            CalendarGlyph calendar = new CalendarGlyph(this);
            calendar.setContentDescription("Aller à une date");
            calendar.setOnClickListener(v -> showDatePicker());

            header.addView(search, 3, new LinearLayout.LayoutParams(d(40), d(52)));
            header.addView(calendar, 4, new LinearLayout.LayoutParams(d(40), d(52)));
        } catch (Exception ignored) {
        }
    }

    private void showSearchDialog() {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Texte du message");
        input.setText(lastSearch);
        input.setSelection(input.length());

        LinearLayout box = new LinearLayout(this);
        box.setPadding(d(20), 0, d(20), 0);
        box.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Rechercher un message")
                .setView(box)
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Rechercher", null)
                .create();

        dialog.setOnShowListener(dlg -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String q = input.getText() == null ? "" : input.getText().toString().trim();
                if (q.isEmpty()) {
                    input.setError("Écris un mot ou une phrase");
                    return;
                }
                lastSearch = q;
                dialog.dismiss();
                searchMessages(q);
            });
            input.requestFocus();
            input.postDelayed(() -> {
                try {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
                } catch (Exception ignored) {}
            }, 180);
        });
        dialog.show();
    }

    private void searchMessages(String query) {
        File dbFile = getDatabasePath(DB_NAME);
        if (!dbFile.exists()) {
            Toast.makeText(this, "Aucune archive chargée.", Toast.LENGTH_SHORT).show();
            return;
        }

        final ArrayList<SearchHit> hits = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor c = null;
        boolean capped = false;
        try {
            db = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
            c = db.rawQuery("SELECT mdate,mtime,sender,body FROM messages ORDER BY id ASC", null);
            String needle = query.toLowerCase(Locale.ROOT);
            int position = 0;
            while (c.moveToNext()) {
                String body = c.getString(3);
                if (body != null && body.toLowerCase(Locale.ROOT).contains(needle)) {
                    String date = c.getString(0);
                    String time = c.getString(1);
                    String sender = c.isNull(2) ? "" : c.getString(2);
                    hits.add(new SearchHit(position, date, time, sender, snippet(body, needle)));
                    if (hits.size() >= 250) {
                        capped = true;
                        break;
                    }
                }
                position++;
            }
        } catch (Exception e) {
            Toast.makeText(this, "Recherche impossible.", Toast.LENGTH_LONG).show();
            return;
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }

        if (hits.isEmpty()) {
            Toast.makeText(this, "Aucun message trouvé pour « " + query + " »", Toast.LENGTH_LONG).show();
            return;
        }

        String[] items = new String[hits.size()];
        for (int i = 0; i < hits.size(); i++) {
            SearchHit h = hits.get(i);
            String who = h.sender == null || h.sender.trim().isEmpty() ? "" : h.sender + " — ";
            items[i] = safe(h.date) + " " + safe(h.time) + "\n" + who + h.snippet;
        }

        String title = capped ? "250+ résultats" : hits.size() + (hits.size() > 1 ? " résultats" : " résultat");
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(items, (dialog, which) -> jumpToPosition(hits.get(which).position, true))
                .setNegativeButton("Fermer", null)
                .show();
    }

    private String snippet(String body, String needleLower) {
        String clean = body.replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
        String lower = clean.toLowerCase(Locale.ROOT);
        int at = lower.indexOf(needleLower);
        if (at < 0) at = 0;
        int start = Math.max(0, at - 35);
        int end = Math.min(clean.length(), Math.max(at + needleLower.length() + 55, start + 95));
        String s = clean.substring(start, end);
        if (start > 0) s = "…" + s;
        if (end < clean.length()) s += "…";
        return s;
    }

    private String safe(String s) { return s == null ? "" : s; }

    private void showDatePicker() {
        int[] initial = initialArchiveDate();
        DatePickerDialog picker = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        jumpToDate(year, month + 1, dayOfMonth);
                    }
                }, initial[0], initial[1] - 1, initial[2]);
        picker.setTitle("Choisir la date du message");
        picker.show();
    }

    private int[] initialArchiveDate() {
        Calendar now = Calendar.getInstance();
        int[] fallback = {now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH)};
        File dbFile = getDatabasePath(DB_NAME);
        if (!dbFile.exists()) return fallback;
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
            String order = inferDateOrder(db);
            c = db.rawQuery("SELECT mdate FROM messages WHERE mdate<>'' ORDER BY id DESC LIMIT 1", null);
            if (c.moveToFirst()) {
                int[] p = parseDate(c.getString(0), order);
                if (p != null) return p;
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
        return fallback;
    }

    private void jumpToDate(int year, int month, int day) {
        File dbFile = getDatabasePath(DB_NAME);
        if (!dbFile.exists()) return;
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
            String order = inferDateOrder(db);
            c = db.rawQuery("SELECT mdate FROM messages ORDER BY id ASC", null);
            int pos = 0;
            while (c.moveToNext()) {
                int[] p = parseDate(c.getString(0), order);
                if (p != null && p[0] == year && p[1] == month && p[2] == day) {
                    jumpToPosition(pos, false);
                    return;
                }
                pos++;
            }
            Toast.makeText(this, String.format(Locale.ROOT,
                    "Aucun message le %02d/%02d/%04d", day, month, year), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Impossible d’ouvrir cette date.", Toast.LENGTH_LONG).show();
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
    }

    private String inferDateOrder(SQLiteDatabase db) {
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT mdate FROM messages WHERE mdate<>'' ORDER BY id ASC LIMIT 600", null);
            while (c.moveToNext()) {
                String raw = c.getString(0);
                if (raw == null) continue;
                String[] a = raw.trim().split("[./-]");
                if (a.length != 3) continue;
                int x = number(a[0]), y = number(a[1]);
                if (a[0].trim().length() == 4 || x > 31) return "YMD";
                if (x > 12 && x <= 31) return "DMY";
                if (y > 12 && y <= 31) return "MDY";
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        return "DMY";
    }

    private int[] parseDate(String raw, String order) {
        if (raw == null) return null;
        String[] a = raw.trim().split("[./-]");
        if (a.length != 3) return null;
        int x = number(a[0]), y = number(a[1]), z = number(a[2]);
        if (x < 0 || y < 0 || z < 0) return null;
        int year, month, day;
        if ("YMD".equals(order)) { year = x; month = y; day = z; }
        else if ("MDY".equals(order)) { month = x; day = y; year = z; }
        else { day = x; month = y; year = z; }
        if (year < 100) year += (year >= 70 ? 1900 : 2000);
        if (month < 1 || month > 12 || day < 1 || day > 31) return null;
        return new int[]{year, month, day};
    }

    private int number(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception e) { return -1; }
    }

    private void jumpToPosition(final int position, boolean fromSearch) {
        try {
            Field f = MainActivity.class.getDeclaredField("messageList");
            f.setAccessible(true);
            Object v = f.get(this);
            if (!(v instanceof ListView)) return;
            final ListView list = (ListView) v;
            list.post(() -> {
                list.setSelection(Math.max(0, position));
                if (fromSearch) Toast.makeText(MainActivityV046.this, "Message trouvé", Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            Toast.makeText(this, "Impossible d’aller à ce message.", Toast.LENGTH_SHORT).show();
        }
    }

    private static class SearchHit {
        final int position;
        final String date, time, sender, snippet;
        SearchHit(int position, String date, String time, String sender, String snippet) {
            this.position = position;
            this.date = date;
            this.time = time;
            this.sender = sender;
            this.snippet = snippet;
        }
    }

    private class SearchGlyph extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        SearchGlyph(Context c) {
            super(c);
            p.setColor(Color.WHITE);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(d(2.2f));
            p.setStrokeCap(Paint.Cap.ROUND);
            setClickable(true);
        }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float cx = getWidth() / 2f - d(2), cy = getHeight() / 2f - d(2);
            c.drawCircle(cx, cy, d(7.2f), p);
            c.drawLine(cx + d(5.3f), cy + d(5.3f), cx + d(11), cy + d(11), p);
        }
    }

    private class CalendarGlyph extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        CalendarGlyph(Context c) {
            super(c);
            p.setColor(Color.WHITE);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(d(2f));
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            setClickable(true);
        }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float cx = getWidth()/2f, cy=getHeight()/2f;
            c.drawRoundRect(cx-d(9), cy-d(8), cx+d(9), cy+d(9), d(2), d(2), p);
            c.drawLine(cx-d(9), cy-d(3), cx+d(9), cy-d(3), p);
            c.drawLine(cx-d(5), cy-d(11), cx-d(5), cy-d(6), p);
            c.drawLine(cx+d(5), cy-d(11), cx+d(5), cy-d(6), p);
            p.setStyle(Paint.Style.FILL);
            c.drawCircle(cx-d(4), cy+d(1), d(1.2f), p);
            c.drawCircle(cx+d(1), cy+d(1), d(1.2f), p);
            c.drawCircle(cx+d(5), cy+d(1), d(1.2f), p);
            c.drawCircle(cx-d(4), cy+d(5), d(1.2f), p);
            c.drawCircle(cx+d(1), cy+d(5), d(1.2f), p);
            p.setStyle(Paint.Style.STROKE);
        }
    }
}
