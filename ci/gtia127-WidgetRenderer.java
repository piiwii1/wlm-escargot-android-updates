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
    private static final float SKIN_W = 1566f;
    private static final float SKIN_H = 576f;
    private static final float SKIN_ASPECT = SKIN_W / SKIN_H;
    private static final float SIL_W = 1672f;
    private static final float SIL_H = 941f;
    private static final float SIL_ASPECT = SIL_W / SIL_H;
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
        boolean silhouette = SkinPrefs.get(context) == SkinPrefs.SILHOUETTE;
        float aspect = silhouette ? SIL_ASPECT : SKIN_ASPECT;
        float designW = silhouette ? SIL_W : SKIN_W;
        float designH = silhouette ? SIL_H : SKIN_H;
        RectF panel = fitPanel(width, height, aspect);
        int skinRes = silhouette ? R.drawable.altimeter_skin_silhouette : R.drawable.altimeter_skin_reference;
        Bitmap skin = BitmapFactory.decodeResource(context.getResources(), skinRes);
        Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        if (skin != null) canvas.drawBitmap(skin, null, panel, bitmapPaint);
        canvas.save();
        canvas.translate(panel.left, panel.top);
        canvas.scale(panel.width() / designW, panel.height() / designH);
        if (silhouette) drawSilhouetteLiveData(canvas, context);
        else drawLiveData(canvas, context);
        canvas.restore();
        return out;
    }

    private static RectF fitPanel(float w, float h, float aspect) {
        float panelW;
        float panelH;
        if (w / h > aspect) {
            panelH = h;
            panelW = h * aspect;
        } else {
            panelW = w;
            panelH = w / aspect;
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
        drawGpsBars(c, context);
        drawAccuracy(c, context, gpsFresh);
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

    private static void drawTrend(Canvas c, Context context, boolean hasAltitude) {
        final float cx = 1418f;
        final float top = 174f;
        final float bottom = 238f;
        final float halfW = 39f;
        double trend = hasAltitude ? AltitudeState.trend(context) : Double.NaN;
        long trendAge = System.currentTimeMillis() - AltitudeState.trendTime(context);
        if (trendAge > 90000L) trend = Double.NaN;
        boolean neutral = Double.isNaN(trend) || Math.abs(trend) < 1.5;
        boolean up = !neutral && trend > 0;

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(4.5f);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeCap(Paint.Cap.ROUND);
        int arrowColor = neutral ? Color.rgb(176,179,187) : (up ? RED : Color.rgb(112,166,230));
        p.setColor(hasAltitude ? arrowColor : Color.rgb(100,103,110));
        if (!neutral && hasAltitude) {
            p.setShadowLayer(9f, 0f, 0f, up ? Color.argb(150,255,24,32) : Color.argb(120,90,150,230));
        }
        Path tri = new Path();
        if (!neutral && !up) {
            tri.moveTo(cx, bottom); tri.lineTo(cx-halfW, top); tri.lineTo(cx+halfW, top);
        } else {
            tri.moveTo(cx, top); tri.lineTo(cx-halfW, bottom); tri.lineTo(cx+halfW, bottom);
        }
        tri.close();
        c.drawPath(tri, p);
        p.clearShadowLayer();

        String text;
        if (!hasAltitude || Double.isNaN(trend) || neutral) text = "—";
        else text = (up ? "+" : "−") + Math.abs(Math.round(trend)) + " m";
        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        tp.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        tp.setColor(hasAltitude ? WHITE : MUTED);
        tp.setTextAlign(Paint.Align.CENTER);
        tp.setTextSize(46f);
        fitText(tp, text, 178f, 30f);
        c.drawText(text, cx, 300f, tp);
    }

    private static void drawGpsBars(Canvas c, Context context) {
        boolean callbackFresh = AltitudeState.isRawGpsFresh(context);
        int bars = gpsBars(AltitudeState.hAcc(context), callbackFresh);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        p.setStrokeCap(Paint.Cap.ROUND);
        final float x = 878f, baseline = 525f, barW = 17f, gap = 9f;
        final float[] heights = {20f, 34f, 49f, 64f};
        for (int i = 0; i < 4; i++) {
            float l = x + i * (barW + gap);
            float t = baseline - heights[i];
            RectF slot = new RectF(l, t, l + barW, baseline);
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.rgb(34, 36, 42));
            c.drawRoundRect(slot, 5f, 5f, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2.2f);
            p.setColor(Color.rgb(92, 96, 105));
            c.drawRoundRect(slot, 5f, 5f, p);
            if (i < bars) {
                RectF fill = new RectF(l + 3f, t + 3f, l + barW - 3f, baseline - 3f);
                p.setStyle(Paint.Style.FILL);
                p.setColor(RED);
                p.setShadowLayer(6f, 0f, 0f, Color.argb(160, 255, 24, 30));
                c.drawRoundRect(fill, 3f, 3f, p);
                p.clearShadowLayer();
            }
        }
    }

    private static void drawAccuracy(Canvas c, Context context, boolean gpsFresh) {
        String value;
        boolean good = false;
        if (AltitudeState.isRawGpsFresh(context)) {
            float acc = bestVisibleAccuracy(context);
            if (!Float.isNaN(acc)) {
                value = "±" + Math.max(1, Math.round(acc)) + " m";
                good = acc <= 120f;
            } else value = "± ?";
        } else if (AltitudeState.isBaroFresh(context)) {
            value = "BARO";
            good = true;
        } else value = "—";
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG | Paint.DITHER_FLAG);
        p.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        p.setColor(good ? WHITE : (AltitudeState.isRawGpsFresh(context) ? Color.rgb(220,220,224) : MUTED));
        p.setTextAlign(Paint.Align.RIGHT);
        p.setTextSize(46f);
        fitText(p, value, 178f, 29f);
        p.setShadowLayer(good ? 5f : 0f, 0f, 2f, Color.argb(120, 0, 0, 0));
        c.drawText(value, 1520f, 503f, p);
        p.clearShadowLayer();
    }

    private static void drawSilhouetteLiveData(Canvas c, Context context) {
        double altitude = AltitudeState.displayAltitude(context);
        boolean hasAltitude = hasFineLocation(context) && !Double.isNaN(altitude);
        drawSilhouetteAltitude(c, hasAltitude ? Math.round(altitude) : null);
        drawSilhouetteTrend(c, context, hasAltitude);
        drawSilhouetteGpsBars(c, context);
        drawSilhouetteAccuracy(c, context);
    }

    private static void drawSilhouetteAltitude(Canvas c, Long altitude) {
        String number = altitude == null ? "---" : Long.toString(altitude);
        final float left = 716f;
        final float baseline = 568f;
        final float maxRight = 1430f;

        Paint value = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG | Paint.DITHER_FLAG);
        value.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        value.setTextSize(235f);
        value.setShader(new LinearGradient(0f, 365f, 0f, baseline,
                new int[]{Color.WHITE, Color.rgb(252,252,253), Color.rgb(188,190,195)},
                new float[]{0f,.58f,1f}, Shader.TileMode.CLAMP));
        value.setShadowLayer(10f, 0f, 6f, Color.argb(190, 0, 0, 0));
        fitText(value, number, altitude == null ? 450f : 520f, 128f);
        c.drawText(number, left, baseline, value);
        value.clearShadowLayer();
        value.setShader(null);

        float x = left + value.measureText(number) + 16f;
        Paint unit = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG | Paint.DITHER_FLAG);
        unit.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        unit.setTextSize(132f);
        unit.setShader(new LinearGradient(0f, 438f, 0f, baseline,
                Color.WHITE, Color.rgb(183,186,191), Shader.TileMode.CLAMP));
        unit.setShadowLayer(7f, 0f, 4f, Color.argb(180, 0, 0, 0));
        fitText(unit, "m", Math.max(65f, maxRight - x), 78f);
        c.drawText("m", x, baseline, unit);
        unit.clearShadowLayer();
        unit.setShader(null);
    }

    private static void drawSilhouetteTrend(Canvas c, Context context, boolean hasAltitude) {
        final float cx = 1512f;
        final float top = 305f;
        final float bottom = 405f;
        final float halfW = 49f;
        double trend = hasAltitude ? AltitudeState.trend(context) : Double.NaN;
        long age = System.currentTimeMillis() - AltitudeState.trendTime(context);
        if (age > 90000L) trend = Double.NaN;
        boolean neutral = Double.isNaN(trend) || Math.abs(trend) < 1.5;
        boolean up = !neutral && trend > 0;

        Path tri = new Path();
        if (!neutral && !up) {
            tri.moveTo(cx, bottom);
            tri.lineTo(cx-halfW, top);
            tri.lineTo(cx+halfW, top);
        } else {
            tri.moveTo(cx, top);
            tri.lineTo(cx-halfW, bottom);
            tri.lineTo(cx+halfW, bottom);
        }
        tri.close();

        Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadow.setStyle(Paint.Style.STROKE);
        shadow.setStrokeJoin(Paint.Join.ROUND);
        shadow.setStrokeWidth(14f);
        shadow.setColor(Color.argb(220, 12, 12, 14));
        c.drawPath(tri, shadow);

        Paint red = new Paint(Paint.ANTI_ALIAS_FLAG);
        red.setStyle(Paint.Style.STROKE);
        red.setStrokeJoin(Paint.Join.ROUND);
        red.setStrokeWidth(8f);
        red.setColor(hasAltitude ? RED : Color.rgb(95, 95, 100));
        red.setShadowLayer(8f,0f,0f,Color.argb(110,255,20,28));
        c.drawPath(tri, red);
        red.clearShadowLayer();

        Paint white = new Paint(Paint.ANTI_ALIAS_FLAG);
        white.setStyle(Paint.Style.STROKE);
        white.setStrokeJoin(Paint.Join.ROUND);
        white.setStrokeWidth(2.4f);
        white.setColor(hasAltitude ? Color.rgb(245,245,247) : Color.rgb(125,125,130));
        c.drawPath(tri, white);

        String text;
        if (!hasAltitude || Double.isNaN(trend) || neutral) text = "—";
        else text = (up ? "+" : "−") + Math.abs(Math.round(trend)) + " m";
        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        tp.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        tp.setColor(hasAltitude ? WHITE : MUTED);
        tp.setTextAlign(Paint.Align.CENTER);
        tp.setTextSize(48f);
        fitText(tp, text, 190f, 30f);
        tp.setShadowLayer(7f,0f,4f,Color.argb(210,0,0,0));
        c.drawText(text, cx, 478f, tp);
        tp.clearShadowLayer();
    }

    private static void drawSilhouetteGpsBars(Canvas c, Context context) {
        int bars = gpsBars(AltitudeState.hAcc(context), AltitudeState.isRawGpsFresh(context));
        final float x = 910f, baseline = 694f, barW = 18f, gap = 9f;
        final float[] heights = {25f, 42f, 60f, 80f};
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        for (int i=0;i<4;i++) {
            float l = x + i*(barW+gap);
            float t = baseline-heights[i];
            RectF r = new RectF(l,t,l+barW,baseline);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(3f);
            p.setColor(Color.rgb(70,70,76));
            c.drawRoundRect(r,4f,4f,p);
            if (i < bars) {
                p.setStyle(Paint.Style.FILL);
                p.setColor(RED);
                p.setShadowLayer(5f,0f,0f,Color.argb(120,255,20,28));
                RectF f = new RectF(l+3f,t+3f,l+barW-3f,baseline-3f);
                c.drawRoundRect(f,3f,3f,p);
                p.clearShadowLayer();
            }
        }
    }

    private static void drawSilhouetteAccuracy(Canvas c, Context context) {
        String value;
        if (AltitudeState.isRawGpsFresh(context)) {
            float acc = bestVisibleAccuracy(context);
            value = Float.isNaN(acc) ? "± ?" : "±" + Math.max(1, Math.round(acc)) + " m";
        } else if (AltitudeState.isBaroFresh(context)) {
            value = "BARO";
        } else value = "—";
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        p.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        p.setTextAlign(Paint.Align.RIGHT);
        p.setTextSize(47f);
        p.setColor(WHITE);
        fitText(p, value, 188f, 29f);
        p.setShadowLayer(6f,0f,3f,Color.argb(190,0,0,0));
        c.drawText(value, 1632f, 684f, p);
        p.clearShadowLayer();
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
        final float cx=326f, cy=288f, radius=205f;
        double rad=Math.toRadians(heading-90f);
        float px=cx+(float)Math.cos(rad)*radius, py=cy+(float)Math.sin(rad)*radius;
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(RED); p.setShadowLayer(8f,0f,0f,Color.argb(190,255,20,28));
        Path tri=new Path(); tri.moveTo(0f,-15f); tri.lineTo(-11f,11f); tri.lineTo(11f,11f); tri.close();
        c.save(); c.translate(px,py); c.rotate(heading); c.drawPath(tri,p); c.restore(); p.clearShadowLayer();
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
        return Build.VERSION.SDK_INT < 23 || c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private static void fitText(Paint p, String text, float maxWidth, float minSize) {
        while (p.measureText(text) > maxWidth && p.getTextSize() > minSize) p.setTextSize(p.getTextSize() - 1f);
    }
}
