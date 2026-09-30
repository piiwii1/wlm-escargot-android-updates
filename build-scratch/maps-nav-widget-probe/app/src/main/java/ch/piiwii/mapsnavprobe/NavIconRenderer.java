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
        int size = Math.max(128, sizePx);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        String text = instruction == null ? "" : instruction.toLowerCase(Locale.ROOT);
        String a = TextUtils.isEmpty(arrow) ? "↑" : arrow;

        if (contains(text, "arriv")) drawArrival(canvas, size);
        else if (contains(text, "rond-point", "rond point") || "⟳".equals(a)) drawRoundabout(canvas, size, extractExit(instruction));
        else if (contains(text, "demi-tour", "demi tour") || "↶".equals(a) || "↷".equals(a)) drawUTurn(canvas, size, "↷".equals(a));
        else if (contains(text, "restez à gauche", "restez a gauche", "serrez à gauche", "serrez a gauche")) drawKeep(canvas, size, false);
        else if (contains(text, "restez à droite", "restez a droite", "serrez à droite", "serrez a droite")) drawKeep(canvas, size, true);
        else if (contains(text, "sortie à gauche", "sortie a gauche")) drawExit(canvas, size, false);
        else if (contains(text, "sortie à droite", "sortie a droite", "prenez la sortie")) drawExit(canvas, size, true);
        else if (contains(text, "fusionnez", "rejoignez la voie", "insérez-vous", "inserez-vous")) drawMerge(canvas, size, true);
        else if (contains(text, "fortement à gauche", "fortement a gauche") || "↙".equals(a)) drawSharpTurn(canvas, size, false);
        else if (contains(text, "fortement à droite", "fortement a droite") || "↘".equals(a)) drawSharpTurn(canvas, size, true);
        else if (contains(text, "légèrement à gauche", "legerement a gauche") || "↖".equals(a)) drawSlight(canvas, size, false);
        else if (contains(text, "légèrement à droite", "legerement a droite") || "↗".equals(a)) drawSlight(canvas, size, true);
        else if (contains(text, "gauche") || "←".equals(a)) drawTurn(canvas, size, false);
        else if (contains(text, "droite") || "→".equals(a)) drawTurn(canvas, size, true);
        else drawStraight(canvas, size);
        return bitmap;
    }

    private static Paint outerStroke(int s) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeWidth(s * .155f);
        p.setColor(Color.argb(235, 215, 249, 255));
        return p;
    }

    private static Paint blueStroke(int s) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeWidth(s * .122f);
        p.setShader(new LinearGradient(0, s * .08f, 0, s * .92f,
                new int[]{Color.rgb(80,235,255), Color.rgb(23,164,248), Color.rgb(0,85,224)},
                new float[]{0f,.48f,1f}, Shader.TileMode.CLAMP));
        return p;
    }

    private static Paint highlightStroke(int s) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeWidth(s * .028f);
        p.setColor(Color.argb(145, 255,255,255));
        return p;
    }

    private static Paint dimStroke(int s) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeWidth(s * .075f);
        p.setColor(Color.argb(100, 104,149,190));
        return p;
    }

    private static Paint blueFill(int s) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0,s*.10f,0,s*.90f,
                new int[]{Color.rgb(93,239,255), Color.rgb(18,157,247), Color.rgb(0,83,220)},
                new float[]{0f,.50f,1f}, Shader.TileMode.CLAMP));
        return p;
    }

    private static void drawRoute(Canvas c, Path p, int s) {
        c.drawPath(p, outerStroke(s));
        c.drawPath(p, blueStroke(s));
        Path glow = new Path(p);
        c.save();
        c.translate(-s*.010f, -s*.018f);
        c.drawPath(glow, highlightStroke(s));
        c.restore();
    }

    private static void arrowHead(Canvas c, int s, float x, float y, float angleDeg) {
        drawHead(c,s,x,y,angleDeg,1f,Color.argb(235,215,249,255),null);
        drawHead(c,s,x,y,angleDeg,.80f,0,blueFill(s));
    }

    private static void drawHead(Canvas c,int s,float x,float y,float angleDeg,float scale,int color,Paint custom) {
        float len=s*.245f*scale, half=s*.145f*scale;
        double a=Math.toRadians(angleDeg);
        float dx=(float)Math.cos(a), dy=(float)Math.sin(a), px=-dy, py=dx;
        float bx=x-dx*len, by=y-dy*len;
        Path h=new Path();
        h.moveTo(x,y);
        h.lineTo(bx+px*half,by+py*half);
        h.quadTo(bx-dx*s*.025f,by-dy*s*.025f,bx-px*half,by-py*half);
        h.close();
        Paint p=custom;
        if(p==null){p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setStyle(Paint.Style.FILL);p.setColor(color);}
        c.drawPath(h,p);
    }

    private static void drawStraight(Canvas c,int s){
        Path p=new Path(); p.moveTo(s*.50f,s*.86f); p.lineTo(s*.50f,s*.27f); drawRoute(c,p,s);
        arrowHead(c,s,s*.50f,s*.10f,-90f);
    }

    private static void drawTurn(Canvas c,int s,boolean right){
        float d=right?1f:-1f;
        Path p=new Path(); p.moveTo(s*.50f,s*.88f); p.lineTo(s*.50f,s*.56f);
        p.cubicTo(s*.50f,s*.38f,s*(.56f+.10f*d),s*.31f,right?s*.76f:s*.24f,s*.31f);
        drawRoute(c,p,s); arrowHead(c,s,right?s*.92f:s*.08f,s*.31f,right?0f:180f);
    }

    private static void drawSlight(Canvas c,int s,boolean right){
        float d=right?1f:-1f;
        Path p=new Path(); p.moveTo(s*.50f,s*.88f);
        p.cubicTo(s*.50f,s*.66f,s*(.53f+.08f*d),s*.48f,right?s*.73f:s*.27f,s*.28f);
        drawRoute(c,p,s); arrowHead(c,s,right?s*.84f:s*.16f,s*.16f,right?-45f:-135f);
    }

    private static void drawSharpTurn(Canvas c,int s,boolean right){
        Path p=new Path(); p.moveTo(s*.50f,s*.89f); p.lineTo(s*.50f,s*.47f);
        p.cubicTo(s*.50f,s*.31f,right?s*.61f:s*.39f,s*.27f,right?s*.70f:s*.30f,s*.35f);
        p.lineTo(right?s*.78f:s*.22f,s*.45f); drawRoute(c,p,s);
        arrowHead(c,s,right?s*.87f:s*.13f,s*.55f,right?45f:135f);
    }

    private static void drawUTurn(Canvas c,int s,boolean right){
        float d=right?1f:-1f;
        Path p=new Path(); p.moveTo(s*.48f,s*.90f); p.lineTo(s*.48f,s*.47f);
        p.cubicTo(s*.48f,s*.20f,s*(.48f+.28f*d),s*.18f,s*(.48f+.28f*d),s*.43f);
        p.lineTo(s*(.48f+.28f*d),s*.60f); drawRoute(c,p,s);
        arrowHead(c,s,s*(.48f+.28f*d),s*.76f,90f);
    }

    private static void drawKeep(Canvas c,int s,boolean right){
        Path straight=new Path(); straight.moveTo(s*.48f,s*.86f); straight.lineTo(s*.48f,s*.20f); c.drawPath(straight,dimStroke(s));
        Path p=new Path(); p.moveTo(s*.48f,s*.86f); p.cubicTo(s*.48f,s*.61f,right?s*.59f:s*.37f,s*.48f,right?s*.73f:s*.27f,s*.29f);
        drawRoute(c,p,s); arrowHead(c,s,right?s*.84f:s*.16f,s*.16f,right?-45f:-135f);
    }

    private static void drawExit(Canvas c,int s,boolean right){
        Path main=new Path(); main.moveTo(s*.43f,s*.88f); main.lineTo(s*.43f,s*.16f); c.drawPath(main,dimStroke(s));
        Path p=new Path(); p.moveTo(s*.43f,s*.88f); p.lineTo(s*.43f,s*.56f);
        p.cubicTo(s*.43f,s*.43f,right?s*.58f:s*.28f,s*.42f,right?s*.72f:s*.28f,s*.30f);
        drawRoute(c,p,s); arrowHead(c,s,right?s*.88f:s*.12f,s*.20f,right?-28f:-152f);
    }

    private static void drawMerge(Canvas c,int s,boolean fromRight){
        Path main=new Path();main.moveTo(s*.48f,s*.88f);main.lineTo(s*.48f,s*.26f);drawRoute(c,main,s);arrowHead(c,s,s*.48f,s*.09f,-90f);
        Path branch=new Path();float x=fromRight?s*.83f:s*.17f;branch.moveTo(x,s*.88f);branch.cubicTo(x,s*.65f,s*.65f,s*.56f,s*.50f,s*.48f);c.drawPath(branch,dimStroke(s));
    }

    private static void drawRoundabout(Canvas c,int s,int exit){
        int e=exit<=0?2:Math.min(exit,8);
        float cx=s*.50f,cy=s*.48f,r=s*.225f;
        Paint dim=dimStroke(s);dim.setStrokeWidth(s*.055f);c.drawCircle(cx,cy,r,dim);
        for(int i=0;i<4;i++){
            double a=Math.toRadians(-90+i*90);float x1=cx+(float)Math.cos(a)*(r+s*.02f),y1=cy+(float)Math.sin(a)*(r+s*.02f);float x2=cx+(float)Math.cos(a)*(r+s*.14f),y2=cy+(float)Math.sin(a)*(r+s*.14f);c.drawLine(x1,y1,x2,y2,dim);
        }
        float exitAngle=90f-(e*45f);
        RectF oval=new RectF(cx-r,cy-r,cx+r,cy+r);
        float sweep=exitAngle-90f;
        Path p=new Path();p.moveTo(cx,s*.92f);p.lineTo(cx,cy+r);p.arcTo(oval,90f,sweep,false);
        double rad=Math.toRadians(exitAngle);float tx=cx+(float)Math.cos(rad)*(r+s*.18f),ty=cy+(float)Math.sin(rad)*(r+s*.18f);p.lineTo(tx,ty);drawRoute(c,p,s);arrowHead(c,s,tx,ty,exitAngle);
    }

    private static void drawArrival(Canvas c,int s){
        Path pole=new Path();pole.moveTo(s*.31f,s*.84f);pole.lineTo(s*.31f,s*.22f);drawRoute(c,pole,s);
        Path outer=new Path();outer.moveTo(s*.34f,s*.20f);outer.lineTo(s*.79f,s*.28f);outer.lineTo(s*.61f,s*.48f);outer.lineTo(s*.34f,s*.40f);outer.close();
        Paint white=new Paint(Paint.ANTI_ALIAS_FLAG);white.setStyle(Paint.Style.FILL);white.setColor(Color.argb(235,215,249,255));c.drawPath(outer,white);
        Path inner=new Path();inner.moveTo(s*.37f,s*.24f);inner.lineTo(s*.72f,s*.30f);inner.lineTo(s*.59f,s*.43f);inner.lineTo(s*.37f,s*.37f);inner.close();c.drawPath(inner,blueFill(s));
    }

    private static int extractExit(String text){if(text==null)return 0;Matcher m=EXIT_NUMBER.matcher(text);if(m.find())try{return Integer.parseInt(m.group(1));}catch(Exception ignored){}return 0;}
    private static boolean contains(String source,String...needles){for(String n:needles)if(source.contains(n))return true;return false;}
}
