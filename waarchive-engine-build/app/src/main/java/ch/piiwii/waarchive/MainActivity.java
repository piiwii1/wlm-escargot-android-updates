package ch.piiwii.waarchive;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
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
    private static final int ICON = Color.rgb(84, 101, 111);

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
        v.setFontFeatureSettings("kern");
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

    private CharSequence messageWithMeta(String message, String time, boolean sent) {
        SpannableStringBuilder s = new SpannableStringBuilder();
        s.append(message);
        s.append("   ");
        int start = s.length();
        s.append(time);
        if (sent) s.append("  ✓✓");
        s.setSpan(new AbsoluteSizeSpan(10, true), start, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new ForegroundColorSpan(META), start, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return s;
    }

    private LinearLayout createBubble(boolean sent) {
        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(dp(sent ? 9 : 14), dp(6), dp(sent ? 14 : 9), dp(6));
        bubble.setBackground(new BubbleDrawable(sent));
        bubble.setElevation(dp(0.45f));
        bubble.setMinimumWidth(dp(58));
        return bubble;
    }

    private void addBubbleRow(LinearLayout list, LinearLayout bubble, boolean sent) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(sent ? Gravity.END : Gravity.START);
        row.setPadding(dp(5), dp(1.25f), dp(5), dp(1.25f));
        LinearLayout.LayoutParams bubbleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        bubbleLp.leftMargin = sent ? dp(48) : 0;
        bubbleLp.rightMargin = sent ? 0 : dp(48);
        row.addView(bubble, bubbleLp);
        list.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void addTextMessage(LinearLayout list, String message, String time, boolean sent) {
        LinearLayout bubble = createBubble(sent);
        TextView body = text("", 14.7f, TEXT);
        body.setText(messageWithMeta(message, time, sent));
        body.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.79f));
        body.setLineSpacing(0, 1.02f);
        bubble.addView(body, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        addBubbleRow(list, bubble, sent);
    }

    private void addPhotoMessage(LinearLayout list, String caption, String time, boolean sent) {
        LinearLayout bubble = createBubble(sent);
        bubble.setPadding(dp(sent ? 4 : 9), dp(4), dp(sent ? 9 : 4), dp(5));

        int width = Math.min(dp(276), (int) (getResources().getDisplayMetrics().widthPixels * 0.74f));
        PhotoPreview preview = new PhotoPreview(this);
        preview.setBackground(rounded(Color.rgb(193, 220, 226), 7));
        bubble.addView(preview, new LinearLayout.LayoutParams(width, Math.round(width * 0.62f)));

        if (caption != null && !caption.isEmpty()) {
            TextView body = text("", 14.6f, TEXT);
            body.setText(messageWithMeta(caption, time, sent));
            body.setLineSpacing(0, 1.02f);
            LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            bodyLp.leftMargin = dp(5);
            bodyLp.rightMargin = dp(5);
            bodyLp.topMargin = dp(5);
            bodyLp.bottomMargin = dp(1);
            bubble.addView(body, bodyLp);
        }
        addBubbleRow(list, bubble, sent);
    }

    private void addAudioMessage(LinearLayout list, String time, boolean sent) {
        LinearLayout bubble = createBubble(sent);
        bubble.setPadding(dp(sent ? 9 : 14), dp(7), dp(sent ? 14 : 9), dp(6));
        bubble.setMinimumWidth(dp(282));

        LinearLayout line = new LinearLayout(this);
        line.setGravity(Gravity.CENTER_VERTICAL);

        IconView play = new IconView(this, IconView.PLAY, Color.WHITE);
        play.setBackground(circle(GREEN));
        line.addView(play, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout audioBody = new LinearLayout(this);
        audioBody.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams audioLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        audioLp.leftMargin = dp(9);

        Waveform waveform = new Waveform(this);
        audioBody.addView(waveform, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(26)));

        LinearLayout metaRow = new LinearLayout(this);
        metaRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView duration = text("0:18", 10.5f, META);
        metaRow.addView(duration, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView stamp = text(time + (sent ? "  ✓✓" : ""), 10.5f, META);
        stamp.setGravity(Gravity.END);
        metaRow.addView(stamp);
        LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        metaLp.topMargin = dp(1);
        audioBody.addView(metaRow, metaLp);
        line.addView(audioBody, audioLp);

        TextView speaker = text("F", 12, Color.WHITE);
        speaker.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        speaker.setGravity(Gravity.CENTER);
        speaker.setBackground(circle(Color.rgb(113, 156, 169)));
        LinearLayout.LayoutParams speakerLp = new LinearLayout.LayoutParams(dp(34), dp(34));
        speakerLp.leftMargin = dp(8);
        line.addView(speaker, speakerLp);

        bubble.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addBubbleRow(list, bubble, sent);
    }

    private TextView chip(String value, int backgroundColor, int foregroundColor, float sp) {
        TextView chip = text(value, sp, foregroundColor);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(9), dp(4), dp(9), dp(4));
        chip.setBackground(rounded(backgroundColor, 7));
        chip.setElevation(dp(0.35f));
        return chip;
    }

    private View createHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(3), 0, dp(3), 0);
        header.setBackgroundColor(GREEN);

        IconView back = new IconView(this, IconView.BACK, Color.WHITE);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(39), dp(56)));

        TextView avatar = text("F", 17, Color.WHITE);
        avatar.setGravity(Gravity.CENTER);
        avatar.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        avatar.setBackground(circle(Color.rgb(117, 160, 174)));
        LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(dp(39), dp(39));
        avatarLp.rightMargin = dp(9);
        header.addView(avatar, avatarLp);

        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        names.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("Fanny", 16.7f, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        TextView subtitle = text("archive locale", 11.4f, Color.rgb(214, 237, 233));
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(1);
        names.addView(title);
        names.addView(subtitle, subLp);
        header.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        header.addView(new IconView(this, IconView.VIDEO, Color.WHITE), new LinearLayout.LayoutParams(dp(39), dp(56)));
        header.addView(new IconView(this, IconView.CALL, Color.WHITE), new LinearLayout.LayoutParams(dp(39), dp(56)));
        header.addView(new IconView(this, IconView.MORE, Color.WHITE), new LinearLayout.LayoutParams(dp(34), dp(56)));

        header.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = insets.getSystemWindowInsetTop();
            v.setPadding(dp(3), top, dp(3), 0);
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            if (lp != null) {
                lp.height = dp(56) + top;
                v.setLayoutParams(lp);
            }
            return insets;
        });
        return header;
    }

    private View createReadOnlyComposer() {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(6), dp(5), dp(6), dp(6));
        bar.setBackgroundColor(WALLPAPER);

        LinearLayout field = new LinearLayout(this);
        field.setGravity(Gravity.CENTER_VERTICAL);
        field.setPadding(dp(3), 0, dp(3), 0);
        field.setBackground(rounded(Color.WHITE, 23));
        field.setElevation(dp(0.25f));

        field.addView(new IconView(this, IconView.SMILE, ICON), new LinearLayout.LayoutParams(dp(39), dp(45)));

        LinearLayout hintBox = new LinearLayout(this);
        hintBox.setGravity(Gravity.CENTER_VERTICAL);
        TextView hint = text("Archive en lecture seule", 14.4f, Color.rgb(111, 126, 134));
        hintBox.addView(hint, new LinearLayout.LayoutParams(0, dp(45), 1f));
        IconView tinyLock = new IconView(this, IconView.LOCK, Color.rgb(135, 148, 154));
        hintBox.addView(tinyLock, new LinearLayout.LayoutParams(dp(24), dp(45)));
        field.addView(hintBox, new LinearLayout.LayoutParams(0, dp(45), 1f));

        field.addView(new IconView(this, IconView.ATTACH, ICON), new LinearLayout.LayoutParams(dp(38), dp(45)));
        field.addView(new IconView(this, IconView.CAMERA, ICON), new LinearLayout.LayoutParams(dp(38), dp(45)));

        bar.addView(field, new LinearLayout.LayoutParams(0, dp(48), 1f));

        FrameLayout disabledMic = new FrameLayout(this);
        disabledMic.setBackground(circle(Color.rgb(116, 184, 168)));
        disabledMic.setAlpha(0.68f);
        IconView mic = new IconView(this, IconView.MIC, Color.WHITE);
        disabledMic.addView(mic, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout.LayoutParams micLp = new LinearLayout.LayoutParams(dp(48), dp(48));
        micLp.leftMargin = dp(6);
        bar.addView(disabledMic, micLp);

        bar.setOnApplyWindowInsetsListener((v, insets) -> {
            int bottom = insets.getSystemWindowInsetBottom();
            v.setPadding(dp(6), dp(5), dp(6), dp(6) + bottom);
            return insets;
        });
        return bar;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        w.setStatusBarColor(GREEN);
        w.setNavigationBarColor(Color.rgb(245, 241, 235));
        w.getDecorView().setSystemUiVisibility(0);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(WALLPAPER);

        View header = createHeader();
        root.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        FrameLayout conversation = new FrameLayout(this);
        conversation.setBackgroundColor(WALLPAPER);
        conversation.addView(new WallpaperView(this), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(Color.TRANSPARENT);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(8), 0, dp(10));
        list.setBackgroundColor(Color.TRANSPARENT);

        LinearLayout dateRow = new LinearLayout(this);
        dateRow.setGravity(Gravity.CENTER);
        dateRow.addView(chip("25 AOÛT 2026", Color.argb(242, 248, 251, 252), Color.rgb(84, 101, 111), 10.8f));
        LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        dateLp.bottomMargin = dp(8);
        list.addView(dateRow, dateLp);

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
        root.addView(conversation, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        View composer = createReadOnlyComposer();
        root.addView(composer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
        header.post(header::requestApplyInsets);
        composer.post(composer::requestApplyInsets);
    }

    private class BubbleDrawable extends Drawable {
        private final boolean sent;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final RectF rect = new RectF();

        BubbleDrawable(boolean sent) {
            this.sent = sent;
            paint.setColor(sent ? SENT : RECEIVED);
            paint.setStyle(Paint.Style.FILL);
        }

        @Override
        public void draw(Canvas canvas) {
            float tail = dp(6);
            float radius = dp(8.5f);
            if (sent) {
                rect.set(0, 0, getBounds().width() - tail, getBounds().height());
                canvas.drawRoundRect(rect, radius, radius, paint);
                path.reset();
                path.moveTo(getBounds().width() - tail - dp(1), dp(2));
                path.lineTo(getBounds().width(), dp(2));
                path.lineTo(getBounds().width() - tail, dp(10));
                path.close();
            } else {
                rect.set(tail, 0, getBounds().width(), getBounds().height());
                canvas.drawRoundRect(rect, radius, radius, paint);
                path.reset();
                path.moveTo(tail + dp(1), dp(2));
                path.lineTo(0, dp(2));
                path.lineTo(tail, dp(10));
                path.close();
            }
            canvas.drawPath(path, paint);
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
        @Override public void setColorFilter(android.graphics.ColorFilter colorFilter) { paint.setColorFilter(colorFilter); invalidateSelf(); }
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
    }

    private class WallpaperView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        WallpaperView(Context context) {
            super(context);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(0.65f));
            paint.setColor(Color.argb(14, 78, 88, 92));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int step = dp(54);
            for (int y = dp(15); y < getHeight(); y += step) {
                for (int x = dp(10); x < getWidth(); x += step) {
                    int shift = ((y / step) % 2) * dp(24);
                    float cx = x + shift;
                    canvas.drawCircle(cx, y, dp(5), paint);
                    canvas.drawLine(cx + dp(10), y - dp(4), cx + dp(18), y + dp(4), paint);
                    canvas.drawCircle(cx + dp(29), y + dp(15), dp(2), paint);
                    canvas.drawArc(cx + dp(36), y - dp(2), cx + dp(46), y + dp(8), 210, 110, false, paint);
                }
            }
        }
    }

    private class Waveform extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int[] bars = {4,8,12,7,15,10,6,13,17,8,5,12,9,16,11,7,14,18,10,5,12,7,15,9,6,13,8,17,11,7,15,9,12,6,14,10};

        Waveform(Context context) {
            super(context);
            paint.setColor(Color.rgb(115, 135, 145));
            paint.setStrokeWidth(dp(1.55f));
            paint.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float gap = getWidth() / (float) bars.length;
            float mid = getHeight() / 2f;
            for (int i = 0; i < bars.length; i++) {
                float h = dp(bars[i] * 0.52f);
                float x = gap * i + gap / 2f;
                canvas.drawLine(x, mid - h / 2f, x, mid + h / 2f, paint);
            }
        }
    }

    private class PhotoPreview extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        PhotoPreview(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            paint.setColor(Color.rgb(145, 205, 222));
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            paint.setColor(Color.rgb(255, 214, 123));
            canvas.drawCircle(getWidth() * 0.73f, getHeight() * 0.28f, dp(23), paint);
            paint.setColor(Color.rgb(77, 139, 115));
            path.reset();
            path.moveTo(0, getHeight());
            path.lineTo(getWidth() * 0.34f, getHeight() * 0.42f);
            path.lineTo(getWidth() * 0.58f, getHeight() * 0.72f);
            path.lineTo(getWidth() * 0.78f, getHeight() * 0.49f);
            path.lineTo(getWidth(), getHeight() * 0.75f);
            path.lineTo(getWidth(), getHeight());
            path.close();
            canvas.drawPath(path, paint);
            paint.setColor(Color.rgb(47, 104, 92));
            path.reset();
            path.moveTo(0, getHeight());
            path.lineTo(getWidth() * 0.29f, getHeight() * 0.63f);
            path.lineTo(getWidth() * 0.48f, getHeight() * 0.80f);
            path.lineTo(getWidth() * 0.68f, getHeight() * 0.66f);
            path.lineTo(getWidth(), getHeight() * 0.83f);
            path.lineTo(getWidth(), getHeight());
            path.close();
            canvas.drawPath(path, paint);
        }
    }

    private class IconView extends View {
        static final int BACK = 1, VIDEO = 2, CALL = 3, MORE = 4, SMILE = 5, ATTACH = 6, CAMERA = 7, MIC = 8, LOCK = 9, PLAY = 10;
        private final int type;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        IconView(Context context, int type, int color) {
            super(context);
            this.type = type;
            p.setColor(color);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(1.8f));
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            setClickable(true);
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            float w = getWidth(), h = getHeight();
            float cx = w / 2f, cy = h / 2f;
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(1.8f));
            path.reset();
            switch (type) {
                case BACK:
                    path.moveTo(cx + dp(5), cy - dp(10)); path.lineTo(cx - dp(5), cy); path.lineTo(cx + dp(5), cy + dp(10)); c.drawPath(path, p); break;
                case VIDEO:
                    c.drawRoundRect(cx - dp(10), cy - dp(7), cx + dp(5), cy + dp(7), dp(2), dp(2), p);
                    path.moveTo(cx + dp(5), cy - dp(4)); path.lineTo(cx + dp(11), cy - dp(7)); path.lineTo(cx + dp(11), cy + dp(7)); path.lineTo(cx + dp(5), cy + dp(4)); c.drawPath(path, p); break;
                case CALL:
                    p.setStrokeWidth(dp(3.5f));
                    c.drawArc(cx - dp(10), cy - dp(10), cx + dp(10), cy + dp(10), 135, 90, false, p);
                    p.setStrokeWidth(dp(1.8f));
                    c.drawRoundRect(cx - dp(11), cy - dp(9), cx - dp(6), cy - dp(3), dp(2), dp(2), p);
                    c.drawRoundRect(cx + dp(6), cy + dp(3), cx + dp(11), cy + dp(9), dp(2), dp(2), p); break;
                case MORE:
                    p.setStyle(Paint.Style.FILL); c.drawCircle(cx, cy - dp(7), dp(1.7f), p); c.drawCircle(cx, cy, dp(1.7f), p); c.drawCircle(cx, cy + dp(7), dp(1.7f), p); break;
                case SMILE:
                    c.drawCircle(cx, cy, dp(9), p); p.setStyle(Paint.Style.FILL); c.drawCircle(cx - dp(3), cy - dp(2), dp(1), p); c.drawCircle(cx + dp(3), cy - dp(2), dp(1), p); p.setStyle(Paint.Style.STROKE); c.drawArc(cx - dp(5), cy - dp(1), cx + dp(5), cy + dp(6), 20, 140, false, p); break;
                case ATTACH:
                    c.drawArc(cx - dp(7), cy - dp(10), cx + dp(7), cy + dp(7), 205, 285, false, p); c.drawArc(cx - dp(4), cy - dp(7), cx + dp(4), cy + dp(5), 205, 285, false, p); break;
                case CAMERA:
                    c.drawRoundRect(cx - dp(10), cy - dp(7), cx + dp(10), cy + dp(8), dp(2), dp(2), p); c.drawCircle(cx, cy, dp(4), p); path.moveTo(cx - dp(5), cy - dp(7)); path.lineTo(cx - dp(2), cy - dp(10)); path.lineTo(cx + dp(3), cy - dp(10)); path.lineTo(cx + dp(6), cy - dp(7)); c.drawPath(path, p); break;
                case MIC:
                    c.drawRoundRect(cx - dp(4), cy - dp(10), cx + dp(4), cy + dp(3), dp(4), dp(4), p); c.drawArc(cx - dp(8), cy - dp(2), cx + dp(8), cy + dp(9), 0, 180, false, p); c.drawLine(cx, cy + dp(8), cx, cy + dp(12), p); c.drawLine(cx - dp(4), cy + dp(12), cx + dp(4), cy + dp(12), p); break;
                case LOCK:
                    c.drawRoundRect(cx - dp(6), cy - dp(1), cx + dp(6), cy + dp(8), dp(1.5f), dp(1.5f), p); c.drawArc(cx - dp(5), cy - dp(8), cx + dp(5), cy + dp(3), 185, 170, false, p); break;
                case PLAY:
                    p.setStyle(Paint.Style.FILL); path.moveTo(cx - dp(3), cy - dp(6)); path.lineTo(cx + dp(6), cy); path.lineTo(cx - dp(3), cy + dp(6)); path.close(); c.drawPath(path, p); break;
            }
        }
    }
}
