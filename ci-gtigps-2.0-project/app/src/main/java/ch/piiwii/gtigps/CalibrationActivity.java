package ch.piiwii.gtigps;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CalibrationActivity extends Activity {
    private FrameLayout root;
    private FrameLayout box;
    private TextView info;
    private int screenW;
    private int screenH;
    private int x, y, w, h;
    private float dragStartRawX, dragStartRawY;
    private int dragStartX, dragStartY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenW = dm.widthPixels;
        screenH = dm.heightPixels;
        Rect r = WindowPrefs.getBounds(this);
        x = r.left; y = r.top; w = r.width(); h = r.height();
        buildUi();
        applyBox();
    }

    private void buildUi() {
        root = new FrameLayout(this);
        root.setBackgroundColor(0x22000000);

        box = new FrameLayout(this);
        GradientDrawable outline = new GradientDrawable();
        outline.setColor(0x14000000);
        outline.setStroke(dp(3), 0xFFFF2028);
        outline.setCornerRadius(dp(8));
        box.setBackground(outline);
        TextView label = new TextView(this);
        label.setText("ZONE GOOGLE MAPS\nGlisse ce cadre sur le widget");
        label.setTextColor(Color.WHITE);
        label.setTextSize(17);
        label.setGravity(Gravity.CENTER);
        label.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        label.setBackgroundColor(0x66000000);
        box.addView(label, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        box.setOnTouchListener(this::dragBox);
        root.addView(box);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setPadding(dp(8), dp(8), dp(8), dp(8));
        controls.setBackgroundColor(0xE6000000);

        info = new TextView(this);
        info.setTextColor(Color.WHITE);
        info.setTextSize(14);
        info.setGravity(Gravity.CENTER);
        controls.addView(info, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(38)));

        controls.addView(row(
                ctl("←", () -> move(-10, 0)), ctl("↑", () -> move(0, -10)),
                ctl("↓", () -> move(0, 10)), ctl("→", () -> move(10, 0))));
        controls.addView(row(
                ctl("L −", () -> resize(-20, 0)), ctl("L +", () -> resize(20, 0)),
                ctl("H −", () -> resize(0, -20)), ctl("H +", () -> resize(0, 20))));
        controls.addView(row(
                action("ANNULER", 0xFF333333, this::finish),
                action("ENREGISTRER", 0xFFD71920, () -> {
                    WindowPrefs.saveBounds(this, new Rect(x, y, x + w, y + h));
                    GTIGpsWidgetProvider.updateAll(this);
                    finish();
                })));

        FrameLayout.LayoutParams controlsLp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(180), Gravity.BOTTOM);
        root.addView(controls, controlsLp);
        setContentView(root);
    }

    private boolean dragBox(View v, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dragStartRawX = event.getRawX();
                dragStartRawY = event.getRawY();
                dragStartX = x;
                dragStartY = y;
                return true;
            case MotionEvent.ACTION_MOVE:
                x = dragStartX + Math.round(event.getRawX() - dragStartRawX);
                y = dragStartY + Math.round(event.getRawY() - dragStartRawY);
                clamp();
                applyBox();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                return true;
            default:
                return false;
        }
    }

    private LinearLayout row(View... views) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        for (View v : views) r.addView(v, new LinearLayout.LayoutParams(0, dp(42), 1f));
        return r;
    }

    private Button ctl(String text, Runnable action) {
        return action(text, 0xFF252525, action);
    }

    private Button action(String text, int color, Runnable action) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setTextSize(13);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(0, 0, 0, 0);
        b.setBackgroundColor(color);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private void move(int dx, int dy) {
        x += dx; y += dy; clamp(); applyBox();
    }

    private void resize(int dw, int dh) {
        w += dw; h += dh; clamp(); applyBox();
    }

    private void clamp() {
        w = Math.max(240, Math.min(w, screenW));
        h = Math.max(180, Math.min(h, screenH));
        x = Math.max(0, Math.min(x, Math.max(0, screenW - w)));
        y = Math.max(0, Math.min(y, Math.max(0, screenH - h)));
    }

    private void applyBox() {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h);
        lp.leftMargin = x;
        lp.topMargin = y;
        box.setLayoutParams(lp);
        if (info != null) info.setText("x=" + x + "  y=" + y + "  ·  " + w + " × " + h + " px");
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
