package ch.piiwii.mapsnavprobe;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.Base64;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

final class AtlasVectorRenderer {
    private AtlasVectorRenderer() {}
    private static Map<String,String> DATA;

    static Bitmap render(String arrow, String instruction, int sizePx) {
        String key = keyFor(instruction);
        if (key == null) return null;
        String data = data().get(key);
        if (data == null && key.startsWith("roundabout_")) {
            int n = ordinal(instruction == null ? "" : instruction.toLowerCase(Locale.ROOT));
            if (n >= 6) data = data().get("roundabout_" + ((n - 1) % 5 + 1));
        }
        if (data == null) return null;
        int s = Math.max(160,sizePx);
        Bitmap out = Bitmap.createBitmap(s,s,Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        Path shape = decode(data,s);
        shape.setFillType(Path.FillType.EVEN_ODD);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0,0,s,s,colors(key),new float[]{0f,.34f,.72f,1f},Shader.TileMode.CLAMP));
        c.drawPath(shape,p);
        c.save(); c.clipPath(shape);
        Paint hi = new Paint(Paint.ANTI_ALIAS_FLAG);
        hi.setShader(new LinearGradient(0,0,s,s,new int[]{Color.argb(105,255,255,255),Color.argb(18,255,255,255),Color.TRANSPARENT},new float[]{0f,.36f,.74f},Shader.TileMode.CLAMP));
        c.drawRect(0,0,s,s,hi); c.restore();
        Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
        edge.setStyle(Paint.Style.STROKE); edge.setStrokeWidth(Math.max(1f,s*.0055f)); edge.setColor(edge(key));
        c.drawPath(shape,edge);
        return out;
    }

    private static int[] colors(String k) {
        if (k.contains("motorway")) return new int[]{Color.rgb(110,238,160),Color.rgb(31,190,99),Color.rgb(0,133,72),Color.rgb(0,91,51)};
        if (k.equals("road_closed") || k.equals("gps_lost")) return new int[]{Color.rgb(255,120,120),Color.rgb(238,63,63),Color.rgb(191,23,23),Color.rgb(128,8,8)};
        if (k.equals("works") || k.equals("accident") || k.equals("danger")) return new int[]{Color.rgb(255,224,116),Color.rgb(255,167,55),Color.rgb(229,91,29),Color.rgb(170,45,15)};
        if (k.equals("lane_recommended")) return new int[]{Color.rgb(100,240,151),Color.rgb(30,195,92),Color.rgb(0,133,67),Color.rgb(0,89,45)};
        if (k.equals("lane_not_recommended")) return new int[]{Color.rgb(196,201,207),Color.rgb(146,154,164),Color.rgb(95,104,114),Color.rgb(62,70,78)};
        return new int[]{Color.rgb(40,218,246),Color.rgb(18,168,250),Color.rgb(8,101,250),Color.rgb(10,69,244)};
    }
    private static int edge(String k) {
        if (k.contains("motorway")) return Color.argb(170,0,97,52);
        if (k.equals("road_closed") || k.equals("gps_lost")) return Color.argb(170,120,0,0);
        if (k.equals("works") || k.equals("accident") || k.equals("danger")) return Color.argb(170,135,54,0);
        if (k.equals("lane_recommended")) return Color.argb(160,0,95,45);
        if (k.equals("lane_not_recommended")) return Color.argb(145,70,78,86);
        return Color.argb(155,0,92,198);
    }

    private static String keyFor(String instruction) {
        String t = instruction == null ? "" : instruction.toLowerCase(Locale.ROOT);
        if (has(t,"continuer tout droit sur la voie","continuez tout droit sur la voie","dans votre voie","sur cette voie")) return "continue_lane";
        if (has(t,"rejoindre la voie de gauche","rejoignez la voie de gauche")) return "join_left";
        if (has(t,"rejoindre la voie de droite","rejoignez la voie de droite")) return "join_right";
        if (has(t,"fusion par la gauche","fusionnez par la gauche","insertion par la gauche")) return "merge_left";
        if (has(t,"fusion par la droite","fusionnez par la droite","insertion par la droite")) return "merge_right";
        if (has(t,"séparation de voie à gauche","separation de voie a gauche")) return "split_left";
        if (has(t,"séparation de voie à droite","separation de voie a droite")) return "split_right";
        if (has(t,"bifurcation à gauche","bifurcation a gauche")) return "fork_left";
        if (has(t,"bifurcation à droite","bifurcation a droite")) return "fork_right";
        if (has(t,"sortie à gauche","sortie a gauche","bretelle à gauche","bretelle a gauche")) return "exit_left";
        if (has(t,"sortie à droite","sortie a droite","bretelle à droite","bretelle a droite")) return "exit_right";
        if (has(t,"rejoindre une autoroute","rejoignez l’autoroute","rejoignez l'autoroute","entrée d’autoroute","entree d'autoroute")) return "join_motorway";
        if (has(t,"quitter l’autoroute","quitter l'autoroute","sortie d’autoroute","sortie d'autoroute")) return "motorway_exit";
        if (has(t,"échangeur","echangeur")) return "interchange";
        if (has(t,"autoroute","voie rapide")) return "motorway";
        if (has(t,"rond-point","rond point","giratoire")) {
            int n=ordinal(t); if(n>=1&&n<=8)return "roundabout_"+n;
            if(has(t,"demi-tour","demi tour")) return "roundabout_5";
            return "roundabout";
        }
        if (has(t,"sens horaire")) return "roundabout_cw";
        if (has(t,"sens antihoraire")) return "roundabout_ccw";
        if (has(t,"angle droit gauche")) return "right_angle_left";
        if (has(t,"angle droit droite")) return "right_angle_right";
        if (has(t,"carrefour en t gauche")) return "t_left";
        if (has(t,"carrefour en t droite")) return "t_right";
        if (has(t,"carrefour en croix")) return "cross";
        if (has(t,"continuer au croisement","traverser le carrefour","traverser le croisement")) return "cross_continue";
        if (has(t,"arrivée à gauche","arrivee a gauche")) return "arrival_left";
        if (has(t,"arrivée à droite","arrivee a droite","destination","point de passage","étape intermédiaire","etape intermediaire")) return "arrival_right";
        if (has(t,"recalcul")) return "recalc";
        if (has(t,"gps perdu","signal gps perdu","localisation perdue")) return "gps_lost";
        if (has(t,"position actuelle")) return "position_arrow";
        if (has(t,"gps","localisation")) return "gps_target";
        if (has(t,"route fermée","route fermee")) return "road_closed";
        if (has(t,"travaux")) return "works";
        if (has(t,"accident")) return "accident";
        if (has(t,"danger","alerte")) return "danger";
        if (has(t,"tunnel")) return "tunnel";
        if (has(t,"ferry","bac")) return "ferry";
        if (has(t,"péage","peage")) return "toll";
        if (has(t,"parking")) return "parking";
        if (has(t,"voie gauche + tout droit","voie gauche et tout droit")) return "lane_left_straight";
        if (has(t,"voie droite + tout droit","voie droite et tout droit")) return "lane_right_straight";
        if (has(t,"voie gauche + demi-tour","voie gauche et demi-tour")) return "lane_uturn";
        if (has(t,"deux voies tout droit")) return "lane_straight_pair";
        if (has(t,"voie gauche seule")) return "lane_left";
        if (has(t,"voie droite seule")) return "lane_left";
        if (has(t,"voie tout droit seule")) return "lane_straight";
        if (has(t,"voie recommandée","voie recommandee")) return "lane_recommended";
        if (has(t,"voie non recommandée","voie non recommandee","grisée","grisee")) return "lane_not_recommended";
        return null;
    }
    private static int ordinal(String t) {
        Matcher m=Pattern.compile("\\b([1-8])(?:re|er|e|ème|eme)?\\s+sortie").matcher(t);
        if(m.find()) try{return Integer.parseInt(m.group(1));}catch(Throwable ignored){}
        return 0;
    }
    private static boolean has(String s,String...v){for(String x:v)if(s.contains(x))return true;return false;}

    private static Path decode(String data,int s) {
        Path p=new Path();
        for(String contour:data.split("\\|")) {
            boolean first=true;
            for(String pt:contour.split(";")) {
                String[] xy=pt.split(","); if(xy.length!=2)continue;
                float x=Float.parseFloat(xy[0])*s/1000f, y=Float.parseFloat(xy[1])*s/1000f;
                if(first){p.moveTo(x,y);first=false;}else p.lineTo(x,y);
            }
            p.close();
        }
        return p;
    }
    private static Map<String,String> data() {
        if(DATA!=null)return DATA;
        HashMap<String,String> m=new HashMap<>();
        try {
            byte[] raw=Base64.decode(PACK,Base64.NO_WRAP);
            GZIPInputStream gz=new GZIPInputStream(new ByteArrayInputStream(raw));
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            byte[] b=new byte[4096]; int n;
            while((n=gz.read(b))>0)out.write(b,0,n);
            String all=new String(out.toByteArray(),"UTF-8");
            for(String line:all.split("\\n")){int i=line.indexOf('=');if(i>0)m.put(line.substring(0,i),line.substring(i+1));}
        } catch(Throwable ignored) {}
        DATA=m; return DATA;
    }
    private static final String PACK="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";
}
