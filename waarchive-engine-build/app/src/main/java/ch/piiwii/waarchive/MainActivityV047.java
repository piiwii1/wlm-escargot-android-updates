package ch.piiwii.waarchive;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Locale;

/**
 * 0.4.7
 * - conserve la 0.4.6 (médias, calendrier, navigation Android)
 * - recherche façon WhatsApp directement dans la conversation
 * - précédent/suivant + compteur résultat courant / total
 * - conserve le contexte autour du message trouvé
 * - avatar Fanny réel dans le header
 */
public class MainActivityV047 extends MainActivityV046 {
    private static final String DB_NAME = "archive.db";
    private LinearLayout searchHeader;
    private EditText searchInput;
    private TextView searchCounter;
    private final ArrayList<Integer> searchPositions = new ArrayList<>();
    private int searchIndex = -1;
    private int searchGeneration = 0;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private boolean searchMode = false;

    private int d(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        patchV047Ui();
    }

    @Override
    protected void onResume() {
        super.onResume();
        patchV047Ui();
    }

    private void patchV047Ui() {
        patchAvatar();
        patchSearchButton();
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

    private void patchAvatar() {
        LinearLayout h = header();
        if (h == null || h.getChildCount() < 2) return;
        View current = h.getChildAt(1);
        if (current instanceof ImageView && "fanny-avatar".equals(current.getTag())) return;

        ImageView avatar = new ImageView(this);
        avatar.setTag("fanny-avatar");
        avatar.setImageResource(R.drawable.avatar_fanny);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatar.setContentDescription("Photo de profil de Fanny");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(d(40), d(40));
        lp.rightMargin = d(10);
        lp.gravity = Gravity.CENTER_VERTICAL;
        h.removeViewAt(1);
        h.addView(avatar, 1, lp);
    }

    private void patchSearchButton() {
        if (searchMode) return;
        LinearLayout h = header();
        if (h == null) return;
        View search = findByDescription(h, "Rechercher un message");
        if (search != null) {
            search.setOnClickListener(v -> enterSearchMode());
        }
    }

    private View findByDescription(View root, String description) {
        CharSequence d = root.getContentDescription();
        if (d != null && description.equals(d.toString())) return root;
        if (root instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) root;
            for (int i = 0; i < g.getChildCount(); i++) {
                View r = findByDescription(g.getChildAt(i), description);
                if (r != null) return r;
            }
        }
        return null;
    }

    private void enterSearchMode() {
        final LinearLayout h = header();
        if (h == null || h.getChildCount() < 6 || searchMode) return;
        searchMode = true;

        // Back reste visible. Avatar, nom, loupe, calendrier et menu disparaissent temporairement.
        for (int i = 1; i < 6; i++) h.getChildAt(i).setVisibility(View.GONE);
        h.getChildAt(0).setOnClickListener(v -> exitSearchMode());

        searchHeader = h;
        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setHint("Rechercher…");
        searchInput.setHintTextColor(Color.rgb(190, 226, 219));
        searchInput.setTextColor(Color.WHITE);
        searchInput.setTextSize(16.5f);
        searchInput.setBackgroundColor(Color.TRANSPARENT);
        searchInput.setPadding(d(4), 0, d(6), 0);
        searchInput.setSelectAllOnFocus(false);
        h.addView(searchInput, new LinearLayout.LayoutParams(0, d(52), 1f));

        searchCounter = new TextView(this);
        searchCounter.setText("0/0");
        searchCounter.setTextSize(11.5f);
        searchCounter.setTextColor(Color.rgb(220, 240, 236));
        searchCounter.setGravity(Gravity.CENTER);
        h.addView(searchCounter, new LinearLayout.LayoutParams(d(54), d(52)));

        SearchNavGlyph previous = new SearchNavGlyph(this, false);
        previous.setContentDescription("Résultat précédent");
        previous.setOnClickListener(v -> navigateSearch(-1));
        h.addView(previous, new LinearLayout.LayoutParams(d(38), d(52)));

        SearchNavGlyph next = new SearchNavGlyph(this, true);
        next.setContentDescription("Résultat suivant");
        next.setOnClickListener(v -> navigateSearch(1));
        h.addView(next, new LinearLayout.LayoutParams(d(38), d(52)));

        CloseGlyph close = new CloseGlyph(this);
        close.setContentDescription("Fermer la recherche");
        close.setOnClickListener(v -> exitSearchMode());
        h.addView(close, new LinearLayout.LayoutParams(d(38), d(52)));

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { scheduleSearch(String.valueOf(s)); }
            @Override public void afterTextChanged(Editable s) {}
        });
        searchInput.requestFocus();
        searchInput.postDelayed(() -> {
            try {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
            } catch (Exception ignored) {}
        }, 120);
    }

    private void exitSearchMode() {
        if (!searchMode) return;
        searchMode = false;
        searchGeneration++;
        searchHandler.removeCallbacksAndMessages(null);
        searchPositions.clear();
        searchIndex = -1;

        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && searchInput != null) imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
        } catch (Exception ignored) {}

        LinearLayout h = searchHeader != null ? searchHeader : header();
        if (h != null) {
            if (searchInput != null) h.removeView(searchInput);
            if (searchCounter != null) h.removeView(searchCounter);
            // Retire les trois contrôles ajoutés (précédent, suivant, fermer).
            while (h.getChildCount() > 6) h.removeViewAt(h.getChildCount() - 1);
            for (int i = 1; i < Math.min(6, h.getChildCount()); i++) h.getChildAt(i).setVisibility(View.VISIBLE);
            if (h.getChildCount() > 0) h.getChildAt(0).setOnClickListener(v -> finish());
        }
        searchHeader = null;
        searchInput = null;
        searchCounter = null;
        patchSearchButton();
    }

    private void scheduleSearch(final String raw) {
        final String query = raw == null ? "" : raw.trim();
        final int generation = ++searchGeneration;
        searchHandler.removeCallbacksAndMessages(null);
        if (query.isEmpty()) {
            searchPositions.clear();
            searchIndex = -1;
            updateCounter();
            return;
        }
        searchCounter.setText("…");
        searchHandler.postDelayed(() -> new Thread(() -> runSearch(query, generation)).start(), 220);
    }

    private void runSearch(String query, int generation) {
        final ArrayList<Integer> found = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            File f = getDatabasePath(DB_NAME);
            if (!f.exists()) return;
            db = SQLiteDatabase.openDatabase(f.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
            c = db.rawQuery("SELECT body FROM messages ORDER BY id ASC", null);
            String needle = query.toLowerCase(Locale.ROOT);
            int position = 0;
            while (c.moveToNext()) {
                String body = c.getString(0);
                if (body != null && body.toLowerCase(Locale.ROOT).contains(needle)) found.add(position);
                position++;
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }

        runOnUiThread(() -> {
            if (!searchMode || generation != searchGeneration) return;
            searchPositions.clear();
            searchPositions.addAll(found);
            if (searchPositions.isEmpty()) {
                searchIndex = -1;
                updateCounter();
                return;
            }
            ListView l = list();
            int here = l == null ? 0 : l.getFirstVisiblePosition();
            searchIndex = nearestIndexAtOrAfter(here);
            updateCounter();
            jumpToSearchResult(false);
        });
    }

    private int nearestIndexAtOrAfter(int listPosition) {
        for (int i = 0; i < searchPositions.size(); i++) {
            if (searchPositions.get(i) >= listPosition) return i;
        }
        return 0;
    }

    private void navigateSearch(int delta) {
        if (searchPositions.isEmpty()) {
            Toast.makeText(this, "Aucun résultat", Toast.LENGTH_SHORT).show();
            return;
        }
        if (searchIndex < 0) searchIndex = 0;
        else searchIndex = (searchIndex + delta + searchPositions.size()) % searchPositions.size();
        updateCounter();
        jumpToSearchResult(true);
    }

    private void updateCounter() {
        if (searchCounter == null) return;
        if (searchPositions.isEmpty() || searchIndex < 0) searchCounter.setText("0/0");
        else searchCounter.setText((searchIndex + 1) + "/" + searchPositions.size());
    }

    private void jumpToSearchResult(boolean animateNotice) {
        if (searchIndex < 0 || searchIndex >= searchPositions.size()) return;
        final ListView l = list();
        if (l == null) return;
        final int position = searchPositions.get(searchIndex);
        l.post(() -> {
            int offset = Math.max(d(58), l.getHeight() / 3);
            l.setSelectionFromTop(position, offset);
            l.postDelayed(() -> highlightVisibleRow(l, position), 120);
        });
        if (animateNotice && searchCounter != null) searchCounter.setAlpha(0.55f);
        if (animateNotice && searchCounter != null) searchCounter.animate().alpha(1f).setDuration(180).start();
    }

    private void highlightVisibleRow(final ListView l, final int position) {
        int childIndex = position - l.getFirstVisiblePosition();
        if (childIndex < 0 || childIndex >= l.getChildCount()) return;
        final View child = l.getChildAt(childIndex);
        if (child == null) return;
        child.setBackgroundColor(Color.argb(70, 255, 213, 79));
        child.postDelayed(() -> child.setBackgroundColor(Color.TRANSPARENT), 1150);
    }

    @Override
    public void onBackPressed() {
        if (searchMode) {
            exitSearchMode();
            return;
        }
        super.onBackPressed();
    }

    private class SearchNavGlyph extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final boolean down;
        SearchNavGlyph(Context c, boolean down) {
            super(c);
            this.down = down;
            p.setColor(Color.WHITE);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(d(2.1f));
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            setClickable(true);
        }
        @Override protected void onDraw(Canvas c) {
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float s = d(6);
            if (down) {
                c.drawLine(cx - s, cy - d(3), cx, cy + d(3), p);
                c.drawLine(cx, cy + d(3), cx + s, cy - d(3), p);
            } else {
                c.drawLine(cx - s, cy + d(3), cx, cy - d(3), p);
                c.drawLine(cx, cy - d(3), cx + s, cy + d(3), p);
            }
        }
    }

    private class CloseGlyph extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        CloseGlyph(Context c) {
            super(c);
            p.setColor(Color.WHITE);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(d(2.2f));
            p.setStrokeCap(Paint.Cap.ROUND);
            setClickable(true);
        }
        @Override protected void onDraw(Canvas c) {
            float cx = getWidth()/2f, cy = getHeight()/2f, s = d(6.5f);
            c.drawLine(cx-s,cy-s,cx+s,cy+s,p);
            c.drawLine(cx+s,cy-s,cx-s,cy+s,p);
        }
    }
}
