package ch.piiwii.mapsnavprobe;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;

/**
 * GTI idle compass. The visual is intentionally matched to the approved
 * flat black/red reference: red outer ring, white scale, red major marks,
 * N/E/S/O cardinals and the red/silver eight-point compass rose.
 */
public final class CompassRenderer {
    private CompassRenderer() {}

    private static final int RED = Color.rgb(244, 0, 31);
    private static final int RED_LIGHT = Color.rgb(255, 37, 49);
    private static final int RED_DARK = Color.rgb(116, 0, 15);
    private static final int BLACK = Color.rgb(8, 9, 11);
    private static final int BLACK_2 = Color.rgb(19, 21, 24);
    private static final int BLACK_3 = Color.rgb(31, 34, 38);
    private static final int WHITE = Color.rgb(238, 240, 243);
    private static final int SILVER = Color.rgb(184, 188, 193);
    private static final int SILVER_DARK = Color.rgb(91, 95, 101);
    private static final int GREY_DARK = Color.rgb(54, 58, 63);

    public static Bitmap render(float headingDegrees, int requestedSizePx) {
        int size = Math.max(240, Math.min(420, requestedSizePx));
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        float c = size * 0.5f;
        float heading = normalize(headingDegrees);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        p.setStrokeCap(Paint.Cap.BUTT);
        p.setStrokeJoin(Paint.Join.ROUND);

        float outerR = size * 0.468f;
        float frameInnerR = size * 0.420f;
        float scaleOuterR = size * 0.392f;
        float scaleInnerR = size * 0.306f;

        // Transparent outside; sober black GTI frame inside the circle.
        p.setStyle(Paint.Style.FILL);
        p.setShader(new RadialGradient(c, c, outerR,
                new int[]{Color.rgb(15, 17, 20), Color.rgb(20, 22, 25), Color.rgb(8, 9, 11)},
                new float[]{0f, 0.76f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(c, c, outerR, p);
        p.setShader(null);

        // Outer red contour from the approved reference.
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(2f, size * 0.0048f));
        p.setColor(RED);
        canvas.drawCircle(c, c, outerR, p);

        // Dark frame band and fine inner edge.
        p.setStrokeWidth(size * 0.038f);
        p.setColor(Color.rgb(24, 26, 29));
        canvas.drawCircle(c, c, outerR - size * 0.020f, p);
        p.setStrokeWidth(Math.max(1f, size * 0.003f));
        p.setColor(Color.rgb(44, 47, 51));
        canvas.drawCircle(c, c, frameInnerR, p);

        // Four fixed red frame bars + inward red triangles.
        drawFrameMarker(canvas, p, c, c, outerR, 0f, size);
        drawFrameMarker(canvas, p, c, c, outerR, 90f, size);
        drawFrameMarker(canvas, p, c, c, outerR, 180f, size);
        drawFrameMarker(canvas, p, c, c, outerR, 270f, size);

        // Rotating compass card. At heading 0 this reproduces the reference.
        canvas.save();
        canvas.rotate(-heading, c, c);

        p.setStyle(Paint.Style.FILL);
        p.setShader(new RadialGradient(c, c, scaleOuterR,
                new int[]{Color.rgb(27, 29, 32), Color.rgb(17, 19, 22), Color.rgb(9, 10, 12)},
                new float[]{0f, 0.70f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(c, c, frameInnerR - size * 0.010f, p);
        p.setShader(null);

        // White 5° scale with the same red 30° accents as the reference.
        for (int deg = 0; deg < 360; deg += 5) {
            boolean major = deg % 30 == 0;
            boolean cardinal = deg % 90 == 0;
            double a = Math.toRadians(deg - 90f);
            float outer = scaleOuterR;
            float len;
            if (cardinal) len = size * 0.047f;
            else if (major) len = size * 0.044f;
            else len = size * 0.017f;
            float inner = outer - len;

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(major || cardinal ? Math.max(2.2f, size * 0.009f)
                    : Math.max(1.2f, size * 0.0040f));
            p.setColor(major || cardinal ? RED : WHITE);
            canvas.drawLine(
                    c + (float) Math.cos(a) * inner,
                    c + (float) Math.sin(a) * inner,
                    c + (float) Math.cos(a) * outer,
                    c + (float) Math.sin(a) * outer,
                    p);
        }

        // Cardinal letters only: N red, E/S/O silver-white.
        p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        p.setTextSize(size * 0.076f);
        float labelR = size * 0.290f;
        drawCenteredText(canvas, p, "N", c, c - labelR, RED_LIGHT);
        drawCenteredText(canvas, p, "E", c + labelR, c, WHITE);
        drawCenteredText(canvas, p, "S", c, c + labelR, WHITE);
        drawCenteredText(canvas, p, "O", c - labelR, c, WHITE);

        // Inner segmented red ring.
        float innerRingR = size * 0.225f;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(2f, size * 0.0065f));
        p.setColor(RED);
        for (int q = 0; q < 4; q++) {
            float start = q * 90f - 70f;
            canvas.drawArc(c - innerRingR, c - innerRingR,
                    c + innerRingR, c + innerRingR,
                    start, 50f, false, p);
        }
        p.setStrokeWidth(Math.max(1f, size * 0.003f));
        p.setColor(Color.rgb(54, 57, 62));
        canvas.drawCircle(c, c, innerRingR + size * 0.020f, p);

        // Eight-point compass rose. North is red, E/W/S silver, diagonals graphite.
        drawNeedle(canvas, c, c, 0f, size * 0.272f, size * 0.074f,
                RED_LIGHT, RED_DARK, size);
        drawNeedle(canvas, c, c, 90f, size * 0.248f, size * 0.064f,
                Color.rgb(224, 226, 229), SILVER_DARK, size);
        drawNeedle(canvas, c, c, 180f, size * 0.260f, size * 0.066f,
                Color.rgb(194, 198, 202), SILVER_DARK, size);
        drawNeedle(canvas, c, c, 270f, size * 0.248f, size * 0.064f,
                Color.rgb(224, 226, 229), SILVER_DARK, size);

        drawNeedle(canvas, c, c, 45f, size * 0.165f, size * 0.040f,
                Color.rgb(95, 99, 104), Color.rgb(36, 39, 43), size);
        drawNeedle(canvas, c, c, 135f, size * 0.165f, size * 0.040f,
                Color.rgb(95, 99, 104), Color.rgb(36, 39, 43), size);
        drawNeedle(canvas, c, c, 225f, size * 0.165f, size * 0.040f,
                Color.rgb(95, 99, 104), Color.rgb(36, 39, 43), size);
        drawNeedle(canvas, c, c, 315f, size * 0.165f, size * 0.040f,
                Color.rgb(95, 99, 104), Color.rgb(36, 39, 43), size);

        // Central hub.
        p.setStyle(Paint.Style.FILL);
        p.setColor(BLACK_2);
        canvas.drawCircle(c, c, size * 0.061f, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(2f, size * 0.006f));
        p.setColor(RED);
        canvas.drawCircle(c, c, size * 0.061f, p);
        p.setStrokeWidth(Math.max(1f, size * 0.0025f));
        p.setColor(Color.rgb(70, 73, 77));
        canvas.drawCircle(c, c, size * 0.050f, p);

        canvas.restore();
        return out;
    }

    private static void drawFrameMarker(Canvas canvas, Paint p, float cx, float cy,
                                        float radius, float degrees, int size) {
        double a = Math.toRadians(degrees - 90f);
        float ux = (float) Math.cos(a);
        float uy = (float) Math.sin(a);
        float px = -uy;
        float py = ux;

        float r1 = radius - size * 0.020f;
        float r2 = radius + size * 0.018f;
        float half = size * 0.008f;
        Path bar = new Path();
        bar.moveTo(cx + ux * r1 + px * half, cy + uy * r1 + py * half);
        bar.lineTo(cx + ux * r2 + px * half, cy + uy * r2 + py * half);
        bar.lineTo(cx + ux * r2 - px * half, cy + uy * r2 - py * half);
        bar.lineTo(cx + ux * r1 - px * half, cy + uy * r1 - py * half);
        bar.close();
        p.setStyle(Paint.Style.FILL);
        p.setColor(RED);
        canvas.drawPath(bar, p);

        float tipR = radius - size * 0.072f;
        float baseR = radius - size * 0.030f;
        float triHalf = size * 0.022f;
        Path tri = new Path();
        tri.moveTo(cx + ux * tipR, cy + uy * tipR);
        tri.lineTo(cx + ux * baseR + px * triHalf, cy + uy * baseR + py * triHalf);
        tri.lineTo(cx + ux * baseR - px * triHalf, cy + uy * baseR - py * triHalf);
        tri.close();
        canvas.drawPath(tri, p);
    }

    private static void drawNeedle(Canvas canvas, float cx, float cy, float degrees,
                                   float length, float halfWidth, int light, int dark, int size) {
        double a = Math.toRadians(degrees - 90f);
        float ux = (float) Math.cos(a);
        float uy = (float) Math.sin(a);
        float px = -uy;
        float py = ux;

        float tipX = cx + ux * length;
        float tipY = cy + uy * length;
        float leftX = cx + px * halfWidth;
        float leftY = cy + py * halfWidth;
        float rightX = cx - px * halfWidth;
        float rightY = cy - py * halfWidth;

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);

        Path left = new Path();
        left.moveTo(cx, cy);
        left.lineTo(leftX, leftY);
        left.lineTo(tipX, tipY);
        left.close();
        p.setShader(new LinearGradient(cx, cy, tipX, tipY, light, dark, Shader.TileMode.CLAMP));
        canvas.drawPath(left, p);

        Path right = new Path();
        right.moveTo(cx, cy);
        right.lineTo(tipX, tipY);
        right.lineTo(rightX, rightY);
        right.close();
        p.setShader(new LinearGradient(cx, cy, tipX, tipY, dark, light, Shader.TileMode.CLAMP));
        canvas.drawPath(right, p);
        p.setShader(null);
    }

    private static void drawCenteredText(Canvas canvas, Paint p, String text,
                                         float x, float centerY, int color) {
        p.setColor(color);
        Paint.FontMetrics fm = p.getFontMetrics();
        float baseline = centerY - (fm.ascent + fm.descent) * 0.5f;
        canvas.drawText(text, x, baseline, p);
    }

    private static float normalize(float value) {
        float n = value % 360f;
        return n < 0f ? n + 360f : n;
    }
}
