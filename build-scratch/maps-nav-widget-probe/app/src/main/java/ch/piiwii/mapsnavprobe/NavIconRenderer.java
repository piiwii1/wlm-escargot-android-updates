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
        int s = Math.max(160, sizePx);
        Bitmap out = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        String text = instruction == null ? "" : instruction.toLowerCase(Locale.ROOT);
        String a = TextUtils.isEmpty(arrow) ? "↑" : arrow;

        if (contains(text, "arriv")) drawArrival(c, s);
        else if (contains(text, "rond-point", "rond point") || "⟳".equals(a)) drawRoundabout(c, s, extractExit(instruction));
        else if (contains(text, "demi-tour", "demi tour") || "↶".equals(a) || "↷".equals(a)) drawUTurn(c, s, "↷".equals(a));
        else if (contains(text, "restez à gauche", "restez a gauche", "serrez à gauche", "serrez a gauche")) drawKeep(c, s, false);
        else if (contains(text, "restez à droite", "restez a droite", "serrez à droite", "serrez a droite")) drawKeep(c, s, true);
        else if (contains(text, "sortie à gauche", "sortie a gauche")) drawExit(c, s, false);
        else if (contains(text, "sortie à droite", "sortie a droite", "prenez la sortie")) drawExit(c, s, true);
        else if (contains(text, "fusionnez", "rejoignez la voie", "insérez-vous", "inserez-vous")) drawMerge(c, s, !contains(text, "gauche"));
        else if (contains(text, "fortement à gauche", "fortement a gauche") || "↙".equals(a)) drawSharp(c, s, false);
        else if (contains(text, "fortement à droite", "fortement a droite") || "↘".equals(a)) drawSharp(c, s, true);
        else if (contains(text, "légèrement à gauche", "legerement a gauche") || "↖".equals(a)) drawSlight(c, s, false);
        else if (contains(text, "légèrement à droite", "legerement a droite") || "↗".equals(a)) drawSlight(c, s, true);
        else if (contains(text, "gauche") || "←".equals(a)) drawTurn(c, s, false);
        else if (contains(text, "droite") || "→".equals(a)) drawTurn(c, s, true);
        else drawStraight(c, s);
        return out;
    }

    private static Paint bodyStroke(int s) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeWidth(s * .145f);
        p.setShader(blueGradient(s));
        p.setShadowLayer(s * .020f, 0, s * .014f, Color.argb(115, 0, 42, 92));
        return p;
    }

    private static Paint subtleHighlight(int s) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeWidth(s * .018f);
        p.setColor(Color.argb(110, 220, 250, 255));
        return p;
    }

    private static Paint fillPaint(int s) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);
        p.setShader(blueGradient(s));
        p.setShadowLayer(s * .018f, 0, s * .012f, Color.argb(105, 0, 42, 92));
        return p;
    }

    private static Shader blueGradient(int s) {
        return new LinearGradient(
                0, s * .08f, 0, s * .92f,
                new int[]{
                        Color.rgb(103, 220, 255),
                        Color.rgb(42, 157, 246),
                        Color.rgb(20, 108, 224),
                        Color.rgb(15, 78, 177)
                },
                new float[]{0f, .36f, .72f, 1f},
                Shader.TileMode.CLAMP
        );
    }

    private static Paint dimPaint(int s) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeWidth(s * .052f);
        p.setColor(Color.argb(92, 118, 143, 166));
        return p;
    }

    private static void drawRoute(Canvas c, Path path, int s) {
        c.drawPath(path, bodyStroke(s));
        c.save();
        c.translate(-s * .018f, -s * .014f);
        c.drawPath(path, subtleHighlight(s));
        c.restore();
    }

    /**
     * GPS-style arrow head with a real neck. The neck overlaps the route body so the symbol reads
     * as one continuous silhouette instead of a line with a triangle pasted on top.
     */
    private static void drawHead(Canvas c, int s, float tipX, float tipY, float angleDeg) {
        c.drawPath(headPath(s, tipX, tipY, angleDeg), fillPaint(s));
    }

    private static Path headPath(int s, float x, float y, float angleDeg) {
        float len = s * .235f;
        float half = s * .155f;
        float neck = s * .071f;
        float neckBack = s * .070f;

        double a = Math.toRadians(angleDeg);
        float dx = (float)Math.cos(a), dy = (float)Math.sin(a);
        float px = -dy, py = dx;

        float bx = x - dx * len;
        float by = y - dy * len;
        float tx = bx - dx * neckBack;
        float ty = by - dy * neckBack;

        Path p = new Path();
        p.moveTo(x, y);
        p.quadTo(
                bx + px * half * .72f, by + py * half * .72f,
                bx + px * half, by + py * half
        );
        p.quadTo(
                bx + px * neck, by + py * neck,
                tx + px * neck, ty + py * neck
        );
        p.lineTo(tx - px * neck, ty - py * neck);
        p.quadTo(
                bx - px * neck, by - py * neck,
                bx - px * half, by - py * half
        );
        p.quadTo(
                bx - px * half * .72f, by - py * half * .72f,
                x, y
        );
        p.close();
        return p;
    }

    private static void drawStraight(Canvas c, int s) {
        // One-piece silhouette: rounded shaft and broad GPS arrow head.
        float cx = s * .50f;
        float left = s * .425f;
        float right = s * .575f;
        float bottom = s * .88f;
        float shoulderY = s * .34f;
        float tipY = s * .095f;
        float shoulderOut = s * .205f;

        Path p = new Path();
        p.moveTo(cx, tipY);
        p.lineTo(cx + shoulderOut, shoulderY);
        p.quadTo(cx + s * .175f, shoulderY + s * .025f, right, shoulderY + s * .020f);
        p.lineTo(right, bottom - s * .050f);
        p.quadTo(right, bottom, cx + s * .025f, bottom);
        p.quadTo(left, bottom, left, bottom - s * .050f);
        p.lineTo(left, shoulderY + s * .020f);
        p.quadTo(cx - s * .175f, shoulderY + s * .025f, cx - shoulderOut, shoulderY);
        p.close();
        c.drawPath(p, fillPaint(s));

        Paint gloss = new Paint(Paint.ANTI_ALIAS_FLAG);
        gloss.setStyle(Paint.Style.STROKE);
        gloss.setStrokeWidth(s * .014f);
        gloss.setStrokeCap(Paint.Cap.ROUND);
        gloss.setColor(Color.argb(95, 225, 251, 255));
        c.drawLine(cx - s*.025f, s*.39f, cx - s*.025f, s*.79f, gloss);
    }

    private static void drawTurn(Canvas c, int s, boolean right) {
        Path p = new Path();
        p.moveTo(s*.50f,s*.88f);
        p.lineTo(s*.50f,s*.59f);
        p.cubicTo(s*.50f,s*.40f, right?s*.59f:s*.41f, s*.31f, right?s*.71f:s*.29f, s*.31f);
        drawRoute(c,p,s);
        drawHead(c,s,right?s*.925f:s*.075f,s*.31f,right?0f:180f);
    }

    private static void drawSlight(Canvas c, int s, boolean right) {
        Path p = new Path();
        p.moveTo(s*.50f,s*.88f);
        p.cubicTo(s*.50f,s*.67f, right?s*.57f:s*.43f, s*.49f, right?s*.69f:s*.31f, s*.33f);
        drawRoute(c,p,s);
        drawHead(c,s,right?s*.845f:s*.155f,s*.15f,right?-46f:-134f);
    }

    private static void drawSharp(Canvas c, int s, boolean right) {
        Path p = new Path();
        p.moveTo(s*.50f,s*.88f);
        p.lineTo(s*.50f,s*.49f);
        p.cubicTo(s*.50f,s*.34f, right?s*.60f:s*.40f, s*.28f, right?s*.67f:s*.33f, s*.35f);
        p.lineTo(right?s*.75f:s*.25f,s*.43f);
        drawRoute(c,p,s);
        drawHead(c,s,right?s*.875f:s*.125f,s*.555f,right?46f:134f);
    }

    private static void drawUTurn(Canvas c, int s, boolean right) {
        float d = right ? 1f : -1f;
        Path p = new Path();
        p.moveTo(s*.45f,s*.89f);
        p.lineTo(s*.45f,s*.49f);
        p.cubicTo(s*.45f,s*.22f, s*(.45f+.30f*d), s*.18f, s*(.45f+.30f*d), s*.43f);
        p.lineTo(s*(.45f+.30f*d),s*.56f);
        drawRoute(c,p,s);
        drawHead(c,s,s*(.45f+.30f*d),s*.77f,90f);
    }

    private static void drawKeep(Canvas c, int s, boolean right) {
        Path ghost = new Path();
        ghost.moveTo(s*.48f,s*.86f);
        ghost.lineTo(s*.48f,s*.18f);
        c.drawPath(ghost, dimPaint(s));

        Path p = new Path();
        p.moveTo(s*.48f,s*.86f);
        p.cubicTo(s*.48f,s*.64f,right?s*.57f:s*.39f,s*.49f,right?s*.69f:s*.31f,s*.33f);
        drawRoute(c,p,s);
        drawHead(c,s,right?s*.845f:s*.155f,s*.15f,right?-46f:-134f);
    }

    private static void drawExit(Canvas c, int s, boolean right) {
        Path main = new Path();
        main.moveTo(s*.42f,s*.88f);
        main.lineTo(s*.42f,s*.14f);
        c.drawPath(main, dimPaint(s));

        Path p = new Path();
        p.moveTo(s*.42f,s*.88f);
        p.lineTo(s*.42f,s*.58f);
        p.cubicTo(s*.42f,s*.46f,right?s*.56f:s*.28f,s*.42f,right?s*.68f:s*.32f,s*.32f);
        drawRoute(c,p,s);
        drawHead(c,s,right?s*.885f:s*.115f,s*.19f,right?-30f:-150f);
    }

    private static void drawMerge(Canvas c, int s, boolean fromRight) {
        Path main = new Path();
        main.moveTo(s*.48f,s*.88f);
        main.lineTo(s*.48f,s*.30f);
        drawRoute(c,main,s);
        drawHead(c,s,s*.48f,s*.09f,-90f);

        Path branch = new Path();
        float x = fromRight ? s*.83f : s*.17f;
        branch.moveTo(x,s*.87f);
        branch.cubicTo(x,s*.67f,fromRight?s*.64f:s*.32f,s*.57f,s*.49f,s*.49f);
        c.drawPath(branch, dimPaint(s));
    }

    private static void drawRoundabout(Canvas c, int s, int exit) {
        int e = exit <= 0 ? 2 : Math.min(exit,8);
        float cx=s*.50f, cy=s*.48f, r=s*.205f;

        Paint guide = dimPaint(s);
        guide.setStrokeWidth(s*.040f);
        c.drawCircle(cx,cy,r,guide);

        int arms = e <= 4 ? 4 : 8;
        for (int i=0;i<arms;i++) {
            double a=Math.toRadians(90 - i*(360f/arms));
            float x1=cx+(float)Math.cos(a)*(r+s*.012f);
            float y1=cy+(float)Math.sin(a)*(r+s*.012f);
            float x2=cx+(float)Math.cos(a)*(r+s*.082f);
            float y2=cy+(float)Math.sin(a)*(r+s*.082f);
            c.drawLine(x1,y1,x2,y2,guide);
        }

        float step=e<=4?90f:45f;
        float exitAngle=90f-e*step;
        RectF oval=new RectF(cx-r,cy-r,cx+r,cy+r);
        Path p=new Path();
        p.moveTo(cx,s*.91f);
        p.lineTo(cx,cy+r);
        p.arcTo(oval,90f,exitAngle-90f,false);

        double rad=Math.toRadians(exitAngle);
        float tx=cx+(float)Math.cos(rad)*(r+s*.145f);
        float ty=cy+(float)Math.sin(rad)*(r+s*.145f);
        p.lineTo(tx,ty);
        drawRoute(c,p,s);
        drawHead(c,s,tx,ty,exitAngle);
    }

    private static void drawArrival(Canvas c, int s) {
        Paint pole = bodyStroke(s);
        pole.setStrokeWidth(s*.060f);
        c.drawLine(s*.31f,s*.82f,s*.31f,s*.25f,pole);

        Path flag=new Path();
        flag.moveTo(s*.34f,s*.23f);
        flag.lineTo(s*.78f,s*.30f);
        flag.lineTo(s*.61f,s*.47f);
        flag.lineTo(s*.34f,s*.40f);
        flag.close();
        c.drawPath(flag,fillPaint(s));
    }

    private static int extractExit(String text) {
        if (text == null) return 0;
        Matcher m=EXIT_NUMBER.matcher(text);
        if (m.find()) try { return Integer.parseInt(m.group(1)); } catch (Exception ignored) {}
        return 0;
    }

    private static boolean contains(String source, String... needles) {
        if (source == null) return false;
        for (String n:needles) if (source.contains(n)) return true;
        return false;
    }
}
