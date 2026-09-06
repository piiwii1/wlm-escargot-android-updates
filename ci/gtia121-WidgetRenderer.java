package ch.piiwii.gtialtimeter;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;

/**
 * 1.2.1: reference-skin renderer.
 *
 * The supplied visual reference is used as the actual panel skin so the mini-widget keeps the
 * same frame, compass/Matterhorn, title, chrome, glass and red lighting instead of approximating
 * them with flat Canvas primitives. Only live data fields are redrawn.
 *
 * The skin's native aspect ratio (1566:576 ~= 2.71875) is always preserved. If a launcher gives
 * a squarer AppWidget cell, the extra area stays transparent rather than stretching the design.
 */
final class WidgetRenderer {
    private static final float SKIN_W = 1566f;
    private static final float SKIN_H = 576f;
    private static final float SKIN_ASPECT = SKIN_W / SKIN_H;

    private static final int RED = Color.rgb(245, 24, 31);
    private static final int WHITE = Color.rgb(248, 248, 250);
    private static final int SILVER = Color.rgb(198, 201, 207);
    private static final int MUTED = Color.rgb(112, 116, 124);

    private WidgetRenderer() {}

    static Bitmap render(Context context, int widthDp, int heightDp) {
        float density = context.getResources().getDisplayMetrics().density;
        int width = Math.max(1, Math.round(widthDp * density));
        int height = Math.max(1, Math.round(heightDp * density));

        Bitmap out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        canvas.drawColor(Color.TRANSPARENT);

        RectF panel = fitPanel(width, height);
        Bitmap skin = BitmapFactory.decodeResource(context.getResources(), R.drawable.altimeter_skin_reference);
        Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        if (skin != null) canvas.drawBitmap(skin, null, panel, bitmapPaint);

        canvas.save();
        canvas.translate(panel.left, panel.top);
        float sx = panel.width() / SKIN_W;
        float sy = panel.height() / SKIN_H;
        canvas.scale(sx, sy);
        drawLiveData(canvas, context);
        canvas.restore();
        return out;
    }

    private static RectF fitPanel(float w, float h) {
        float panelW;
        float panelH;
        if (w / h > SKIN_ASPECT) {
            panelH = h;
            panelW = h * SKIN_ASPECT;
        } else {
            panelW = w;
            panelH = w / SKIN_ASPECT;
        }
        float left = (w - panelW) * 0.5f;
        float top = (h - panelH) * 0.5f;
        return new RectF(left, top, left + panelW, top + panelH);
    }

    private static void drawLiveData(Canvas c, Context context) {
        double altitude = AltitudeState.filtered(context);
        long fixTime = AltitudeState.fixTime(context);
        long age = fixTime <= 0 ? Long.MAX_VALUE : System.currentTimeMillis() - fixTime;
        boolean valid = hasFineLocation(context)
                && AltitudeState.gpsEnabled(context)
                && !Double.isNaN(altitude)
                && age <= 30000L;

        drawAltitude(c, valid ? Math.round(altitude) : null);
        drawTrend(c, context, valid);
        drawGpsBars(c, context, valid);
        drawAccuracy(c, context, valid);
        drawHeadingMarker(c, context);
    }

    private static void drawAltitude(Canvas c, Long altitude) {
        final float left = 666f;
        final float right = 1300f;
        final float baseline = 392f;

        String number = altitude == null ? "---" : Long.toString(altitude);
        Paint value = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG | Paint.DITHER_FLAG);
        value.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        value.setTextSize(210f);
        value.setShader(new LinearGradient(0f, 205f, 0f, baseline,
                new int[]{Color.WHITE, Color.rgb(248,248,250), Color.rgb(182,184,190)},
                new float[]{0f,.58f,1f}, Shader.TileMode.CLAMP));
        value.setShadowLayer(12f, 0f, 7f, Color.argb(190, 230, 12, 20));
        fitText(value, number, altitude == null ? 420f : 505f, 118f);
        c.drawText(number, left, baseline, value);
        value.clearShadowLayer();
        value.setShader(null);

        float x = left + value.measureText(number) + 18f;
        Paint unit = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG | Paint.DITHER_FLAG);
        unit.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        unit.setTextSize(132f);
        unit.setShader(new LinearGradient(0f, 275f, 0f, baseline,
                Color.WHITE, Color.rgb(180,183,190), Shader.TileMode.CLAMP));
        unit.setShadowLayer(7f, 0f, 4f, Color.argb(155, 225, 14, 22));
        fitText(unit, "m", Math.max(70f, right - x), 82f);
        c.drawText("m", x, baseline, unit);
        unit.clearShadowLayer();
        unit.setShader(null);
    }

    private static void drawTrend(Canvas c, Context context, boolean valid) {
        final float cx = 1416f;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        p.setStrokeCap(Paint.Cap.ROUND);

        double trend = valid ? AltitudeState.trend(context) : Double.NaN;
        boolean neutral = Double.isNaN(trend) || Math.abs(trend) < 1.5;
        boolean up = !neutral && trend > 0;

        if (neutral) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(6f);
            p.setColor(valid ? Color.rgb(160,163,171) : Color.rgb(95,98,106));
            Path tri = new Path();
            tri.moveTo(cx, 166f);
            tri.lineTo(cx - 48f, 244f);
            tri.lineTo(cx + 48f, 244f);
            tri.close();
            c.drawPath(tri, p);
            p.setStyle(Paint.Style.FILL);
        } else {
            int color = up ? RED : Color.rgb(105,160,225);
            p.setColor(color);
            p.setShadowLayer(18f, 0f, 0f, up ? Color.argb(215,255,20,28) : Color.argb(170,90,150,230));
            Path tri = new Path();
            if (up) {
                tri.moveTo(cx, 157f);
                tri.lineTo(cx - 49f, 242f);
                tri.lineTo(cx + 49f, 242f);
            } else {
                tri.moveTo(cx, 244f);
                tri.lineTo(cx - 49f, 159f);
                tri.lineTo(cx + 49f, 159f);
            }
            tri.close();
            c.drawPath(tri, p);
            p.clearShadowLayer();
        }

        String text;
        if (!valid) text = "--";
        else if (neutral) text = "0 m";
        else text = (up ? "+" : "−") + Math.abs(Math.round(trend)) + " m";

        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        tp.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        tp.setColor(valid ? WHITE : MUTED);
        tp.setTextAlign(Paint.Align.CENTER);
        tp.setTextSize(48f);
        fitText(tp, text, 185f, 31f);
        c.drawText(text, cx, 304f, tp);
    }

    private static void drawGpsBars(Canvas c, Context context, boolean valid) {
        int bars = gpsBars(AltitudeState.hAcc(context), AltitudeState.gpsEnabled(context) && (valid || AltitudeState.positionReceived(context)));
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        final float x = 848f;
        final float baseline = 500f;
        final float barW = 28f;
        final float gap = 15f;
        final float[] heights = {28f, 49f, 72f, 96f};
        for (int i = 0; i < 4; i++) {
            float l = x + i * (barW + gap);
            float t = baseline - heights[i];
            p.setColor(i < bars ? RED : Color.rgb(62,65,72));
            if (i < bars) p.setShadowLayer(8f,0f,0f,Color.argb(165,255,22,28));
            c.drawRect(l, t, l + barW, baseline, p);
            p.clearShadowLayer();
        }
    }

    private static void drawAccuracy(Canvas c, Context context, boolean valid) {
        float acc = !Float.isNaN(AltitudeState.vAcc(context)) ? AltitudeState.vAcc(context) : AltitudeState.hAcc(context);
        boolean usable = valid && !Float.isNaN(acc) && acc <= 120f;
        String value = usable ? "±" + Math.max(1, Math.round(acc)) + " m" : "—";

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        p.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        p.setColor(usable ? WHITE : MUTED);
        p.setTextSize(48f);
        fitText(p, value, 175f, 30f);
        c.drawText(value, 1368f, 498f, p);
    }

    private static void drawHeadingMarker(Canvas c, Context context) {
        float heading = AltitudeState.heading(context);
        long age = System.currentTimeMillis() - AltitudeState.headingTime(context);
        if (Float.isNaN(heading) || age > 30000L) return;

        final float cx = 326f;
        final float cy = 288f;
        final float radius = 205f;
        double rad = Math.toRadians(heading - 90f);
        float px = cx + (float)Math.cos(rad) * radius;
        float py = cy + (float)Math.sin(rad) * radius;

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(RED);
        p.setShadowLayer(8f,0f,0f,Color.argb(190,255,20,28));
        Path tri = new Path();
        tri.moveTo(0f, -15f);
        tri.lineTo(-11f, 11f);
        tri.lineTo(11f, 11f);
        tri.close();
        c.save();
        c.translate(px, py);
        c.rotate(heading);
        c.drawPath(tri, p);
        c.restore();
        p.clearShadowLayer();
    }

    private static int gpsBars(float accuracy, boolean enabled) {
        if (!enabled || Float.isNaN(accuracy)) return 0;
        if (accuracy < 10f) return 4;
        if (accuracy <= 25f) return 3;
        if (accuracy <= 40f) return 2;
        if (accuracy <= 70f) return 1;
        return 0;
    }

    private static boolean hasFineLocation(Context c) {
        return Build.VERSION.SDK_INT < 23 || c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private static void fitText(Paint p, String text, float maxWidth, float minSize) {
        while (p.measureText(text) > maxWidth && p.getTextSize() > minSize) p.setTextSize(p.getTextSize() - 1f);
    }
}
