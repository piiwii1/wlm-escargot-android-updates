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

final class WidgetRenderer {
    private static final float SKIN_W = 1597f;
    private static final float SKIN_H = 595f;
    private static final float SKIN_ASPECT = SKIN_W / SKIN_H;
    private static final int RED = Color.rgb(245, 24, 31);
    private static final int WHITE = Color.rgb(248, 248, 250);
    private static final int MUTED = Color.rgb(112, 116, 124);

    private WidgetRenderer() {}

    static Bitmap render(Context context, int widthDp, int heightDp) {
        float density = context.getResources().getDisplayMetrics().density;
        float supersample = density <= 1.5f ? 2.0f : 1.0f;
        int width = Math.max(1, Math.round(widthDp * density * supersample));
        int height = Math.max(1, Math.round(heightDp * density * supersample));
        Bitmap out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        canvas.drawColor(Color.TRANSPARENT);

        RectF panel = fitPanel(width, height);
        Bitmap skin = BitmapFactory.decodeResource(context.getResources(), R.drawable.altimeter_skin_reference);
        Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        if (skin != null) canvas.drawBitmap(skin, null, panel, bitmapPaint);

        canvas.save();
        canvas.translate(panel.left, panel.top);
        canvas.scale(panel.width() / SKIN_W, panel.height() / SKIN_H);
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
        double altitude = AltitudeState.displayAltitude(context);
        boolean hasAltitude = hasFineLocation(context) && !Double.isNaN(altitude);
        boolean gpsFresh = AltitudeState.isGpsFresh(context);
        drawAltitude(c, hasAltitude ? Math.round(altitude) : null);
        drawTrend(c, context, hasAltitude);
        drawBottomBand(c, context, gpsFresh);
        drawHeadingMarker(c, context);
    }

    private static void drawAltitude(Canvas c, Long altitude) {
        final float left = 680f;
        final float right = 1285f;
        final float baseline = 418f;
        String number = altitude == null ? "---" : Long.toString(altitude);

        Paint value = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG | Paint.DITHER_FLAG);
        value.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        value.setTextSize(220f);
        value.setShader(new LinearGradient(0f, 220f, 0f, baseline,
                new int[]{Color.WHITE, Color.rgb(250, 250, 252), Color.rgb(180, 182, 188)},
                new float[]{0f, .58f, 1f}, Shader.TileMode.CLAMP));
        value.setShadowLayer(11f, 0f, 6f, Color.argb(150, 220, 14, 20));
        fitText(value, number, altitude == null ? 410f : 500f, 122f);
        c.drawText(number, left, baseline, value);
        value.clearShadowLayer();
        value.setShader(null);

        float x = left + value.measureText(number) + 18f;
        Paint unit = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG | Paint.DITHER_FLAG);
        unit.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        unit.setTextSize(128f);
        unit.setShader(new LinearGradient(0f, 285f, 0f, baseline,
                Color.WHITE, Color.rgb(178, 181, 188), Shader.TileMode.CLAMP));
        unit.setShadowLayer(6f, 0f, 3f, Color.argb(120, 210, 14, 20));
        fitText(unit, "m", Math.max(68f, right - x), 80f);
        c.drawText("m", x, baseline, unit);
        unit.clearShadowLayer();
        unit.setShader(null);
    }

    private static void drawTrend(Canvas c, Context context, boolean hasAltitude) {
        final float cx = 1455f;
        double trend = hasAltitude ? AltitudeState.trend(context) : Double.NaN;
        long trendAge = System.currentTimeMillis() - AltitudeState.trendTime(context);
        if (trendAge > 90000L) trend = Double.NaN;
        boolean neutral = Double.isNaN(trend) || Math.abs(trend) < 1.5;
        boolean up = !neutral && trend > 0;

        Paint arrow = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        arrow.setStyle(Paint.Style.STROKE);
        arrow.setStrokeCap(Paint.Cap.ROUND);
        arrow.setStrokeJoin(Paint.Join.ROUND);
        arrow.setStrokeWidth(6f);
        int arrowColor = !hasAltitude ? Color.rgb(85, 88, 96)
                : neutral ? Color.rgb(188, 191, 198)
                : up ? RED : Color.rgb(105, 160, 225);
        arrow.setColor(arrowColor);
        if (!neutral && hasAltitude) {
            arrow.setShadowLayer(10f, 0f, 0f,
                    up ? Color.argb(145, 255, 20, 28) : Color.argb(120, 90, 150, 230));
        }

        // Outline only: never fill the triangle. The center stays transparent.
        Path tri = new Path();
        if (neutral || up) {
            tri.moveTo(cx, 175f);
            tri.lineTo(cx - 45f, 253f);
            tri.lineTo(cx + 45f, 253f);
        } else {
            tri.moveTo(cx, 253f);
            tri.lineTo(cx - 45f, 175f);
            tri.lineTo(cx + 45f, 175f);
        }
        tri.close();
        c.drawPath(tri, arrow);
        arrow.clearShadowLayer();

        String text;
        if (!hasAltitude || Double.isNaN(trend)) text = "—";
        else if (neutral) text = "0 m";
        else text = (up ? "+" : "−") + Math.abs(Math.round(trend)) + " m";

        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        tp.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        tp.setColor(hasAltitude ? WHITE : MUTED);
        tp.setTextAlign(Paint.Align.CENTER);
        tp.setTextSize(45f);
        fitText(tp, text, 175f, 29f);
        c.drawText(text, cx, 325f, tp);
    }

    private static void drawBottomBand(Canvas c, Context context, boolean gpsFresh) {
        drawGpsPin(c);

        Paint label = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        label.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        label.setColor(WHITE);
        label.setTextSize(48f);
        c.drawText("GPS", 760f, 522f, label);

        drawGpsBars(c, context);

        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setColor(Color.rgb(158, 161, 168));
        line.setStrokeWidth(3f);
        c.drawLine(1030f, 456f, 1030f, 548f, line);

        drawCrosshair(c, 1110f, 500f, 30f);

        Paint precision = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        precision.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        precision.setColor(WHITE);
        precision.setTextSize(43f);
        fitText(precision, "PRÉCISION", 225f, 31f);
        c.drawText("PRÉCISION", 1160f, 518f, precision);

        drawAccuracy(c, context, gpsFresh);
    }

    private static void drawGpsPin(Canvas c) {
        final float cx = 700f;
        final float cy = 485f;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        p.setColor(RED);
        p.setShadowLayer(8f, 0f, 0f, Color.argb(150, 255, 22, 28));
        Path pin = new Path();
        pin.moveTo(cx, 539f);
        pin.cubicTo(cx - 31f, 505f, cx - 31f, 474f, cx, 458f);
        pin.cubicTo(cx + 31f, 474f, cx + 31f, 505f, cx, 539f);
        pin.close();
        c.drawPath(pin, p);
        p.clearShadowLayer();
        p.setColor(Color.rgb(13, 13, 16));
        c.drawCircle(cx, 482f, 10f, p);
    }

    private static void drawGpsBars(Canvas c, Context context) {
        boolean callbackFresh = AltitudeState.isRawGpsFresh(context);
        int bars = gpsBars(AltitudeState.hAcc(context), callbackFresh);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

        // Entirely inside the lower band: top of tallest bar is below the divider at y=435.
        final float x = 865f;
        final float baseline = 535f;
        final float barW = 21f;
        final float gap = 12f;
        final float[] heights = {24f, 41f, 59f, 77f};

        for (int i = 0; i < 4; i++) {
            float l = x + i * (barW + gap);
            float t = baseline - heights[i];
            RectF slot = new RectF(l, t, l + barW, baseline);
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.rgb(34, 36, 42));
            c.drawRoundRect(slot, 5f, 5f, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2.2f);
            p.setColor(Color.rgb(88, 92, 101));
            c.drawRoundRect(slot, 5f, 5f, p);
            if (i < bars) {
                RectF fill = new RectF(l + 3f, t + 3f, l + barW - 3f, baseline - 3f);
                p.setStyle(Paint.Style.FILL);
                p.setColor(RED);
                p.setShadowLayer(6f, 0f, 0f, Color.argb(150, 255, 24, 30));
                c.drawRoundRect(fill, 3f, 3f, p);
                p.clearShadowLayer();
            }
        }
    }

    private static void drawCrosshair(Canvas c, float cx, float cy, float r) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(4f);
        p.setColor(WHITE);
        c.drawCircle(cx, cy, r, p);
        c.drawLine(cx - r - 11f, cy, cx - r + 9f, cy, p);
        c.drawLine(cx + r - 9f, cy, cx + r + 11f, cy, p);
        c.drawLine(cx, cy - r - 11f, cx, cy - r + 9f, p);
        c.drawLine(cx, cy + r - 9f, cx, cy + r + 11f, p);
        p.setStyle(Paint.Style.FILL);
        c.drawCircle(cx, cy, 6f, p);
    }

    private static void drawAccuracy(Canvas c, Context context, boolean gpsFresh) {
        String value = "—";
        boolean good = false;
        if (AltitudeState.isRawGpsFresh(context)) {
            float acc = bestVisibleAccuracy(context);
            if (!Float.isNaN(acc)) {
                value = "±" + Math.max(1, Math.round(acc)) + " m";
                good = acc <= 120f;
            }
        }

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG | Paint.DITHER_FLAG);
        p.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        p.setColor(good ? WHITE : MUTED);
        p.setTextAlign(Paint.Align.RIGHT);
        p.setTextSize(47f);
        fitText(p, value, 150f, 30f);
        c.drawText(value, 1528f, 520f, p);
    }

    private static float bestVisibleAccuracy(Context context) {
        float v = AltitudeState.vAcc(context);
        if (!Float.isNaN(v) && v > 0f && v < 500f) return v;
        float h = AltitudeState.hAcc(context);
        if (!Float.isNaN(h) && h > 0f && h < 500f) return h;
        v = AltitudeState.acceptedVAcc(context);
        if (!Float.isNaN(v) && v > 0f && v < 500f) return v;
        h = AltitudeState.acceptedHAcc(context);
        return (!Float.isNaN(h) && h > 0f && h < 500f) ? h : Float.NaN;
    }

    private static void drawHeadingMarker(Canvas c, Context context) {
        float heading = AltitudeState.heading(context);
        long age = System.currentTimeMillis() - AltitudeState.headingTime(context);
        if (Float.isNaN(heading) || age > 30000L) return;
        final float cx = 338f, cy = 297f, radius = 211f;
        double rad = Math.toRadians(heading - 90f);
        float px = cx + (float) Math.cos(rad) * radius;
        float py = cy + (float) Math.sin(rad) * radius;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(RED);
        p.setShadowLayer(7f, 0f, 0f, Color.argb(170, 255, 20, 28));
        Path tri = new Path();
        tri.moveTo(0f, -14f);
        tri.lineTo(-10f, 10f);
        tri.lineTo(10f, 10f);
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
        if (accuracy <= 8f) return 4;
        if (accuracy <= 20f) return 3;
        if (accuracy <= 45f) return 2;
        if (accuracy <= 120f) return 1;
        return 0;
    }

    private static boolean hasFineLocation(Context c) {
        return Build.VERSION.SDK_INT < 23
                || c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private static void fitText(Paint p, String text, float maxWidth, float minSize) {
        while (p.measureText(text) > maxWidth && p.getTextSize() > minSize) {
            p.setTextSize(p.getTextSize() - 1f);
        }
    }
}
