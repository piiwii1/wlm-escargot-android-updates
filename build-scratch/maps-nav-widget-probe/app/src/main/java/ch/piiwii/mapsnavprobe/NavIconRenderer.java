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
        int size = Math.max(160, sizePx);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        String text = instruction == null ? "" : instruction.toLowerCase(Locale.ROOT);
        String a = TextUtils.isEmpty(arrow) ? "↑" : arrow;

        if (contains(text, "arriv")) drawArrival(canvas, size);
        else if (contains(text, "rond-point", "rond point") || "⟳".equals(a)) drawRoundabout(canvas, size, extractExit(instruction));
        else if (contains(text, "demi-tour", "demi tour") || "↶".equals(a) || "↷".equals(a)) drawUTurn(canvas, size, "↷".equals(a));
        else if (contains(text, "restez au centre", "restez au milieu", "voie du centre")) drawStraight(canvas, size);
        else if (contains(text, "restez à gauche", "restez a gauche", "serrez à gauche", "serrez a gauche")) drawKeep(canvas, size, false);
        else if (contains(text, "restez à droite", "restez a droite", "serrez à droite", "serrez a droite")) drawKeep(canvas, size, true);
        else if (contains(text, "sortie à gauche", "sortie a gauche")) drawExit(canvas, size, false);
        else if (contains(text, "sortie à droite", "sortie a droite", "prenez la sortie")) drawExit(canvas, size, true);
        else if (contains(text, "fusionnez", "rejoignez la voie", "insérez-vous", "inserez-vous")) drawMerge(canvas, size, contains(text, "gauche") ? false : true);
        else if (contains(text, "fortement à gauche", "fortement a gauche") || "↙".equals(a)) drawSharpTurn(canvas, size, false);
        else if (contains(text, "fortement à droite", "fortement a droite") || "↘".equals(a)) drawSharpTurn(canvas, size, true);
        else if (contains(text, "légèrement à gauche", "legerement a gauche") || "↖".equals(a)) drawSlight(canvas, size, false);
        else if (contains(text, "légèrement à droite", "legerement a droite") || "↗".equals(a)) drawSlight(canvas, size, true);
        else if (contains(text, "gauche") || "←".equals(a)) drawTurn(canvas, size, false);
        else if (contains(text, "droite") || "→".equals(a)) drawTurn(canvas, size, true);
        else drawStraight(canvas, size);
        return bitmap;
    }

    private static Paint shadowStroke(int s) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeWidth(s * .155f);
        p.setColor(Color.argb(210, 0, 42, 95));
        p.setShadowLayer(s * .032f, 0, s * .016f, Color.argb(155, 0, 112, 255));
        return p;
    }

    private static Paint bodyStroke(int s) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeWidth(s * .118f);
        p.setShader(new LinearGradient(0, s * .08f, 0, s * .94f,
                new int[]{Color.rgb(109, 242, 255), Color.rgb(20, 170, 250), Color.rgb(0, 86, 225), Color.rgb(0, 54, 164)},
                new float[]{0f, .36f, .72f, 1f}, Shader.TileMode.CLAMP));
        return p;
    }

    private static Paint shineStroke(int s) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeWidth(s * .018f);
        p.setColor(Color.argb(175, 205, 251, 255));
        return p;
    }

    private static Paint dimStroke(int s) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeWidth(s * .060f);
        p.setColor(Color.argb(100, 82, 120, 157));
        return p;
    }

    private static Paint blueFill(int s) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0, s * .06f, 0, s * .94f,
                new int[]{Color.rgb(111, 242, 255), Color.rgb(21, 170, 249), Color.rgb(0, 82, 219), Color.rgb(0, 53, 161)},
                new float[]{0f, .38f, .74f, 1f}, Shader.TileMode.CLAMP));
        return p;
    }

    private static Paint darkFill() {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(0, 42, 95));
        return p;
    }

    private static void drawRoute(Canvas c, Path p, int s) {
        c.drawPath(p, shadowStroke(s));
        c.drawPath(p, bodyStroke(s));
        c.save();
        c.translate(-s * .010f, -s * .014f);
        c.drawPath(p, shineStroke(s));
        c.restore();
    }

    private static void arrowHead(Canvas c, int s, float x, float y, float angleDeg) {
        drawHead(c, s, x, y, angleDeg, 1.00f, darkFill());
        drawHead(c, s, x, y, angleDeg, .82f, blueFill(s));
        Paint shine = new Paint(Paint.ANTI_ALIAS_FLAG);
        shine.setStyle(Paint.Style.STROKE);
        shine.setStrokeWidth(s * .012f);
        shine.setColor(Color.argb(140, 220, 253, 255));
        shine.setStrokeJoin(Paint.Join.ROUND);
        Path h = headPath(s, x - s * .008f, y - s * .010f, angleDeg, .68f);
        c.drawPath(h, shine);
    }

    private static void drawHead(Canvas c, int s, float x, float y, float angleDeg, float scale, Paint paint) {
        c.drawPath(headPath(s, x, y, angleDeg, scale), paint);
    }

    private static Path headPath(int s, float x, float y, float angleDeg, float scale) {
        float len = s * .215f * scale;
        float half = s * .125f * scale;
        double a = Math.toRadians(angleDeg);
        float dx = (float) Math.cos(a), dy = (float) Math.sin(a), px = -dy, py = dx;
        float bx = x - dx * len, by = y - dy * len;
        Path h = new Path();
        h.moveTo(x, y);
        h.lineTo(bx + px * half, by + py * half);
        h.quadTo(bx - dx * s * .028f, by - dy * s * .028f, bx - px * half, by - py * half);
        h.close();
        return h;
    }

    private static void drawStraight(Canvas c, int s) {
        Path p = new Path();
        p.moveTo(s * .50f, s * .88f);
        p.lineTo(s * .50f, s * .28f);
        drawRoute(c, p, s);
        arrowHead(c, s, s * .50f, s * .105f, -90f);
    }

    private static void drawTurn(Canvas c, int s, boolean right) {
        Path p = new Path();
        p.moveTo(s * .50f, s * .89f);
        p.lineTo(s * .50f, s * .58f);
        p.cubicTo(s * .50f, s * .40f, right ? s * .59f : s * .41f, s * .32f, right ? s * .74f : s * .26f, s * .32f);
        drawRoute(c, p, s);
        arrowHead(c, s, right ? s * .91f : s * .09f, s * .32f, right ? 0f : 180f);
    }

    private static void drawSlight(Canvas c, int s, boolean right) {
        Path p = new Path();
        p.moveTo(s * .50f, s * .89f);
        p.cubicTo(s * .50f, s * .66f, right ? s * .57f : s * .43f, s * .49f, right ? s * .72f : s * .28f, s * .29f);
        drawRoute(c, p, s);
        arrowHead(c, s, right ? s * .84f : s * .16f, s * .16f, right ? -45f : -135f);
    }

    private static void drawSharpTurn(Canvas c, int s, boolean right) {
        Path p = new Path();
        p.moveTo(s * .50f, s * .89f);
        p.lineTo(s * .50f, s * .48f);
        p.cubicTo(s * .50f, s * .32f, right ? s * .60f : s * .40f, s * .26f, right ? s * .69f : s * .31f, s * .34f);
        p.lineTo(right ? s * .78f : s * .22f, s * .43f);
        drawRoute(c, p, s);
        arrowHead(c, s, right ? s * .875f : s * .125f, s * .535f, right ? 45f : 135f);
    }

    private static void drawUTurn(Canvas c, int s, boolean right) {
        float d = right ? 1f : -1f;
        Path p = new Path();
        p.moveTo(s * .47f, s * .90f);
        p.lineTo(s * .47f, s * .48f);
        p.cubicTo(s * .47f, s * .20f, s * (.47f + .29f * d), s * .18f, s * (.47f + .29f * d), s * .43f);
        p.lineTo(s * (.47f + .29f * d), s * .59f);
        drawRoute(c, p, s);
        arrowHead(c, s, s * (.47f + .29f * d), s * .755f, 90f);
    }

    private static void drawKeep(Canvas c, int s, boolean right) {
        Path straight = new Path();
        straight.moveTo(s * .49f, s * .87f);
        straight.lineTo(s * .49f, s * .19f);
        c.drawPath(straight, dimStroke(s));

        Path p = new Path();
        p.moveTo(s * .49f, s * .87f);
        p.cubicTo(s * .49f, s * .63f, right ? s * .59f : s * .39f, s * .49f, right ? s * .72f : s * .28f, s * .30f);
        drawRoute(c, p, s);
        arrowHead(c, s, right ? s * .835f : s * .165f, s * .165f, right ? -45f : -135f);
    }

    private static void drawExit(Canvas c, int s, boolean right) {
        Path main = new Path();
        main.moveTo(s * .43f, s * .89f);
        main.lineTo(s * .43f, s * .14f);
        c.drawPath(main, dimStroke(s));

        Path p = new Path();
        p.moveTo(s * .43f, s * .89f);
        p.lineTo(s * .43f, s * .57f);
        p.cubicTo(s * .43f, s * .45f, right ? s * .57f : s * .29f, s * .42f, right ? s * .71f : s * .29f, s * .31f);
        drawRoute(c, p, s);
        arrowHead(c, s, right ? s * .875f : s * .125f, s * .205f, right ? -28f : -152f);
    }

    private static void drawMerge(Canvas c, int s, boolean fromRight) {
        Path main = new Path();
        main.moveTo(s * .48f, s * .89f);
        main.lineTo(s * .48f, s * .28f);
        drawRoute(c, main, s);
        arrowHead(c, s, s * .48f, s * .10f, -90f);

        Path branch = new Path();
        float x = fromRight ? s * .83f : s * .17f;
        branch.moveTo(x, s * .88f);
        branch.cubicTo(x, s * .66f, fromRight ? s * .65f : s * .31f, s * .57f, s * .49f, s * .49f);
        c.drawPath(branch, dimStroke(s));
    }

    private static void drawRoundabout(Canvas c, int s, int exit) {
        int e = exit <= 0 ? 2 : Math.min(exit, 8);
        float cx = s * .50f, cy = s * .48f, r = s * .225f;

        Paint guide = dimStroke(s);
        guide.setStrokeWidth(s * .050f);
        c.drawCircle(cx, cy, r, guide);

        int arms = e <= 4 ? 4 : 8;
        for (int i = 0; i < arms; i++) {
            double a = Math.toRadians(90 - i * (360f / arms));
            float x1 = cx + (float) Math.cos(a) * (r + s * .012f);
            float y1 = cy + (float) Math.sin(a) * (r + s * .012f);
            float x2 = cx + (float) Math.cos(a) * (r + s * .115f);
            float y2 = cy + (float) Math.sin(a) * (r + s * .115f);
            c.drawLine(x1, y1, x2, y2, guide);
        }

        float step = e <= 4 ? 90f : 45f;
        float exitAngle = 90f - (e * step);
        RectF oval = new RectF(cx - r, cy - r, cx + r, cy + r);
        float sweep = exitAngle - 90f;

        Path p = new Path();
        p.moveTo(cx, s * .93f);
        p.lineTo(cx, cy + r);
        p.arcTo(oval, 90f, sweep, false);

        double rad = Math.toRadians(exitAngle);
        float tx = cx + (float) Math.cos(rad) * (r + s * .17f);
        float ty = cy + (float) Math.sin(rad) * (r + s * .17f);
        p.lineTo(tx, ty);
        drawRoute(c, p, s);
        arrowHead(c, s, tx, ty, exitAngle);
    }

    private static void drawArrival(Canvas c, int s) {
        Path pole = new Path();
        pole.moveTo(s * .31f, s * .84f);
        pole.lineTo(s * .31f, s * .23f);
        drawRoute(c, pole, s);

        Path shadow = new Path();
        shadow.moveTo(s * .34f, s * .20f);
        shadow.lineTo(s * .79f, s * .28f);
        shadow.lineTo(s * .61f, s * .48f);
        shadow.lineTo(s * .34f, s * .40f);
        shadow.close();
        c.drawPath(shadow, darkFill());

        Path flag = new Path();
        flag.moveTo(s * .365f, s * .235f);
        flag.lineTo(s * .735f, s * .30f);
        flag.lineTo(s * .59f, s * .435f);
        flag.lineTo(s * .365f, s * .375f);
        flag.close();
        c.drawPath(flag, blueFill(s));
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
