package ch.piiwii.modeconvoi;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.Locale;

/**
 * Foreground-only visual convoy alerts. Critical events are deliberately large
 * and centered so a driver/passenger already looking at Mode Convoi cannot miss
 * an important stop/problem message. Background delivery remains a notification.
 */
public final class ConvoyVisualAlertController implements ConvoyForegroundAlertBus.Listener, AutoCloseable {
    private final Activity activity;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ArrayDeque<JSONObject> queue = new ArrayDeque<>();
    private Dialog currentDialog;
    private boolean currentCritical;
    private Runnable dismissRunnable;
    private boolean closed;

    public ConvoyVisualAlertController(Activity activity) {
        this.activity = activity;
    }

    @Override public boolean onConvoyEvent(JSONObject event) {
        if (closed || event == null || !isVisualEvent(event)) return false;
        JSONObject copy;
        try { copy = new JSONObject(event.toString()); }
        catch (Exception ignored) { copy = event; }
        final JSONObject safe = copy;
        main.post(() -> enqueue(safe));
        return true;
    }

    private boolean isVisualEvent(JSONObject event) {
        String type = event.optString("type", "");
        if ("emergency-stop".equals(type) || "rally".equals(type)) return true;
        if (!"status".equals(type)) return false;
        String status = event.optString("status", "").toLowerCase(Locale.ROOT);
        String label = event.optString("label", "").toLowerCase(Locale.ROOT);
        return !("clear".equals(status) || label.contains("annul") || label.contains("effac"));
    }

    private void enqueue(JSONObject event) {
        if (closed || activity.isFinishing() || (Build.VERSION.SDK_INT >= 17 && activity.isDestroyed())) return;
        AlertStyle style = styleFor(event);
        if (style.critical) {
            queue.addFirst(event);
            if (currentDialog != null && currentDialog.isShowing() && !currentCritical) {
                currentDialog.dismiss();
                return;
            }
        } else {
            queue.addLast(event);
        }
        if (currentDialog == null || !currentDialog.isShowing()) showNext();
    }

    private void showNext() {
        if (closed || queue.isEmpty() || activity.isFinishing() || (Build.VERSION.SDK_INT >= 17 && activity.isDestroyed())) return;
        JSONObject event = queue.pollFirst();
        AlertStyle style = styleFor(event);
        currentCritical = style.critical;

        Dialog dialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
        currentDialog = dialog;

        FrameLayout scrim = new FrameLayout(activity);
        scrim.setBackgroundColor(Color.argb(style.critical ? 190 : 150, 0, 0, 0));
        scrim.setPadding(dp(18), dp(18), dp(18), dp(18));

        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(24), dp(style.critical ? 28 : 22), dp(24), dp(22));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(style.backgroundColor);
        bg.setCornerRadius(dp(style.critical ? 34 : 26));
        bg.setStroke(dp(style.critical ? 4 : 2), style.borderColor);
        card.setBackground(bg);

        TextView icon = text(style.icon, style.critical ? 88 : 64, true, Color.WHITE);
        icon.setGravity(Gravity.CENTER);
        card.addView(icon, new LinearLayout.LayoutParams(-1, dp(style.critical ? 118 : 88)));

        TextView title = text(style.title, style.critical ? 28 : 22, true, Color.WHITE);
        title.setGravity(Gravity.CENTER);
        title.setLetterSpacing(.04f);
        card.addView(title);

        String who = event.optString("participantName", "").trim();
        if (!who.isEmpty()) {
            TextView participant = text(who.toUpperCase(Locale.ROOT), 16, true, Color.WHITE);
            participant.setGravity(Gravity.CENTER);
            participant.setPadding(0, dp(8), 0, 0);
            card.addView(participant);
        }

        String label = event.optString("label", "Nouvelle information").trim();
        TextView message = text(label, style.critical ? 19 : 17, false, Color.WHITE);
        message.setGravity(Gravity.CENTER);
        message.setPadding(dp(4), dp(8), dp(4), dp(8));
        card.addView(message);

        TextView hint = text("TOUCHE POUR FERMER", 10, true, Color.argb(205, 255, 255, 255));
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, dp(8), 0, 0);
        card.addView(hint);

        int width = dp(style.critical ? 340 : 320);
        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(width, -2, Gravity.CENTER);
        scrim.addView(card, cardLp);
        scrim.setOnClickListener(v -> dialog.dismiss());
        card.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(scrim);
        dialog.setCancelable(true);
        dialog.setOnDismissListener(d -> {
            if (dismissRunnable != null) main.removeCallbacks(dismissRunnable);
            dismissRunnable = null;
            currentDialog = null;
            currentCritical = false;
            main.post(this::showNext);
        });
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        vibrate(style.critical);
        dismissRunnable = () -> {
            if (currentDialog == dialog && dialog.isShowing()) dialog.dismiss();
        };
        main.postDelayed(dismissRunnable, style.durationMs);
    }

    private AlertStyle styleFor(JSONObject event) {
        String type = event.optString("type", "").toLowerCase(Locale.ROOT);
        String status = event.optString("status", "").toLowerCase(Locale.ROOT);
        String label = event.optString("label", "").toLowerCase(Locale.ROOT);
        String key = status + " " + label;

        if ("emergency-stop".equals(type)) return new AlertStyle("🛑", "ARRÊT GÉNÉRAL", Color.rgb(150, 18, 24), Color.rgb(255, 92, 92), true, 6500);
        if (key.contains("arrêt") || key.contains("arret") || key.contains("stop")) return new AlertStyle("🛑", "ARRÊT", Color.rgb(150, 18, 24), Color.rgb(255, 92, 92), true, 6000);
        if (key.contains("probl") || key.contains("panne") || key.contains("voiture")) return new AlertStyle("⚠️", "PROBLÈME", Color.rgb(135, 48, 12), Color.rgb(255, 151, 68), true, 6000);
        if (key.contains("essence") || key.contains("fuel") || key.contains("carbur")) return new AlertStyle("⛽", "ESSENCE", Color.rgb(72, 58, 18), Color.rgb(255, 190, 40), false, 4600);
        if (key.contains("pause")) return new AlertStyle("☕", "PAUSE", Color.rgb(57, 49, 41), Color.rgb(212, 167, 119), false, 4300);
        if (key.contains("wc") || key.contains("toilet")) return new AlertStyle("🚻", "WC", Color.rgb(23, 71, 98), Color.rgb(80, 184, 235), false, 4300);
        if (key.contains("rejoin") || key.contains("joining")) return new AlertStyle("↗️", "JE REJOINS", Color.rgb(25, 76, 56), Color.rgb(84, 205, 142), false, 4300);
        if (key.contains(" ok") || key.startsWith("ok") || key.contains("tout va")) return new AlertStyle("✅", "OK", Color.rgb(27, 86, 45), Color.rgb(102, 220, 132), false, 3600);
        if ("rally".equals(type)) return new AlertStyle("📍", "REGROUPEMENT", Color.rgb(63, 52, 18), Color.rgb(255, 181, 20), false, 5000);
        return new AlertStyle("⚑", "MESSAGE DU CONVOI", Color.rgb(43, 47, 52), Color.rgb(255, 181, 20), false, 4300);
    }

    private TextView text(String value, float sizeSp, boolean bold, int color) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        if (bold) view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        return view;
    }

    private int dp(int value) {
        return Math.max(1, Math.round(value * activity.getResources().getDisplayMetrics().density));
    }

    private void vibrate(boolean critical) {
        try {
            Vibrator vibrator;
            if (Build.VERSION.SDK_INT >= 31) {
                VibratorManager manager = (VibratorManager) activity.getSystemService(Activity.VIBRATOR_MANAGER_SERVICE);
                vibrator = manager == null ? null : manager.getDefaultVibrator();
            } else {
                vibrator = (Vibrator) activity.getSystemService(Activity.VIBRATOR_SERVICE);
            }
            if (vibrator == null || !vibrator.hasVibrator()) return;
            long[] pattern = critical ? new long[]{0, 130, 80, 180, 80, 240} : new long[]{0, 90};
            if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            else vibrator.vibrate(pattern, -1);
        } catch (Throwable ignored) {}
    }

    @Override public void close() {
        closed = true;
        ConvoyForegroundAlertBus.unregister(this);
        queue.clear();
        if (dismissRunnable != null) main.removeCallbacks(dismissRunnable);
        dismissRunnable = null;
        if (currentDialog != null && currentDialog.isShowing()) {
            try { currentDialog.dismiss(); } catch (Throwable ignored) {}
        }
        currentDialog = null;
    }

    private static final class AlertStyle {
        final String icon;
        final String title;
        final int backgroundColor;
        final int borderColor;
        final boolean critical;
        final long durationMs;

        AlertStyle(String icon, String title, int backgroundColor, int borderColor, boolean critical, long durationMs) {
            this.icon = icon;
            this.title = title;
            this.backgroundColor = backgroundColor;
            this.borderColor = borderColor;
            this.critical = critical;
            this.durationMs = durationMs;
        }
    }
}
