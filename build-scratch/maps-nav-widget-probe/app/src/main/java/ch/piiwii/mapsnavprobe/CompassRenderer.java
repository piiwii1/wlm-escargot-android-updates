package ch.piiwii.mapsnavprobe;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;

/** Draws the idle GTI compass used by the launcher widget. */
public final class CompassRenderer {
    private CompassRenderer() {}

    private static final int GTI_RED = Color.rgb(226, 0, 26);
    private static final int GTI_RED_DARK = Color.rgb(117, 0, 13);
    private static final int TEXT = Color.rgb(238, 241, 244);
    private static final int MUTED = Color.rgb(126, 132, 139);

    public static Bitmap render(float headingDegrees, int requestedSizePx) {
        int size = Math.max(220, Math.min(360, requestedSizePx));
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        float c = size / 2f;
        float radius = size * 0.40f;

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.argb(235, 5, 7, 10));
        canvas.drawCircle(c, c, radius + size * 0.055f, p);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(2f, size * 0.010f));
        p.setColor(Color.rgb(43, 47, 52));
        canvas.drawCircle(c, c, radius + size * 0.028f, p);
        p.setStrokeWidth(Math.max(2f, size * 0.008f));
        p.setColor(GTI_RED_DARK);
        canvas.drawCircle(c, c, radius, p);

        canvas.save();
        canvas.rotate(-normalize(headingDegrees), c, c);

        for (int deg = 0; deg < 360; deg += 10) {
            boolean major = deg % 30 == 0;
            double a = Math.toRadians(deg - 90);
            float outer = radius - size * 0.018f;
            float inner = outer - (major ? size * 0.040f : size * 0.022f);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(major ? Math.max(2f, size * 0.009f) : Math.max(1f, size * 0.004f));
            p.setColor(deg == 0 ? GTI_RED : (major ? Color.rgb(122, 128, 134) : Color.rgb(61, 65, 70)));
            canvas.drawLine(
                    c + (float) Math.cos(a) * inner,
                    c + (float) Math.sin(a) * inner,
                    c + (float) Math.cos(a) * outer,
                    c + (float) Math.sin(a) * outer,
                    p);
        }

        p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        p.setTextSize(size * 0.090f);
        float textRadius = radius - size * 0.095f;
        drawCardinal(canvas, p, "N", c, c - textRadius, GTI_RED);
        drawCardinal(canvas, p, "E", c + textRadius, c + size * 0.030f, TEXT);
        drawCardinal(canvas, p, "S", c, c + textRadius + size * 0.032f, TEXT);
        drawCardinal(canvas, p, "O", c - textRadius, c + size * 0.030f, TEXT);

        canvas.restore();

        // Fixed marker: the top of the phone / launcher screen.
        Path marker = new Path();
        float markerTop = c - radius - size * 0.005f;
        marker.moveTo(c, markerTop);
        marker.lineTo(c - size * 0.026f, markerTop + size * 0.052f);
        marker.lineTo(c + size * 0.026f, markerTop + size * 0.052f);
        marker.close();
        p.setStyle(Paint.Style.FILL);
        p.setColor(GTI_RED);
        canvas.drawPath(marker, p);

        p.setColor(Color.rgb(18, 20, 23));
        canvas.drawCircle(c, c, size * 0.032f, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(2f, size * 0.010f));
        p.setColor(GTI_RED);
        canvas.drawCircle(c, c, size * 0.032f, p);

        p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        p.setTextSize(size * 0.064f);
        p.setColor(TEXT);
        int rounded = Math.round(normalize(headingDegrees));
        if (rounded == 360) rounded = 0;
        canvas.drawText(rounded + "°  " + direction(rounded), c, size * 0.955f, p);

        p.setTextSize(size * 0.030f);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        p.setColor(MUTED);
        canvas.drawText("BOUSSOLE", c, size * 0.875f, p);

        return bitmap;
    }

    private static void drawCardinal(Canvas canvas, Paint p, String text, float x, float y, int color) {
        p.setColor(color);
        canvas.drawText(text, x, y, p);
    }

    private static float normalize(float value) {
        float n = value % 360f;
        return n < 0f ? n + 360f : n;
    }

    private static String direction(int degrees) {
        String[] d = {"N", "NE", "E", "SE", "S", "SO", "O", "NO"};
        int index = (int) Math.floor((degrees + 22.5) / 45.0) & 7;
        return d[index];
    }
}
