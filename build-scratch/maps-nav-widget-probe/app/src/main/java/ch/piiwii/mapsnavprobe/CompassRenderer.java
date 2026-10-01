package ch.piiwii.mapsnavprobe;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

/** Draws the idle GTI compass used by the launcher widget. */
public final class CompassRenderer {
    private CompassRenderer() {}

    private static final int GTI_RED = Color.rgb(226, 0, 26);
    private static final int GTI_RED_LIGHT = Color.rgb(255, 50, 66);
    private static final int GTI_RED_DARK = Color.rgb(92, 0, 10);
    private static final int GRAPHITE = Color.rgb(10, 12, 15);
    private static final int GRAPHITE_2 = Color.rgb(21, 24, 28);
    private static final int TEXT = Color.rgb(242, 244, 247);
    private static final int MUTED = Color.rgb(128, 134, 141);
    private static final int TICK = Color.rgb(91, 97, 104);
    private static final int TICK_MINOR = Color.rgb(47, 51, 56);

    public static Bitmap render(float headingDegrees, int requestedSizePx) {
        int size = Math.max(240, Math.min(400, requestedSizePx));
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        float c = size / 2f;
        float heading = normalize(headingDegrees);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStrokeCap(Paint.Cap.ROUND);

        // Clean dark GTI dial, intentionally flat and sober.
        float outerRadius = size * 0.395f;
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.argb(245, 5, 7, 9));
        canvas.drawCircle(c, c - size * 0.025f, outerRadius + size * 0.030f, p);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(2f, size * 0.010f));
        p.setColor(Color.rgb(44, 48, 53));
        canvas.drawCircle(c, c - size * 0.025f, outerRadius + size * 0.014f, p);
        p.setStrokeWidth(Math.max(2f, size * 0.006f));
        p.setColor(GTI_RED_DARK);
        canvas.drawCircle(c, c - size * 0.025f, outerRadius, p);

        float dialCenterY = c - size * 0.025f;
        canvas.save();
        canvas.rotate(-heading, c, dialCenterY);

        // 5-degree scale. The longer 30-degree marks make orientation readable at a glance.
        for (int deg = 0; deg < 360; deg += 5) {
            boolean cardinal = deg % 90 == 0;
            boolean major = deg % 30 == 0;
            boolean medium = deg % 10 == 0;
            double a = Math.toRadians(deg - 90);
            float outer = outerRadius - size * 0.020f;
            float len = cardinal ? size * 0.052f : (major ? size * 0.043f : (medium ? size * 0.030f : size * 0.017f));
            float inner = outer - len;

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(cardinal ? Math.max(2f, size * 0.010f)
                    : (major ? Math.max(1.7f, size * 0.007f)
                    : (medium ? Math.max(1.2f, size * 0.005f) : Math.max(1f, size * 0.003f))));
            p.setColor(deg == 0 ? GTI_RED_LIGHT : (major || cardinal ? TICK : TICK_MINOR));
            canvas.drawLine(
                    c + (float) Math.cos(a) * inner,
                    dialCenterY + (float) Math.sin(a) * inner,
                    c + (float) Math.cos(a) * outer,
                    dialCenterY + (float) Math.sin(a) * outer,
                    p);
        }

        // Cardinal points rotate with the compass card, while the phone heading marker stays fixed.
        p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        p.setTextSize(size * 0.086f);
        float textRadius = outerRadius - size * 0.094f;
        drawCardinal(canvas, p, "N", c, dialCenterY - textRadius, GTI_RED_LIGHT);
        drawCardinal(canvas, p, "E", c + textRadius, dialCenterY + size * 0.029f, TEXT);
        drawCardinal(canvas, p, "S", c, dialCenterY + textRadius + size * 0.030f, TEXT);
        drawCardinal(canvas, p, "O", c - textRadius, dialCenterY + size * 0.029f, TEXT);

        // Small inter-cardinal labels improve precision without cluttering the dial.
        p.setTextSize(size * 0.034f);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        p.setColor(MUTED);
        drawAtAngle(canvas, p, "NE", c, dialCenterY, textRadius, 45);
        drawAtAngle(canvas, p, "SE", c, dialCenterY, textRadius, 135);
        drawAtAngle(canvas, p, "SO", c, dialCenterY, textRadius, 225);
        drawAtAngle(canvas, p, "NO", c, dialCenterY, textRadius, 315);

        canvas.restore();

        // Fixed top marker = direction in which the phone / head unit is pointing.
        Path marker = new Path();
        float markerTop = dialCenterY - outerRadius - size * 0.010f;
        marker.moveTo(c, markerTop);
        marker.lineTo(c - size * 0.030f, markerTop + size * 0.058f);
        marker.lineTo(c + size * 0.030f, markerTop + size * 0.058f);
        marker.close();
        p.setStyle(Paint.Style.FILL);
        p.setColor(GTI_RED_LIGHT);
        canvas.drawPath(marker, p);

        // Minimal centre hub and fixed heading line.
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1.5f, size * 0.005f));
        p.setColor(Color.rgb(54, 59, 65));
        canvas.drawCircle(c, dialCenterY, size * 0.044f, p);
        p.setStrokeWidth(Math.max(2f, size * 0.008f));
        p.setColor(GTI_RED);
        canvas.drawCircle(c, dialCenterY, size * 0.024f, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(GRAPHITE_2);
        canvas.drawCircle(c, dialCenterY, size * 0.015f, p);

        // Heading readout in a restrained lower plate.
        float plateLeft = size * 0.225f;
        float plateRight = size * 0.775f;
        float plateTop = size * 0.865f;
        float plateBottom = size * 0.982f;
        RectF plate = new RectF(plateLeft, plateTop, plateRight, plateBottom);
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(12, 14, 17));
        canvas.drawRoundRect(plate, size * 0.030f, size * 0.030f, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1f, size * 0.004f));
        p.setColor(Color.rgb(48, 52, 57));
        canvas.drawRoundRect(plate, size * 0.030f, size * 0.030f, p);

        int rounded = Math.round(heading) % 360;
        String dir = direction16(rounded);

        p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        p.setTextSize(size * 0.054f);
        p.setColor(TEXT);
        drawCenteredBaseline(canvas, p, rounded + "°  " + dir, c, size * 0.930f);

        p.setTextSize(size * 0.026f);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        p.setLetterSpacing(0.16f);
        p.setColor(GTI_RED_LIGHT);
        drawCenteredBaseline(canvas, p, "CAP", c, size * 0.885f);
        p.setLetterSpacing(0f);

        return bitmap;
    }

    private static void drawAtAngle(Canvas canvas, Paint p, String text, float cx, float cy, float radius, float degrees) {
        double a = Math.toRadians(degrees - 90f);
        float x = cx + (float) Math.cos(a) * radius;
        float y = cy + (float) Math.sin(a) * radius + p.getTextSize() * 0.34f;
        canvas.drawText(text, x, y, p);
    }

    private static void drawCardinal(Canvas canvas, Paint p, String text, float x, float y, int color) {
        p.setColor(color);
        canvas.drawText(text, x, y, p);
    }

    private static void drawCenteredBaseline(Canvas canvas, Paint p, String text, float x, float centerY) {
        Paint.FontMetrics fm = p.getFontMetrics();
        float baseline = centerY - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(text, x, baseline, p);
    }

    private static float normalize(float value) {
        float n = value % 360f;
        return n < 0f ? n + 360f : n;
    }

    private static String direction16(int degrees) {
        String[] d = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                "S", "SSO", "SO", "OSO", "O", "ONO", "NO", "NNO"};
        int index = (int) Math.floor((degrees + 11.25) / 22.5) & 15;
        return d[index];
    }
}
