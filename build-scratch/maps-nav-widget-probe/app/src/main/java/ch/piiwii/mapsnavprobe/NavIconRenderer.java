package ch.piiwii.mapsnavprobe;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.TextUtils;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NavIconRenderer {
    private NavIconRenderer() {}

    private static final Pattern EXIT_NUMBER = Pattern.compile("(?iu)\\b(\\d{1,2})(?:re|er|e|ème|eme)?\\s+sortie\\b");

    public static Bitmap render(String arrow, String instruction, int sizePx) {
        int size = Math.max(96, sizePx);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        String text = instruction == null ? "" : instruction.toLowerCase(Locale.ROOT);
        String a = TextUtils.isEmpty(arrow) ? "↑" : arrow;

        if (contains(text, "arriv")) {
            drawArrival(canvas, size);
        } else if (contains(text, "rond-point", "rond point")) {
            drawRoundabout(canvas, size, extractExit(instruction));
        } else if (contains(text, "demi-tour", "demi tour") || "↶".equals(a) || "↷".equals(a)) {
            drawUTurn(canvas, size, "↷".equals(a));
        } else if (contains(text, "restez à gauche", "restez a gauche", "serrez à gauche", "serrez a gauche")) {
            drawKeep(canvas, size, false);
        } else if (contains(text, "restez à droite", "restez a droite", "serrez à droite", "serrez a droite")) {
            drawKeep(canvas, size, true);
        } else if (contains(text, "sortie à gauche", "sortie a gauche")) {
            drawExit(canvas, size, false);
        } else if (contains(text, "sortie à droite", "sortie a droite", "prenez la sortie")) {
            drawExit(canvas, size, true);
        } else if (contains(text, "fusionnez", "rejoignez la voie", "insérez-vous", "inserez-vous")) {
            drawMerge(canvas, size, true);
        } else if (contains(text, "fortement à gauche", "fortement a gauche") || "↙".equals(a)) {
            drawSharpTurn(canvas, size, false);
        } else if (contains(text, "fortement à droite", "fortement a droite") || "↘".equals(a)) {
            drawSharpTurn(canvas, size, true);
        } else if (contains(text, "légèrement à gauche", "legerement a gauche") || "↖".equals(a)) {
            drawSlight(canvas, size, false);
        } else if (contains(text, "légèrement à droite", "legerement a droite") || "↗".equals(a)) {
            drawSlight(canvas, size, true);
        } else if (contains(text, "gauche") || "←".equals(a)) {
            drawTurn(canvas, size, false);
        } else if (contains(text, "droite") || "→".equals(a)) {
            drawTurn(canvas, size, true);
        } else {
            drawStraight(canvas, size);
        }
        return bitmap;
    }

    private static Paint strokePaint(int size) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeWidth(size * 0.145f);
        p.setShader(new LinearGradient(0, size * 0.08f, 0, size * 0.92f,
                new int[]{Color.rgb(75, 224, 255), Color.rgb(14, 148, 244), Color.rgb(2, 74, 210)},
                new float[]{0f, 0.48f, 1f}, Shader.TileMode.CLAMP));
        p.setShadowLayer(size * 0.045f, 0, size * 0.025f, Color.argb(150, 0, 86, 255));
        return p;
    }

    private static Paint fillPaint(int size) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0, size * 0.05f, 0, size * 0.95f,
                new int[]{Color.rgb(88, 233, 255), Color.rgb(11, 140, 241), Color.rgb(0, 70, 205)},
                new float[]{0f, 0.50f, 1f}, Shader.TileMode.CLAMP));
        p.setShadowLayer(size * 0.045f, 0, size * 0.025f, Color.argb(145, 0, 80, 255));
        return p;
    }

    private static Paint dimPaint(int size) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeWidth(size * 0.095f);
        p.setColor(Color.argb(105, 80, 128, 175));
        return p;
    }

    private static void arrowHead(Canvas c, int size, float x, float y, float angleDeg) {
        float len = size * 0.245f;
        float half = size * 0.14f;
        double a = Math.toRadians(angleDeg);
        float dx = (float) Math.cos(a);
        float dy = (float) Math.sin(a);
        float px = -dy;
        float py = dx;
        float bx = x - dx * len;
        float by = y - dy * len;
        Path head = new Path();
        head.moveTo(x, y);
        head.lineTo(bx + px * half, by + py * half);
        head.lineTo(bx - px * half, by - py * half);
        head.close();
        c.drawPath(head, fillPaint(size));
    }

    private static void drawStraight(Canvas c, int s) {
        Paint p = strokePaint(s);
        Path path = new Path();
        path.moveTo(s * .50f, s * .86f);
        path.lineTo(s * .50f, s * .20f);
        c.drawPath(path, p);
        arrowHead(c, s, s * .50f, s * .10f, -90f);
    }

    private static void drawTurn(Canvas c, int s, boolean right) {
        float dir = right ? 1f : -1f;
        Paint p = strokePaint(s);
        Path path = new Path();
        path.moveTo(s * .50f, s * .88f);
        path.lineTo(s * .50f, s * .52f);
        path.cubicTo(s * .50f, s * .36f, s * (.50f + .13f * dir), s * .31f, s * (.66f + .13f * dir), s * .31f);
        float endX = right ? s * .88f : s * .12f;
        path.lineTo(endX, s * .31f);
        c.drawPath(path, p);
        arrowHead(c, s, right ? s * .94f : s * .06f, s * .31f, right ? 0f : 180f);
    }

    private static void drawSlight(Canvas c, int s, boolean right) {
        float dir = right ? 1f : -1f;
        Paint p = strokePaint(s);
        Path path = new Path();
        path.moveTo(s * .50f, s * .88f);
        path.cubicTo(s * .50f, s * .65f, s * (.52f + .10f * dir), s * .47f, s * (.67f + .12f * dir), s * .28f);
        c.drawPath(path, p);
        arrowHead(c, s, right ? s * .84f : s * .16f, s * .16f, right ? -45f : -135f);
    }

    private static void drawSharpTurn(Canvas c, int s, boolean right) {
        float dir = right ? 1f : -1f;
        Paint p = strokePaint(s);
        Path path = new Path();
        path.moveTo(s * .50f, s * .90f);
        path.lineTo(s * .50f, s * .43f);
        path.cubicTo(s * .50f, s * .28f, s * (.61f + .07f * dir), s * .23f, s * (.69f + .14f * dir), s * .34f);
        path.lineTo(right ? s * .83f : s * .17f, s * .48f);
        c.drawPath(path, p);
        arrowHead(c, s, right ? s * .88f : s * .12f, s * .54f, right ? 45f : 135f);
    }

    private static void drawUTurn(Canvas c, int s, boolean right) {
        float sign = right ? 1f : -1f;
        Paint p = strokePaint(s);
        Path path = new Path();
        path.moveTo(s * .50f, s * .90f);
        path.lineTo(s * .50f, s * .47f);
        path.cubicTo(s * .50f, s * .22f, s * (.50f + .27f * sign), s * .20f, s * (.50f + .27f * sign), s * .43f);
        path.lineTo(s * (.50f + .27f * sign), s * .63f);
        c.drawPath(path, p);
        arrowHead(c, s, s * (.50f + .27f * sign), s * .72f, 90f);
    }

    private static void drawKeep(Canvas c, int s, boolean right) {
        Paint dim = dimPaint(s);
        Path base = new Path();
        base.moveTo(s * .50f, s * .88f);
        base.lineTo(s * .50f, s * .18f);
        c.drawPath(base, dim);

        float dir = right ? 1f : -1f;
        Paint p = strokePaint(s);
        Path chosen = new Path();
        chosen.moveTo(s * .50f, s * .88f);
        chosen.cubicTo(s * .50f, s * .62f, s * (.52f + .12f * dir), s * .48f, s * (.68f + .10f * dir), s * .30f);
        c.drawPath(chosen, p);
        arrowHead(c, s, right ? s * .83f : s * .17f, s * .16f, right ? -45f : -135f);
    }

    private static void drawExit(Canvas c, int s, boolean right) {
        Paint dim = dimPaint(s);
        Path main = new Path();
        main.moveTo(s * .43f, s * .90f);
        main.lineTo(s * .43f, s * .13f);
        c.drawPath(main, dim);

        float dir = right ? 1f : -1f;
        Paint p = strokePaint(s);
        Path exit = new Path();
        exit.moveTo(s * .43f, s * .90f);
        exit.lineTo(s * .43f, s * .53f);
        exit.cubicTo(s * .43f, s * .43f, s * (.54f + .12f * dir), s * .40f, right ? s * .77f : s * .23f, s * .29f);
        c.drawPath(exit, p);
        arrowHead(c, s, right ? s * .89f : s * .11f, s * .22f, right ? -25f : -155f);
    }

    private static void drawMerge(Canvas c, int s, boolean fromRight) {
        Paint p = strokePaint(s);
        Path main = new Path();
        main.moveTo(s * .48f, s * .90f);
        main.lineTo(s * .48f, s * .18f);
        c.drawPath(main, p);
        arrowHead(c, s, s * .48f, s * .10f, -90f);

        Paint dim = dimPaint(s);
        Path branch = new Path();
        float x = fromRight ? s * .83f : s * .17f;
        branch.moveTo(x, s * .88f);
        branch.cubicTo(x, s * .62f, s * .64f, s * .55f, s * .48f, s * .48f);
        c.drawPath(branch, dim);
    }

    private static void drawRoundabout(Canvas c, int s, int exit) {
        int safeExit = exit <= 0 ? 2 : Math.min(exit, 8);
        float cx = s * .50f;
        float cy = s * .48f;
        float r = s * .245f;
        float stroke = s * .125f;

        Paint ringBg = dimPaint(s);
        ringBg.setStrokeWidth(stroke * .78f);
        c.drawCircle(cx, cy, r, ringBg);

        Paint p = strokePaint(s);
        p.setStrokeWidth(stroke);
        Path route = new Path();
        route.moveTo(cx, s * .93f);
        route.lineTo(cx, cy + r);
        RectF oval = new RectF(cx - r, cy - r, cx + r, cy + r);
        float exitAngle = 45f - 45f * safeExit;
        float sweep = exitAngle - 90f;
        route.arcTo(oval, 90f, sweep, false);

        double rad = Math.toRadians(exitAngle);
        float ex = cx + (float)Math.cos(rad) * r;
        float ey = cy + (float)Math.sin(rad) * r;
        float outR = s * .43f;
        float tx = cx + (float)Math.cos(rad) * outR;
        float ty = cy + (float)Math.sin(rad) * outR;
        route.lineTo(tx, ty);
        c.drawPath(route, p);
        arrowHead(c, s, tx, ty, exitAngle);
    }

    private static void drawArrival(Canvas c, int s) {
        Paint p = strokePaint(s);
        p.setStrokeWidth(s * .105f);
        c.drawLine(s * .31f, s * .84f, s * .31f, s * .19f, p);

        Path flag = new Path();
        flag.moveTo(s * .34f, s * .20f);
        flag.lineTo(s * .78f, s * .29f);
        flag.lineTo(s * .61f, s * .47f);
        flag.lineTo(s * .34f, s * .40f);
        flag.close();
        c.drawPath(flag, fillPaint(s));
    }

    private static int extractExit(String text) {
        if (text == null) return 0;
        Matcher m = EXIT_NUMBER.matcher(text);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (Exception ignored) {}
        }
        return 0;
    }

    private static boolean contains(String source, String... needles) {
        for (String n : needles) if (source.contains(n)) return true;
        return false;
    }
}
