package ch.piiwii.waarchive;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int TEXT = Color.rgb(17, 27, 33);
    private static final int META = Color.rgb(102, 119, 129);
    private static final int GREEN = Color.rgb(0, 128, 105);
    private static final int SENT = Color.rgb(217, 253, 211);
    private static final int RECEIVED = Color.WHITE;
    private static final int WALLPAPER = Color.rgb(239, 234, 226);

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private TextView text(String value, float sp, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setIncludeFontPadding(false);
        v.setGravity(Gravity.CENTER_VERTICAL);
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

    private GradientDrawable bubbleBackground(boolean sent) {
        float r = dp(9);
        float small = dp(3);
        GradientDrawable d = new GradientDrawable();
        d.setColor(sent ? SENT : RECEIVED);
        if (sent) {
            d.setCornerRadii(new float[]{r, r, small, small, r, r, r, r});
        } else {
            d.setCornerRadii(new float[]{small, small, r, r, r, r, r, r});
        }
        return d;
    }

    private LinearLayout createBubble(boolean sent) {
        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(dp(9), dp(6), dp(8), dp(5));
        bubble.setBackground(bubbleBackground(sent));
        bubble.setElevation(dp(0.6f));
        bubble.setMinimumWidth(dp(72));
        return bubble;
    }

    private void addBubbleRow(LinearLayout list, LinearLayout bubble, boolean sent) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(sent ? Gravity.END : Gravity.START);
        row.setPadding(dp(7), dp(1.5f), dp(7), dp(1.5f));
        LinearLayout.LayoutParams bubbleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        bubbleLp.leftMargin = sent ? dp(54) : 0;
        bubbleLp.rightMargin = sent ? 0 : dp(54);
        row.addView(bubble, bubbleLp);
        list.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void addMeta(LinearLayout bubble, String time, boolean sent) {
        TextView meta = text(time + (sent ? "  ✓✓" : ""), 10.5f, META);
        meta.setGravity(Gravity.END);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.END;
        lp.leftMargin = dp(28);
        lp.topMargin = dp(1);
        bubble.addView(meta, lp);
    }

    private void addTextMessage(LinearLayout list, String message, String time, boolean sent) {
        LinearLayout bubble = createBubble(sent);
        TextView body = text(message, 15.5f, TEXT);
        body.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.76f));
        body.setLineSpacing(0, 1.04f);
        bubble.addView(body, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        addMeta(bubble, time, sent);
        addBubbleRow(list, bubble, sent);
    }

    private void addPhotoMessage(LinearLayout list, String caption, String time, boolean sent) {
        LinearLayout bubble = createBubble(sent);
        bubble.setPadding(dp(4), dp(4), dp(4), dp(5));

        PhotoPreview preview = new PhotoPreview(this);
        preview.setBackground(rounded(Color.rgb(179, 211, 220), 8));
        bubble.addView(preview, new LinearLayout.LayoutParams(dp(250), dp(155)));

        if (caption != null && !caption.isEmpty()) {
            TextView body = text(caption, 15, TEXT);
            LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            bodyLp.leftMargin = dp(6);
            bodyLp.rightMargin = dp(6);
            bodyLp.topMargin = dp(5);
            bubble.addView(body, bodyLp);
        }
        addMeta(bubble, time, sent);
        addBubbleRow(list, bubble, sent);
    }

    private void addAudioMessage(LinearLayout list, String time, boolean sent) {
        LinearLayout bubble = createBubble(sent);
        LinearLayout line = new LinearLayout(this);
        line.setGravity(Gravity.CENTER_VERTICAL);

        TextView play = text("▶", 15, Color.WHITE);
        play.setGravity(Gravity.CENTER);
        play.setBackground(circle(GREEN));
        line.addView(play, new LinearLayout.LayoutParams(dp(40), dp(40)));

        LinearLayout audioBody = new LinearLayout(this);
        audioBody.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams audioLp = new LinearLayout.LayoutParams(dp(205), ViewGroup.LayoutParams.WRAP_CONTENT);
        audioLp.leftMargin = dp(8);

        Waveform waveform = new Waveform(this);
        audioBody.addView(waveform, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(27)));
        TextView duration = text("0:18", 10.5f, META);
        duration.setGravity(Gravity.START);
        audioBody.addView(duration);
        line.addView(audioBody, audioLp);

        bubble.addView(line);
        addMeta(bubble, time, sent);
        addBubbleRow(list, bubble, sent);
    }

    private TextView chip(String value, int backgroundColor, int foregroundColor, float sp) {
        TextView chip = text(value, sp, foregroundColor);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(10), dp(5), dp(10), dp(5));
        chip.setBackground(rounded(backgroundColor, 8));
        chip.setElevation(dp(0.5f));
        return chip;
    }

    private View createHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(4), 0, dp(4), 0);
        header.setBackgroundColor(GREEN);

        TextView back = text("‹", 37, Color.WHITE);
        back.setGravity(Gravity.CENTER);
        header.addView(back, new LinearLayout.LayoutParams(dp(38), dp(60)));

        TextView avatar = text("F", 18, Color.WHITE);
        avatar.setGravity(Gravity.CENTER);
        avatar.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        avatar.setBackground(circle(Color.rgb(113, 156, 169)));
        LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(dp(41), dp(41));
        avatarLp.rightMargin = dp(10);
        header.addView(avatar, avatarLp);

        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        names.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("Fanny", 17.5f, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        TextView subtitle = text("archive • lecture seule", 12, Color.rgb(216, 238, 235));
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(1);
        names.addView(title);
        names.addView(subtitle, subLp);
        header.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        TextView video = text("▣", 21, Color.WHITE);
        video.setGravity(Gravity.CENTER);
        header.addView(video, new LinearLayout.LayoutParams(dp(38), dp(60)));

        TextView call = text("☎", 20, Color.WHITE);
        call.setGravity(Gravity.CENTER);
        header.addView(call, new LinearLayout.LayoutParams(dp(38), dp(60)));

        TextView menu = text("⋮", 26, Color.WHITE);
        menu.setGravity(Gravity.CENTER);
        header.addView(menu, new LinearLayout.LayoutParams(dp(34), dp(60)));

        return header;
    }

    private View createReadOnlyComposer() {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(6), dp(6), dp(6), dp(7));
        bar.setBackgroundColor(WALLPAPER);

        LinearLayout field = new LinearLayout(this);
        field.setGravity(Gravity.CENTER_VERTICAL);
        field.setPadding(dp(6), 0, dp(5), 0);
        field.setBackground(rounded(Color.WHITE, 24));

        TextView smile = text("☺", 23, Color.rgb(84, 101, 111));
        smile.setGravity(Gravity.CENTER);
        field.addView(smile, new LinearLayout.LayoutParams(dp(38), dp(45)));

        TextView hint = text("Archive en lecture seule", 15, Color.rgb(102, 119, 129));
        field.addView(hint, new LinearLayout.LayoutParams(0, dp(45), 1f));

        TextView attach = text("⌕", 23, Color.rgb(84, 101, 111));
        attach.setRotation(-35f);
        attach.setGravity(Gravity.CENTER);
        field.addView(attach, new LinearLayout.LayoutParams(dp(38), dp(45)));

        TextView camera = text("▣", 20, Color.rgb(84, 101, 111));
        camera.setGravity(Gravity.CENTER);
        field.addView(camera, new LinearLayout.LayoutParams(dp(38), dp(45)));

        bar.addView(field, new LinearLayout.LayoutParams(0, dp(48), 1f));

        TextView lock = text("🔒", 17, Color.WHITE);
        lock.setGravity(Gravity.CENTER);
        lock.setBackground(circle(GREEN));
        LinearLayout.LayoutParams lockLp = new LinearLayout.LayoutParams(dp(48), dp(48));
        lockLp.leftMargin = dp(6);
        bar.addView(lock, lockLp);
        return bar;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        w.setStatusBarColor(Color.rgb(0, 105, 92));
        w.setNavigationBarColor(Color.rgb(245, 241, 235));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(WALLPAPER);
        root.addView(createHeader(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(60)));

        FrameLayout conversation = new FrameLayout(this);
        conversation.setBackgroundColor(WALLPAPER);
        conversation.addView(new WallpaperView(this), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(Color.TRANSPARENT);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(9), 0, dp(12));
        list.setBackgroundColor(Color.TRANSPARENT);

        LinearLayout dateRow = new LinearLayout(this);
        dateRow.setGravity(Gravity.CENTER);
        dateRow.addView(chip("25 AOÛT 2026", Color.rgb(255, 255, 255), Color.rgb(84, 101, 111), 11.5f));
        LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        dateLp.bottomMargin = dp(9);
        list.addView(dateRow, dateLp);

        LinearLayout securityRow = new LinearLayout(this);
        securityRow.setGravity(Gravity.CENTER);
        TextView security = chip("🔒  Cette conversation est une archive locale. Aucun message ne peut être envoyé.",
                Color.rgb(255, 244, 199), Color.rgb(84, 101, 111), 11.5f);
        security.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.88f));
        LinearLayout.LayoutParams securityLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        securityLp.bottomMargin = dp(9);
        securityRow.addView(security, securityLp);
        list.addView(securityRow);

        addTextMessage(list, "Salut 🙂 Tu vois mieux la conversation comme ça ?", "00:31", false);
        addTextMessage(list, "Oui, là on commence enfin à avoir une vraie base visuelle WhatsApp.", "00:31", true);
        addTextMessage(list, "Les bulles sont beaucoup plus propres.", "00:32", false);
        addPhotoMessage(list, "Les photos auront leur aperçu directement dans la discussion.", "00:32", true);
        addAudioMessage(list, "00:33", false);
        addTextMessage(list, "Et les vocaux garderont un lecteur intégré dans la bulle.", "00:33", true);
        addTextMessage(list, "Ensuite on remplace ces messages de démonstration par la vraie base SQLite de l’archive.", "00:34", false);
        addTextMessage(list, "Sans toucher à la fondation qui fonctionne.", "00:34", true);

        scroll.addView(list, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        conversation.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(conversation, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        root.addView(createReadOnlyComposer(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }

    private class WallpaperView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        WallpaperView(Activity context) {
            super(context);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(Color.argb(26, 90, 97, 101));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int step = dp(82);
            for (int y = dp(20); y < getHeight(); y += step) {
                for (int x = dp(18); x < getWidth(); x += step) {
                    int shift = ((y / step) % 2) * dp(31);
                    float cx = x + shift;
                    canvas.drawCircle(cx, y, dp(7), paint);
                    canvas.drawLine(cx + dp(14), y - dp(5), cx + dp(24), y + dp(5), paint);
                    canvas.drawCircle(cx + dp(43), y + dp(25), dp(3), paint);
                }
            }
        }
    }

    private class Waveform extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int[] bars = {4,8,12,7,15,10,6,13,17,8,5,12,9,16,11,7,14,18,10,5,12,7,15,9,6,13,8,17,11,7};

        Waveform(Activity context) {
            super(context);
            paint.setColor(Color.rgb(111, 132, 142));
            paint.setStrokeWidth(dp(1.8f));
            paint.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float gap = getWidth() / (float) bars.length;
            float mid = getHeight() / 2f;
            for (int i = 0; i < bars.length; i++) {
                float h = dp(bars[i] * 0.55f);
                float x = gap * i + gap / 2f;
                canvas.drawLine(x, mid - h / 2f, x, mid + h / 2f, paint);
            }
        }
    }

    private class PhotoPreview extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        PhotoPreview(Activity context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(150, 203, 222));
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);

            paint.setColor(Color.rgb(255, 218, 133));
            canvas.drawCircle(getWidth() * 0.72f, getHeight() * 0.28f, dp(18), paint);

            paint.setColor(Color.rgb(76, 132, 114));
            android.graphics.Path back = new android.graphics.Path();
            back.moveTo(0, getHeight());
            back.lineTo(getWidth() * 0.34f, getHeight() * 0.42f);
            back.lineTo(getWidth() * 0.58f, getHeight() * 0.72f);
            back.lineTo(getWidth() * 0.77f, getHeight() * 0.53f);
            back.lineTo(getWidth(), getHeight() * 0.79f);
            back.lineTo(getWidth(), getHeight());
            back.close();
            canvas.drawPath(back, paint);

            paint.setColor(Color.rgb(51, 102, 88));
            android.graphics.Path front = new android.graphics.Path();
            front.moveTo(0, getHeight());
            front.lineTo(getWidth() * 0.28f, getHeight() * 0.63f);
            front.lineTo(getWidth() * 0.48f, getHeight() * 0.82f);
            front.lineTo(getWidth() * 0.68f, getHeight() * 0.66f);
            front.lineTo(getWidth(), getHeight() * 0.86f);
            front.lineTo(getWidth(), getHeight());
            front.close();
            canvas.drawPath(front, paint);
        }
    }
}
