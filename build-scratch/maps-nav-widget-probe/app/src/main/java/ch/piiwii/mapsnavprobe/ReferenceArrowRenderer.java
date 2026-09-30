package ch.piiwii.mapsnavprobe;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import java.util.Locale;

final class ReferenceArrowRenderer {
    private ReferenceArrowRenderer() {}

    static Bitmap render(String arrow, String instruction, int sizePx) {
        String key = keyFor(arrow, instruction);
        if (key == null) return null;
        String data = pathData(key);
        if (data == null) return null;
        int s = Math.max(160, sizePx);
        Bitmap out = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        Path shape = decode(data, s);
        shape.setFillType(Path.FillType.EVEN_ODD);

        Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
        base.setStyle(Paint.Style.FILL);
        base.setShader(new LinearGradient(0, 0, s, s,
                new int[]{Color.rgb(40,218,246), Color.rgb(18,168,250), Color.rgb(8,101,250), Color.rgb(10,69,244)},
                new float[]{0f,.33f,.72f,1f}, Shader.TileMode.CLAMP));
        c.drawPath(shape, base);

        c.save();
        c.clipPath(shape);
        Path band = new Path();
        band.moveTo(-s*.12f, s*.43f);
        band.cubicTo(s*.22f,s*.47f, s*.37f,s*.28f, s*.62f,s*.31f);
        band.cubicTo(s*.80f,s*.31f, s*.96f,s*.35f, s*1.12f,s*.39f);
        band.lineTo(s*1.12f,s*1.12f);
        band.lineTo(-s*.12f,s*1.12f);
        band.close();
        Paint bandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bandPaint.setShader(new LinearGradient(0,s*.30f,0,s,
                new int[]{Color.argb(75,14,117,255),Color.argb(105,7,76,248)},
                null,Shader.TileMode.CLAMP));
        c.drawPath(band, bandPaint);

        Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
        glow.setShader(new RadialGradient(s*.20f,s*.10f,s*.62f,
                new int[]{Color.argb(85,170,250,255),Color.argb(0,170,250,255)},
                null,Shader.TileMode.CLAMP));
        c.drawRect(0,0,s,s,glow);
        c.restore();

        Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(Math.max(1f,s*.006f));
        edge.setColor(Color.argb(155,0,92,198));
        c.drawPath(shape, edge);
        return out;
    }

    private static String keyFor(String arrow, String instruction) {
        String a = arrow == null ? "" : arrow;
        String t = instruction == null ? "" : instruction.toLowerCase(Locale.ROOT);
        if (has(t,"restez à gauche","restez a gauche","serrez à gauche","serrez a gauche")) return "keep_left";
        if (has(t,"demi-tour","demi tour")) {
            if ("↷".equals(a) || has(t,"droite")) return "uturn_right";
            return "uturn_left";
        }
        if (has(t,"fortement à gauche","fortement a gauche") || "↙".equals(a)) return "sharp_left";
        if (has(t,"fortement à droite","fortement a droite") || "↘".equals(a)) return "sharp_right";
        if (has(t,"légèrement à gauche","legerement a gauche") || "↖".equals(a)) return "slight_left";
        if (has(t,"légèrement à droite","legerement a droite") || "↗".equals(a)) return "slight_right";
        if (has(t,"tournez à gauche","tourner à gauche","tournez a gauche","tourner a gauche") || "←".equals(a)) return "turn_left";
        if (has(t,"tournez à droite","tourner à droite","tournez a droite","tourner a droite") || "→".equals(a)) return "turn_right";
        if ("↑".equals(a) || has(t,"tout droit","continuez","continuer","direction")) return "straight";
        return null;
    }

    private static boolean has(String s, String... values) {
        for (String v : values) if (s.contains(v)) return true;
        return false;
    }

    private static Path decode(String data, int s) {
        Path p = new Path();
        String[] contours = data.split("\\|");
        for (String contour : contours) {
            String[] pts = contour.split(";");
            boolean first = true;
            for (String pt : pts) {
                String[] xy = pt.split(",");
                if (xy.length != 2) continue;
                float x = Float.parseFloat(xy[0]) * s / 1000f;
                float y = Float.parseFloat(xy[1]) * s / 1000f;
                if (first) { p.moveTo(x,y); first=false; } else p.lineTo(x,y);
            }
            p.close();
        }
        return p;
    }

    private static String pathData(String key) {
        switch (key) {
            case "straight": return "494,120;475,124;455,135;181,407;169,428;167,451;179,478;237,535;256,545;272,546;289,542;302,534;395,443;395,854;397,866;410,889;423,900;438,907;449,910;549,910;565,906;581,896;595,881;604,857;604,443;695,533;711,543;733,546;748,543;761,536;820,477;830,456;831,435;828,423;817,407;549,139;524,124";
            case "turn_left": return "109,352;104,360;104,374;108,383;118,392;399,610;423,621;439,620;453,614;464,603;470,590;473,575;473,459;590,459;613,463;648,478;663,489;684,514;695,534;703,559;704,848;710,865;727,883;754,890;856,889;878,881;892,866;898,852;900,836;899,565;892,516;874,459;842,403;801,356;766,329;722,306;661,291;630,289;473,289;470,154;463,141;451,131;439,126;424,125;406,132";
            case "turn_right": return "913,328;610,104;598,100;581,102;567,109;556,123;552,133;550,266;412,266;375,270;323,284;285,301;256,320;208,362;179,399;150,455;136,500;130,549;130,852;132,863;140,880;156,895;172,901;277,902;300,894;317,876;324,856;325,554;341,514;352,499;380,475;405,462;435,454;549,452;551,562;559,582;574,594;584,597;602,596;906,375;914,367;919,355;919,341";
            case "slight_left": return "234,240;226,250;221,262;226,285;248,309;400,510;408,514;424,518;434,515;447,506;474,431;499,444;523,462;547,486;563,510;577,541;587,584;588,839;592,856;600,869;619,881;634,884;738,884;754,880;765,874;778,859;782,844;782,616;779,579;768,526;754,485;737,451;715,415;687,381;662,356;623,325;539,277;569,167;569,153;562,138;545,126;524,125";
            case "slight_right": return "589,77;573,76;559,81;548,90;540,106;539,234;438,317;386,371;363,401;339,440;308,514;292,578;285,639;284,877;288,892;297,907;309,917;323,923;444,923;463,914;478,895;483,879;483,624;486,589;499,538;527,493;544,476;569,459;570,489;577,505;596,518;608,518;619,514;818,344;833,326;833,309;829,297;611,90";
            case "sharp_left": return "113,355;107,368;107,382;112,393;120,402;411,614;432,625;446,626;463,620;475,608;482,596;484,464;600,464;616,467;638,477;653,488;667,503;678,523;683,541;683,824;686,838;693,853;710,868;732,875;850,874;866,868;883,853;889,841;892,822;892,537;887,490;870,437;842,388;805,345;774,321;728,298;679,285;484,283;482,160;474,144;464,135;451,129;434,128;412,138";
            case "sharp_right": return "907,337;607,120;596,116;581,116;564,124;553,135;546,152;545,277;422,278;380,284;329,301;283,325;250,351;219,383;188,427;166,479;154,541;153,829;155,847;163,865;179,879;199,886;298,886;318,880;339,859;345,839;344,557;347,540;357,517;369,501;388,483;416,467;455,456;545,455;545,557;549,571;565,589;576,594;593,594;609,586;902,377;912,360;912,350";
            case "uturn_left": return "301,221;272,249;243,284;219,322;195,375;182,423;175,469;175,588;106,589;90,599;83,613;83,625;89,638;254,841;264,849;274,852;288,852;301,844;476,637;481,624;480,611;475,601;464,592;454,589;367,588;365,474;368,450;376,419;387,398;402,377;430,351;450,338;478,326;507,321;534,320;563,323;595,332;636,354;669,387;683,411;691,431;696,468;695,801;696,819;704,840;717,854;741,864;844,864;856,861;874,850;888,829;891,815;891,487;888,449;877,402;853,341;822,288;790,246;764,220;734,196;680,166;644,152;608,142;543,134;468,140;407,156;347,187";
            case "uturn_right": return "700,234;643,195;581,170;511,157;447,158;387,169;321,195;264,233;222,274;194,312;167,362;148,418;140,463;137,502;137,812;144,833;155,846;169,855;183,858;277,858;298,851;315,834;321,817;321,489;327,452;344,415;375,380;411,358;462,345;508,345;549,356;585,377;614,411;629,443;636,474;636,589;548,591;537,598;532,608;532,624;541,638;602,709;703,837;717,844;730,844;749,833;918,632;923,612;916,598;900,589;820,589;819,472;813,428;801,387;786,348;766,312;738,272";
            case "keep_left": return "488,460;463,469;448,482;434,503;427,526;427,863;433,880;443,889;456,896;535,896;553,892;566,880;573,857;573,534;566,507;548,481;521,464|143,255;137,265;136,278;146,293;336,436;345,440;359,440;374,432;381,423;384,413;386,356;550,455;592,486;623,526;634,552;642,584;644,862;650,879;662,890;677,896;765,896;778,892;793,882;801,863;801,635;795,576;781,526;766,489;737,444;707,411;663,380;485,273;384,215;384,132;374,117;359,109;348,108;334,113";
            default: return null;
        }
    }
}
