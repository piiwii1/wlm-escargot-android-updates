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

    private static Paint edgePaint(int s) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeWidth(s * .150f);
        p.setColor(Color.rgb(0, 54, 116));
        return p;
    }

    private static Paint routePaint(int s) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeWidth(s * .124f);
        p.setShader(new LinearGradient(0, s * .10f, 0, s * .90f,
                new int[]{Color.rgb(141, 248, 255), Color.rgb(48, 195, 255), Color.rgb(7, 125, 238), Color.rgb(0, 83, 199)},
                new float[]{0f, .32f, .70f, 1f}, Shader.TileMode.CLAMP));
        return p;
    }

    private static Paint shinePaint(int s) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeWidth(s * .010f);
        p.setColor(Color.argb(165, 240, 254, 255));
        return p;
    }

    private static Paint fillPaint(int s) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0, s * .10f, 0, s * .90f,
                new int[]{Color.rgb(145, 249, 255), Color.rgb(48, 195, 255), Color.rgb(7, 125, 238), Color.rgb(0, 83, 199)},
                new float[]{0f, .32f, .70f, 1f}, Shader.TileMode.CLAMP));
        return p;
    }

    private static Paint edgeFill() {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(0, 54, 116));
        return p;
    }

    private static Paint dimPaint(int s) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeWidth(s * .050f);
        p.setColor(Color.argb(88, 108, 137, 165));
        return p;
    }

    private static void route(Canvas c, Path path, int s) {
        c.drawPath(path, edgePaint(s));
        c.drawPath(path, routePaint(s));
        c.save();
        c.translate(-s * .008f, -s * .010f);
        c.drawPath(path, shinePaint(s));
        c.restore();
    }

    private static void head(Canvas c, int s, float x, float y, float angle) {
        Path outer = headPath(s, x, y, angle, 1f);
        c.drawPath(outer, edgeFill());
        Path inner = headPath(s, x, y, angle, .88f);
        c.drawPath(inner, fillPaint(s));
    }

    private static Path headPath(int s, float x, float y, float angle, float scale) {
        float len = s * .175f * scale;
        float half = s * .102f * scale;
        double a = Math.toRadians(angle);
        float dx = (float)Math.cos(a), dy = (float)Math.sin(a);
        float px = -dy, py = dx;
        float bx = x - dx * len, by = y - dy * len;
        Path p = new Path();
        p.moveTo(x, y);
        p.lineTo(bx + px * half, by + py * half);
        p.quadTo(bx - dx * s * .010f, by - dy * s * .010f, bx - px * half, by - py * half);
        p.close();
        return p;
    }

    private static void drawStraight(Canvas c, int s) {
        Path p = new Path();
        p.moveTo(s*.50f, s*.87f); p.lineTo(s*.50f, s*.27f);
        route(c,p,s); head(c,s,s*.50f,s*.105f,-90f);
    }

    private static void drawTurn(Canvas c, int s, boolean right) {
        Path p = new Path();
        p.moveTo(s*.50f,s*.88f); p.lineTo(s*.50f,s*.58f);
        p.cubicTo(s*.50f,s*.40f, right?s*.59f:s*.41f, s*.31f, right?s*.75f:s*.25f, s*.31f);
        route(c,p,s); head(c,s,right?s*.91f:s*.09f,s*.31f,right?0f:180f);
    }

    private static void drawSlight(Canvas c, int s, boolean right) {
        Path p = new Path();
        p.moveTo(s*.50f,s*.88f);
        p.cubicTo(s*.50f,s*.65f, right?s*.57f:s*.43f, s*.48f, right?s*.72f:s*.28f, s*.29f);
        route(c,p,s); head(c,s,right?s*.84f:s*.16f,s*.16f,right?-45f:-135f);
    }

    private static void drawSharp(Canvas c, int s, boolean right) {
        Path p = new Path();
        p.moveTo(s*.50f,s*.88f); p.lineTo(s*.50f,s*.48f);
        p.cubicTo(s*.50f,s*.32f, right?s*.60f:s*.40f, s*.27f, right?s*.69f:s*.31f, s*.34f);
        p.lineTo(right?s*.78f:s*.22f,s*.42f);
        route(c,p,s); head(c,s,right?s*.87f:s*.13f,s*.53f,right?45f:135f);
    }

    private static void drawUTurn(Canvas c, int s, boolean right) {
        float d = right ? 1f : -1f;
        Path p = new Path();
        p.moveTo(s*.47f,s*.89f); p.lineTo(s*.47f,s*.49f);
        p.cubicTo(s*.47f,s*.22f, s*(.47f+.28f*d), s*.19f, s*(.47f+.28f*d), s*.43f);
        p.lineTo(s*(.47f+.28f*d),s*.59f);
        route(c,p,s); head(c,s,s*(.47f+.28f*d),s*.75f,90f);
    }

    private static void drawKeep(Canvas c, int s, boolean right) {
        Path ghost = new Path();
        ghost.moveTo(s*.48f,s*.86f); ghost.lineTo(s*.48f,s*.19f);
        c.drawPath(ghost, dimPaint(s));
        Path p = new Path();
        p.moveTo(s*.48f,s*.86f);
        p.cubicTo(s*.48f,s*.62f,right?s*.58f:s*.38f,s*.49f,right?s*.72f:s*.28f,s*.30f);
        route(c,p,s); head(c,s,right?s*.84f:s*.16f,s*.16f,right?-45f:-135f);
    }

    private static void drawExit(Canvas c, int s, boolean right) {
        Path ghost = new Path();
        ghost.moveTo(s*.42f,s*.88f); ghost.lineTo(s*.42f,s*.14f);
        c.drawPath(ghost, dimPaint(s));
        Path p = new Path();
        p.moveTo(s*.42f,s*.88f); p.lineTo(s*.42f,s*.57f);
        p.cubicTo(s*.42f,s*.45f,right?s*.57f:s*.27f,s*.42f,right?s*.71f:s*.29f,s*.31f);
        route(c,p,s); head(c,s,right?s*.87f:s*.13f,s*.20f,right?-28f:-152f);
    }

    private static void drawMerge(Canvas c, int s, boolean fromRight) {
        Path p = new Path();
        p.moveTo(s*.48f,s*.88f); p.lineTo(s*.48f,s*.28f);
        route(c,p,s); head(c,s,s*.48f,s*.105f,-90f);
        Path ghost = new Path();
        float x = fromRight ? s*.82f : s*.18f;
        ghost.moveTo(x,s*.87f);
        ghost.cubicTo(x,s*.66f,fromRight?s*.65f:s*.31f,s*.57f,s*.49f,s*.49f);
        c.drawPath(ghost, dimPaint(s));
    }

    private static void drawRoundabout(Canvas c, int s, int exit) {
        int e = exit <= 0 ? 2 : Math.min(exit,8);
        float cx=s*.50f, cy=s*.48f, r=s*.210f;
        Paint guide=dimPaint(s); guide.setStrokeWidth(s*.044f);
        c.drawCircle(cx,cy,r,guide);

        int arms = e <= 4 ? 4 : 8;
        for (int i=0;i<arms;i++) {
            double a=Math.toRadians(90 - i*(360f/arms));
            float x1=cx+(float)Math.cos(a)*(r+s*.014f), y1=cy+(float)Math.sin(a)*(r+s*.014f);
            float x2=cx+(float)Math.cos(a)*(r+s*.090f), y2=cy+(float)Math.sin(a)*(r+s*.090f);
            c.drawLine(x1,y1,x2,y2,guide);
        }

        float step=e<=4?90f:45f;
        float exitAngle=90f-e*step;
        RectF oval=new RectF(cx-r,cy-r,cx+r,cy+r);
        Path p=new Path();
        p.moveTo(cx,s*.91f); p.lineTo(cx,cy+r);
        p.arcTo(oval,90f,exitAngle-90f,false);
        double rad=Math.toRadians(exitAngle);
        float tx=cx+(float)Math.cos(rad)*(r+s*.155f), ty=cy+(float)Math.sin(rad)*(r+s*.155f);
        p.lineTo(tx,ty);
        route(c,p,s); head(c,s,tx,ty,exitAngle);
    }

    private static void drawArrival(Canvas c, int s) {
        Paint edge = edgePaint(s); edge.setStrokeWidth(s*.078f);
        Paint body = routePaint(s); body.setStrokeWidth(s*.060f);
        c.drawLine(s*.31f,s*.82f,s*.31f,s*.24f,edge);
        c.drawLine(s*.31f,s*.82f,s*.31f,s*.24f,body);
        Path flag=new Path();
        flag.moveTo(s*.34f,s*.22f); flag.lineTo(s*.78f,s*.29f); flag.lineTo(s*.61f,s*.47f); flag.lineTo(s*.34f,s*.40f); flag.close();
        c.drawPath(flag,edgeFill());
        Path inner=new Path();
        inner.moveTo(s*.36f,s*.25f); inner.lineTo(s*.73f,s*.31f); inner.lineTo(s*.59f,s*.43f); inner.lineTo(s*.36f,s*.37f); inner.close();
        c.drawPath(inner,fillPaint(s));
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
