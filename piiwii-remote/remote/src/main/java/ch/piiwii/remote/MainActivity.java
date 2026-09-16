package ch.piiwii.remote;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final Map<String, Device> found = new LinkedHashMap<>();
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private LinearLayout deviceContainer;
    private TextView status, infoTitle, infoSubtitle;
    private String selectedIp;

    private final int BG = Color.rgb(6, 13, 22);
    private final int PANEL = Color.rgb(17, 28, 40);
    private final int PANEL2 = Color.rgb(26, 39, 53);
    private final int WHITE = Color.rgb(245, 248, 252);
    private final int MUTED = Color.rgb(143, 157, 176);
    private final int BLUE = Color.rgb(82, 169, 255);
    private final int GREEN = Color.rgb(54, 218, 139);
    private final int RED = Color.rgb(255, 105, 95);

    static class Device {
        String name, type, ip;
        Device(String n, String t, String i) { name = n; type = t; ip = i; }
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(9, 20, 33));
        getWindow().setNavigationBarColor(Color.rgb(4, 9, 15));
        setContentView(buildUi());
        discover();
    }

    private View buildUi() {
        FrameLayout frame = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.rgb(11,25,41), Color.rgb(6,14,24), Color.rgb(3,8,14)});
        frame.setBackground(bg);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        frame.addView(scroll, new FrameLayout.LayoutParams(-1,-1));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(18), dp(22), dp(26));
        scroll.addView(root, new ScrollView.LayoutParams(-1,-2));

        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titles = col();
        LinearLayout.LayoutParams titlesLp = new LinearLayout.LayoutParams(0,-2,1f);
        TextView title = tv("PiiWii Remote", 29, WHITE, true);
        titles.addView(title);
        status = tv("Recherche des appareils…", 13, MUTED, false);
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(-2,-2); stLp.topMargin=dp(3);
        titles.addView(status, stLp);
        header.addView(titles, titlesLp);

        TextView refresh = quick("↻", false);
        refresh.setOnClickListener(v -> discover());
        header.addView(refresh, lp(dp(50),dp(50),8,0));
        TextView settings = quick("⚙", false);
        settings.setOnClickListener(v -> Toast.makeText(this,"Réglages supplémentaires bientôt disponibles",Toast.LENGTH_SHORT).show());
        header.addView(settings, lp(dp(50),dp(50),8,0));
        TextView power = quick("⏻", true);
        power.setOnClickListener(v -> requestPower());
        header.addView(power, lp(dp(50),dp(50),8,0));
        root.addView(header);

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        deviceContainer = row();
        hsv.addView(deviceContainer, new HorizontalScrollView.LayoutParams(-2,dp(88)));
        LinearLayout.LayoutParams hsvLp = new LinearLayout.LayoutParams(-1,dp(88)); hsvLp.topMargin=dp(22);
        root.addView(hsv,hsvLp);

        LinearLayout muteBlock = col(); muteBlock.setGravity(Gravity.CENTER_HORIZONTAL);
        IconView mute = icon("mute", dp(96), false); mute.setOnClickListener(v -> send("MUTE"));
        muteBlock.addView(mute, new LinearLayout.LayoutParams(dp(96),dp(96)));
        TextView muteLabel = tv("Muet",14,Color.rgb(202,211,222),false);
        muteBlock.addView(muteLabel, topWrap(8));
        root.addView(muteBlock, topMatchWrap(25));

        LinearLayout center = row(); center.setGravity(Gravity.CENTER); center.setWeightSum(3f);
        center.addView(controlBlock("volDown","Volume −",92,13,v -> send("VOL_DOWN")), new LinearLayout.LayoutParams(0,-2,1f));
        center.addView(controlBlock("play","Play / Pause",116,14,v -> send("PLAY_PAUSE")), new LinearLayout.LayoutParams(0,-2,1f));
        center.addView(controlBlock("volUp","Volume +",92,13,v -> send("VOL_UP")), new LinearLayout.LayoutParams(0,-2,1f));
        root.addView(center, topMatchWrap(17));

        LinearLayout lower = row(); lower.setGravity(Gravity.CENTER);
        lower.addView(controlBlock("prev","Précédent",94,13,v -> send("PREV")), new LinearLayout.LayoutParams(0,-2,1f));
        lower.addView(controlBlock("next","Suivant",94,13,v -> send("NEXT")), new LinearLayout.LayoutParams(0,-2,1f));
        root.addView(lower, topMatchWrap(22));

        LinearLayout info = row(); info.setGravity(Gravity.CENTER_VERTICAL); info.setPadding(dp(13),dp(13),dp(13),dp(13));
        info.setBackground(roundRect(new int[]{Color.rgb(18,30,43),Color.rgb(12,22,32)},22,Color.rgb(39,57,76),1));
        TextView art = tv("♪",31,Color.rgb(110,188,255),true); art.setGravity(Gravity.CENTER);
        art.setBackground(roundRect(new int[]{Color.rgb(50,74,115),Color.rgb(12,27,45)},15,Color.TRANSPARENT,0));
        info.addView(art,new LinearLayout.LayoutParams(dp(66),dp(66)));
        LinearLayout it = col();
        infoTitle = tv("PiiWii Remote",17,WHITE,true);
        infoSubtitle = tv("Sélectionne un appareil",13,MUTED,false);
        it.addView(infoTitle); it.addView(infoSubtitle,topWrap(5));
        LinearLayout.LayoutParams itLp = new LinearLayout.LayoutParams(0,-2,1f); itLp.leftMargin=dp(15);
        info.addView(it,itLp);
        IconView eq = new IconView(this,"eq",false); info.addView(eq,new LinearLayout.LayoutParams(dp(32),dp(32)));
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(-1,dp(92)); infoLp.topMargin=dp(26);
        root.addView(info,infoLp);
        return frame;
    }

    private LinearLayout controlBlock(String kind, String label, int size, int textSize, View.OnClickListener listener) {
        LinearLayout block = col(); block.setGravity(Gravity.CENTER_HORIZONTAL);
        IconView v = icon(kind,dp(size),"play".equals(kind)); v.setOnClickListener(listener);
        block.addView(v,new LinearLayout.LayoutParams(dp(size),dp(size)));
        TextView t=tv(label,textSize,Color.rgb(202,211,222),false);
        block.addView(t,topWrap(8));
        return block;
    }

    private IconView icon(String kind,int size,boolean primary){ return new IconView(this,kind,primary); }

    private TextView quick(String glyph, boolean power) {
        TextView t = tv(glyph, power?25:24, power?RED:WHITE, false);
        t.setGravity(Gravity.CENTER);
        t.setBackground(power ? oval(new int[]{Color.rgb(84,35,39),Color.rgb(39,18,21)}, RED,2)
                              : oval(new int[]{Color.rgb(38,50,65),Color.rgb(20,29,40)},Color.rgb(53,72,92),1));
        return t;
    }

    private void requestPower() {
        Device d=selected();
        if(d==null){Toast.makeText(this,"Sélectionne d’abord un appareil",Toast.LENGTH_SHORT).show();return;}
        if("TV".equals(d.type)){
            new AlertDialog.Builder(this).setTitle("Éteindre la TV")
                    .setMessage("L’extinction complète d’une Android TV n’est pas autorisée à une application standard sur tous les modèles. Le bouton est intégré, mais ce mode TV nécessitera un accès système/ADB spécifique.")
                    .setPositiveButton("OK",null).show();
            return;
        }
        new AlertDialog.Builder(this).setTitle("Éteindre "+d.name+" ?")
                .setMessage("Le PC sélectionné va s’éteindre immédiatement.")
                .setNegativeButton("Annuler",null)
                .setPositiveButton("Éteindre",(x,w)->send("POWER")).show();
    }

    private Device selected(){return selectedIp==null?null:found.get(selectedIp);}

    private void send(String c){
        Device d=selected();
        if(d==null){Toast.makeText(this,"Aucun appareil sélectionné",Toast.LENGTH_SHORT).show();return;}
        pool.execute(()->{
            try(DatagramSocket s=new DatagramSocket()){
                byte[] x=("PIIWII_CMD|"+c).getBytes(StandardCharsets.UTF_8);
                s.send(new DatagramPacket(x,x.length,InetAddress.getByName(d.ip),45455));
                runOnUiThread(()->{status.setText("Connecté à "+d.name);infoTitle.setText(d.name);infoSubtitle.setText(label(c));});
            }catch(Exception e){runOnUiThread(()->status.setText("Erreur : "+e.getMessage()));}
        });
    }

    private String label(String c){
        switch(c){case"MUTE":return"Commande Muet envoyée";case"VOL_DOWN":return"Volume diminué";case"VOL_UP":return"Volume augmenté";case"PREV":return"Piste précédente";case"PLAY_PAUSE":return"Lecture / Pause";case"NEXT":return"Piste suivante";case"POWER":return"Extinction demandée";default:return"Commande envoyée";}
    }

    private void discover(){
        found.clear();selectedIp=null;refreshDeviceCards();status.setText("Recherche des appareils…");infoTitle.setText("PiiWii Remote");infoSubtitle.setText("Recherche sur le Wi‑Fi local");
        pool.execute(()->{
            try(DatagramSocket s=new DatagramSocket(45456)){
                s.setBroadcast(true);s.setSoTimeout(2200);
                byte[] q="PIIWII_DISCOVER".getBytes(StandardCharsets.UTF_8);
                s.send(new DatagramPacket(q,q.length,InetAddress.getByName("255.255.255.255"),45454));
                long end=System.currentTimeMillis()+2200; byte[] buf=new byte[512];
                while(System.currentTimeMillis()<end){
                    try{
                        DatagramPacket p=new DatagramPacket(buf,buf.length);s.receive(p);
                        String m=new String(p.getData(),0,p.getLength(),StandardCharsets.UTF_8);String[] a=m.split("\\|");
                        if(a.length>=3&&"PIIWII_REMOTE".equals(a[0])){String ip=p.getAddress().getHostAddress();found.put(ip,new Device(a[1],a[2],ip));if(selectedIp==null)selectedIp=ip;runOnUiThread(this::refreshDeviceCards);}
                    }catch(SocketTimeoutException ignored){break;}
                }
            }catch(Exception e){runOnUiThread(()->status.setText("Recherche impossible : "+e.getMessage()));}
            runOnUiThread(()->{if(found.isEmpty()){status.setText("Aucun appareil trouvé — vérifie le même Wi‑Fi");infoTitle.setText("Aucun appareil");infoSubtitle.setText("Lance l’agent Windows ou l’app TV");}else{Device d=selected();status.setText(found.size()+" appareil(s) disponible(s)");if(d!=null)updateSelectedInfo(d);}});
        });
    }

    private void refreshDeviceCards(){
        deviceContainer.removeAllViews();
        if(found.isEmpty()){
            TextView e=tv("Recherche en cours…",14,MUTED,false);e.setGravity(Gravity.CENTER);e.setPadding(dp(20),0,dp(20),0);deviceContainer.addView(e,new LinearLayout.LayoutParams(dp(220),dp(86)));return;
        }
        ArrayList<Device> list=new ArrayList<>(found.values());
        for(int i=0;i<list.size();i++){
            Device d=list.get(i);boolean active=d.ip.equals(selectedIp);
            LinearLayout card=row();card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(dp(15),dp(12),dp(15),dp(12));
            card.setBackground(roundRect(new int[]{active?Color.rgb(25,48,72):Color.rgb(20,32,44),Color.rgb(14,24,34)},20,active?BLUE:Color.rgb(39,57,76),active?2:1));
            IconView monitor=new IconView(this,"monitor",false);card.addView(monitor,new LinearLayout.LayoutParams(dp(38),dp(38)));
            LinearLayout tc=col();TextView n=tv(d.name,16,WHITE,true);tc.addView(n);TextView st=tv((active?"● ":"○ ")+(active?"Connecté":"Disponible"),12,active?GREEN:MUTED,false);tc.addView(st,topWrap(3));
            LinearLayout.LayoutParams tcLp=new LinearLayout.LayoutParams(dp(126),-2);tcLp.leftMargin=dp(12);card.addView(tc,tcLp);
            card.setOnClickListener(v->{selectedIp=d.ip;refreshDeviceCards();updateSelectedInfo(d);});
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(190),dp(86));if(i>0)cp.leftMargin=dp(10);deviceContainer.addView(card,cp);
        }
    }

    private void updateSelectedInfo(Device d){infoTitle.setText(d.name);infoSubtitle.setText(("TV".equals(d.type)?"Android TV":"Windows")+" • prêt à contrôler");status.setText("Connecté à "+d.name);}

    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);return l;}
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private TextView tv(String s,int sp,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private LinearLayout.LayoutParams lp(int w,int h,int left,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.leftMargin=dp(left);p.topMargin=dp(top);return p;}
    private LinearLayout.LayoutParams topWrap(int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.topMargin=dp(top);return p;}
    private LinearLayout.LayoutParams topMatchWrap(int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);return p;}
    private GradientDrawable oval(int[] colors,int stroke,int width){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,colors);g.setShape(GradientDrawable.OVAL);if(width>0)g.setStroke(dp(width),stroke);return g;}
    private GradientDrawable roundRect(int[] colors,int radius,int stroke,int width){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,colors);g.setCornerRadius(dp(radius));if(width>0)g.setStroke(dp(width),stroke);return g;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private float dp(float v){return v*getResources().getDisplayMetrics().density;}

    @Override protected void onDestroy(){super.onDestroy();pool.shutdownNow();}

    private class IconView extends View {
        private final String kind; private final boolean primary; private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); private final Path path=new Path();
        IconView(Activity c,String kind,boolean primary){super(c);this.kind=kind;this.primary=primary;setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight(),cx=w/2f,cy=h/2f;
            if(!"monitor".equals(kind)&&!"eq".equals(kind)){
                p.setStyle(Paint.Style.FILL);p.setColor(primary?Color.rgb(25,47,69):Color.rgb(24,34,46));c.drawCircle(cx,cy,Math.min(w,h)/2f-2,p);
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(primary?2:1));p.setColor(primary?BLUE:Color.rgb(55,75,96));c.drawCircle(cx,cy,Math.min(w,h)/2f-3,p);
            }
            p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeWidth(dp(2.4f));p.setColor(primary?Color.rgb(105,183,255):WHITE);p.setStyle(Paint.Style.FILL);
            float s=Math.min(w,h);
            if("monitor".equals(kind)){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setColor(BLUE);RectF r=new RectF(w*.12f,h*.18f,w*.88f,h*.68f);c.drawRoundRect(r,dp(2),dp(2),p);c.drawLine(cx,h*.68f,cx,h*.82f,p);c.drawLine(w*.35f,h*.82f,w*.65f,h*.82f,p);}
            else if("mute".equals(kind)||"volDown".equals(kind)||"volUp".equals(kind)){drawSpeaker(c,cx,cy,s*.33f);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2.7f));if("mute".equals(kind)){c.drawLine(cx+s*.02f,cy-s*.20f,cx+s*.27f,cy+s*.20f,p);}else if("volDown".equals(kind)){c.drawLine(cx+s*.10f,cy,cx+s*.29f,cy,p);}else{c.drawLine(cx+s*.10f,cy,cx+s*.29f,cy,p);c.drawLine(cx+s*.195f,cy-s*.095f,cx+s*.195f,cy+s*.095f,p);}}
            else if("play".equals(kind)){p.setColor(Color.rgb(105,183,255));path.reset();path.moveTo(cx-s*.20f,cy-s*.20f);path.lineTo(cx+s*.02f,cy);path.lineTo(cx-s*.20f,cy+s*.20f);path.close();c.drawPath(path,p);RectF a=new RectF(cx+s*.08f,cy-s*.20f,cx+s*.13f,cy+s*.20f);RectF b=new RectF(cx+s*.18f,cy-s*.20f,cx+s*.23f,cy+s*.20f);c.drawRoundRect(a,dp(2),dp(2),p);c.drawRoundRect(b,dp(2),dp(2),p);}
            else if("prev".equals(kind)||"next".equals(kind)){boolean n="next".equals(kind);p.setStrokeWidth(dp(3));p.setStyle(Paint.Style.STROKE);float barX=cx+(n?s*.18f:-s*.18f);c.drawLine(barX,cy-s*.18f,barX,cy+s*.18f,p);p.setStyle(Paint.Style.FILL);path.reset();if(n){path.moveTo(cx-s*.15f,cy-s*.19f);path.lineTo(cx+s*.13f,cy);path.lineTo(cx-s*.15f,cy+s*.19f);}else{path.moveTo(cx+s*.15f,cy-s*.19f);path.lineTo(cx-s*.13f,cy);path.lineTo(cx+s*.15f,cy+s*.19f);}path.close();c.drawPath(path,p);}
            else if("eq".equals(kind)){p.setColor(BLUE);p.setStrokeWidth(dp(3));p.setStyle(Paint.Style.STROKE);float[] hh={.22f,.38f,.55f,.34f,.24f};for(int i=0;i<5;i++){float x=w*(.20f+i*.15f);c.drawLine(x,cy-h*hh[i]/2,x,cy+h*hh[i]/2,p);}}
        }
        private void drawSpeaker(Canvas c,float cx,float cy,float z){p.setStyle(Paint.Style.FILL);path.reset();path.moveTo(cx-z*.85f,cy-z*.30f);path.lineTo(cx-z*.40f,cy-z*.30f);path.lineTo(cx+z*.15f,cy-z*.78f);path.lineTo(cx+z*.15f,cy+z*.78f);path.lineTo(cx-z*.40f,cy+z*.30f);path.lineTo(cx-z*.85f,cy+z*.30f);path.close();c.drawPath(path,p);}
    }
}
