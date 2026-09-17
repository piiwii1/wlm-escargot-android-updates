package ch.piiwii.remote;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private static final int BLUE=Color.rgb(70,165,255), CYAN=Color.rgb(70,216,255), GREEN=Color.rgb(48,220,145), RED=Color.rgb(255,91,82);
    private static final int WHITE=Color.rgb(245,249,253), MUTED=Color.rgb(144,161,181);
    private final Map<String,Device> devices=Collections.synchronizedMap(new LinkedHashMap<>());
    private final ExecutorService pool=Executors.newCachedThreadPool();
    private LinearLayout deviceRow;
    private TextView status, activeName, activeMeta, lastAction;
    private String selectedIp;

    static class Device { String name,type,ip; Device(String n,String t,String i){name=n;type=t;ip=i;} }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(5,15,27));
        getWindow().setNavigationBarColor(Color.rgb(2,7,13));
        setContentView(buildUi());
        discover();
    }

    private View buildUi(){
        FrameLayout frame=new FrameLayout(this);
        frame.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{Color.rgb(8,25,42),Color.rgb(4,13,23),Color.rgb(2,7,13)}));
        frame.addView(new AmbientView(this),new FrameLayout.LayoutParams(-1,-1));

        ScrollView sc=new ScrollView(this); sc.setFillViewport(true); sc.setOverScrollMode(View.OVER_SCROLL_NEVER); frame.addView(sc,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout root=col(); root.setPadding(dp(20),dp(14),dp(20),dp(26)); sc.addView(root,new ScrollView.LayoutParams(-1,-2));

        LinearLayout header=row(); header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout names=col();
        TextView title=tv("PiiWii Remote",29,WHITE,true); names.addView(title);
        TextView sub=tv("Une télécommande. Tous tes appareils.",12,MUTED,false); names.addView(sub,top(2));
        header.addView(names,new LinearLayout.LayoutParams(0,-2,1f));
        header.addView(action("refresh",false,v->discover()),gap(dp(46),dp(46),7));
        header.addView(action("settings",false,v->Toast.makeText(this,"Réglages bientôt disponibles",Toast.LENGTH_SHORT).show()),gap(dp(46),dp(46),7));
        header.addView(action("power",true,v->requestPower()),gap(dp(52),dp(52),7));
        root.addView(header);

        status=tv("●  Recherche des appareils…",12,MUTED,true); status.setGravity(Gravity.CENTER_VERTICAL); status.setPadding(dp(12),dp(7),dp(12),dp(7));
        status.setBackground(round(Color.rgb(13,29,44),Color.rgb(33,57,79),50,1)); root.addView(status,top(14));

        root.addView(section("APPAREILS"),topMatch(19));
        HorizontalScrollView hsv=new HorizontalScrollView(this); hsv.setHorizontalScrollBarEnabled(false); hsv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        deviceRow=row(); hsv.addView(deviceRow,new HorizontalScrollView.LayoutParams(-2,dp(96)));
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,dp(96)); hp.topMargin=dp(8); root.addView(hsv,hp);

        root.addView(section("COMMANDES"),topMatch(20));
        LinearLayout muteWrap=centerCol(); muteWrap.addView(control("mute",88,false,v->send("MUTE")),new LinearLayout.LayoutParams(dp(88),dp(88))); muteWrap.addView(label("Muet"),top(7)); root.addView(muteWrap,topMatch(8));

        LinearLayout mid=row(); mid.setGravity(Gravity.CENTER); mid.setWeightSum(3f);
        mid.addView(controlWrap("volDown","Volume −",84,false,v->send("VOL_DOWN")),weight());
        mid.addView(controlWrap("play","Lecture / Pause",112,true,v->send("PLAY_PAUSE")),weight());
        mid.addView(controlWrap("volUp","Volume +",84,false,v->send("VOL_UP")),weight());
        root.addView(mid,topMatch(7));

        LinearLayout nav=row(); nav.setGravity(Gravity.CENTER);
        nav.addView(controlWrap("prev","Précédent",82,false,v->send("PREV")),weight());
        nav.addView(controlWrap("next","Suivant",82,false,v->send("NEXT")),weight());
        root.addView(nav,topMatch(10));

        LinearLayout card=row(); card.setGravity(Gravity.CENTER_VERTICAL); card.setPadding(dp(14),dp(12),dp(14),dp(12)); card.setElevation(dp(4));
        card.setBackground(gradient(new int[]{Color.rgb(19,39,58),Color.rgb(9,21,33)},22,Color.rgb(38,68,94),1));
        IconView monitor=new IconView(this,"monitor",false,false); monitor.setBackground(round(Color.rgb(22,54,81),Color.rgb(54,121,174),16,1)); card.addView(monitor,new LinearLayout.LayoutParams(dp(68),dp(68)));
        LinearLayout info=col(); info.addView(tv("APPAREIL ACTIF",10,MUTED,true)); activeName=tv("PiiWii Remote",17,WHITE,true); info.addView(activeName,top(3)); activeMeta=tv("Sélectionne un appareil",12,MUTED,false); info.addView(activeMeta,top(3)); lastAction=tv("Prêt",12,CYAN,true); info.addView(lastAction,top(6));
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(0,-2,1f); ip.leftMargin=dp(13); card.addView(info,ip);
        card.addView(new IconView(this,"signal",false,false),new LinearLayout.LayoutParams(dp(34),dp(34)));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(100)); cp.topMargin=dp(18); root.addView(card,cp);

        TextView foot=tv("PiiWii Remote 1.2.0  •  Réseau local",10,Color.rgb(90,108,128),false); foot.setGravity(Gravity.CENTER); root.addView(foot,topMatch(12));
        return frame;
    }

    private LinearLayout controlWrap(String kind,String text,int size,boolean primary,View.OnClickListener l){ LinearLayout c=centerCol(); c.addView(control(kind,size,primary,l),new LinearLayout.LayoutParams(dp(size),dp(size))); c.addView(label(text),top(7)); return c; }
    private IconView control(String kind,int size,boolean primary,View.OnClickListener l){ IconView v=new IconView(this,kind,primary,false); tap(v,l); return v; }
    private IconView action(String kind,boolean danger,View.OnClickListener l){ IconView v=new IconView(this,kind,false,danger); v.setElevation(dp(danger?7:3)); tap(v,l); return v; }
    private void tap(View v,View.OnClickListener l){ v.setClickable(true); v.setOnClickListener(x->{x.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); x.animate().scaleX(.92f).scaleY(.92f).setDuration(70).withEndAction(()->x.animate().scaleX(1f).scaleY(1f).setDuration(110).start()).start(); l.onClick(x);}); }

    private void discover(){
        devices.clear(); selectedIp=null; refreshCards(); setStatus("●  Recherche des appareils…",MUTED); setInfo(null,"Recherche sur le Wi-Fi local");
        pool.execute(()->{
            try(DatagramSocket s=new DatagramSocket(45456)){
                s.setBroadcast(true); s.setSoTimeout(2200);
                byte[] q="PIIWII_DISCOVER".getBytes(StandardCharsets.UTF_8); s.send(new DatagramPacket(q,q.length,InetAddress.getByName("255.255.255.255"),45454));
                long end=System.currentTimeMillis()+2200; byte[] buf=new byte[512];
                while(System.currentTimeMillis()<end){
                    try{ DatagramPacket p=new DatagramPacket(buf,buf.length); s.receive(p); String m=new String(p.getData(),0,p.getLength(),StandardCharsets.UTF_8); String[] a=m.split("\\|");
                        if(a.length>=3 && "PIIWII_REMOTE".equals(a[0])){ String ip=p.getAddress().getHostAddress(); devices.put(ip,new Device(a[1],a[2],ip)); if(selectedIp==null) selectedIp=ip; runOnUiThread(this::refreshCards); }
                    }catch(SocketTimeoutException e){ break; }
                }
            }catch(Exception e){ runOnUiThread(()->setStatus("●  Recherche impossible",RED)); }
            runOnUiThread(()->{ if(devices.isEmpty()){setStatus("●  Aucun appareil trouvé",RED); setInfo(null,"Lance l’agent Windows ou l’app TV");} else {Device d=selected(); setStatus("●  "+devices.size()+" appareil(s) disponible(s)",GREEN); setInfo(d,"Prêt à contrôler");} });
        });
    }

    private void refreshCards(){
        deviceRow.removeAllViews();
        if(devices.isEmpty()){ TextView t=tv("Recherche en cours…",13,MUTED,false); t.setGravity(Gravity.CENTER); deviceRow.addView(t,new LinearLayout.LayoutParams(dp(215),dp(94))); return; }
        ArrayList<Device> list=new ArrayList<>(devices.values());
        for(int i=0;i<list.size();i++){ Device d=list.get(i); boolean active=d.ip.equals(selectedIp);
            LinearLayout card=row(); card.setGravity(Gravity.CENTER_VERTICAL); card.setPadding(dp(14),dp(11),dp(14),dp(11)); card.setElevation(dp(active?4:1));
            card.setBackground(gradient(new int[]{active?Color.rgb(24,55,82):Color.rgb(16,31,45),Color.rgb(9,20,31)},20,active?BLUE:Color.rgb(37,59,78),active?2:1));
            IconView icon=new IconView(this,"monitor",false,false); icon.setBackground(round(active?Color.rgb(22,67,103):Color.rgb(18,39,57),active?Color.rgb(61,150,216):Color.rgb(42,66,85),14,1)); card.addView(icon,new LinearLayout.LayoutParams(dp(50),dp(50)));
            LinearLayout txt=col(); txt.addView(tv(d.name,15,WHITE,true)); txt.addView(tv("●  "+(active?"Connecté":"Disponible"),12,active?GREEN:MUTED,true),top(3)); txt.addView(tv("TV".equals(d.type)?"Android TV":"Windows",10,MUTED,false),top(3)); LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(dp(118),-2); tp.leftMargin=dp(11); card.addView(txt,tp);
            tap(card,v->{selectedIp=d.ip; refreshCards(); setInfo(d,"Prêt à contrôler"); setStatus("●  Connecté à "+d.name,GREEN);});
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(196),dp(94)); if(i>0)p.leftMargin=dp(10); deviceRow.addView(card,p);
        }
    }

    private void send(String cmd){ Device d=selected(); if(d==null){Toast.makeText(this,"Sélectionne d’abord un appareil",Toast.LENGTH_SHORT).show();return;} lastAction.setText(label(cmd));
        pool.execute(()->{ try(DatagramSocket s=new DatagramSocket()){ byte[] b=("PIIWII_CMD|"+cmd).getBytes(StandardCharsets.UTF_8); s.send(new DatagramPacket(b,b.length,InetAddress.getByName(d.ip),45455)); runOnUiThread(()->{setStatus("●  Connecté à "+d.name,GREEN); lastAction.setText(label(cmd));}); }catch(Exception e){runOnUiThread(()->{setStatus("●  Erreur de communication",RED); lastAction.setText("Commande non envoyée");});} }); }

    private void requestPower(){ Device d=selected(); if(d==null){Toast.makeText(this,"Sélectionne d’abord un appareil",Toast.LENGTH_SHORT).show();return;} if("TV".equals(d.type)){new AlertDialog.Builder(this).setTitle("Éteindre la TV").setMessage("L’extinction complète dépend du modèle Android TV et des droits système. Les commandes média restent disponibles.").setPositiveButton("OK",null).show();return;} new AlertDialog.Builder(this).setTitle("Éteindre "+d.name+" ?").setMessage("Le PC va s’éteindre immédiatement.").setNegativeButton("Annuler",null).setPositiveButton("Éteindre",(x,w)->send("POWER")).show(); }
    private Device selected(){return selectedIp==null?null:devices.get(selectedIp);}    
    private String label(String c){switch(c){case"MUTE":return"Muet envoyé";case"VOL_DOWN":return"Volume diminué";case"VOL_UP":return"Volume augmenté";case"PREV":return"Piste précédente";case"PLAY_PAUSE":return"Lecture / Pause";case"NEXT":return"Piste suivante";case"POWER":return"Extinction demandée";default:return"Commande envoyée";}}
    private void setStatus(String s,int color){status.setText(s);status.setTextColor(color);}
    private void setInfo(Device d,String action){if(d==null){activeName.setText("PiiWii Remote");activeMeta.setText("Aucun appareil sélectionné");}else{activeName.setText(d.name);activeMeta.setText(("TV".equals(d.type)?"Android TV":"Windows")+"  •  "+d.ip);}lastAction.setText(action);}

    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);return l;} private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;} private LinearLayout centerCol(){LinearLayout l=col();l.setGravity(Gravity.CENTER_HORIZONTAL);return l;}
    private TextView tv(String s,int sp,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;} private TextView label(String s){TextView t=tv(s,12,Color.rgb(205,215,226),false);t.setGravity(Gravity.CENTER);return t;} private TextView section(String s){TextView t=tv(s,10,Color.rgb(106,137,166),true);if(android.os.Build.VERSION.SDK_INT>=21)t.setLetterSpacing(.16f);return t;}
    private LinearLayout.LayoutParams top(int v){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.topMargin=dp(v);return p;} private LinearLayout.LayoutParams topMatch(int v){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(v);return p;} private LinearLayout.LayoutParams weight(){return new LinearLayout.LayoutParams(0,-2,1f);} private LinearLayout.LayoutParams gap(int w,int h,int left){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.leftMargin=dp(left);return p;}
    private GradientDrawable round(int fill,int stroke,int radius,int sw){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));if(sw>0)g.setStroke(dp(sw),stroke);return g;} private GradientDrawable gradient(int[] colors,int radius,int stroke,int sw){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,colors);g.setCornerRadius(dp(radius));if(sw>0)g.setStroke(dp(sw),stroke);return g;} private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);} private float dpf(float v){return v*getResources().getDisplayMetrics().density;}
    @Override protected void onDestroy(){super.onDestroy();pool.shutdownNow();}

    private class AmbientView extends View { Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); Path path=new Path(); AmbientView(Activity a){super(a);setLayerType(View.LAYER_TYPE_SOFTWARE,null);} @Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();p.setShader(new RadialGradient(w*.84f,h*.12f,w*.55f,new int[]{Color.argb(55,38,134,221),Color.TRANSPARENT},null,Shader.TileMode.CLAMP));c.drawCircle(w*.84f,h*.12f,w*.55f,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dpf(1));p.setColor(Color.argb(45,65,170,255));path.moveTo(-w*.1f,h*.34f);path.cubicTo(w*.28f,h*.25f,w*.55f,h*.43f,w*1.1f,h*.3f);c.drawPath(path,p);path.reset();path.moveTo(-w*.1f,h*.66f);path.cubicTo(w*.25f,h*.55f,w*.65f,h*.74f,w*1.1f,h*.58f);c.drawPath(path,p);} }

    private class IconView extends View {
        final String kind; final boolean primary,danger; Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); Path path=new Path();
        IconView(Activity a,String k,boolean pr,boolean dg){super(a);kind=k;primary=pr;danger=dg;setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight(),cx=w/2f,cy=h/2f,r=Math.min(w,h)/2f-dpf(4);int accent=danger?RED:primary?CYAN:BLUE;int top=danger?Color.rgb(68,29,31):primary?Color.rgb(24,66,96):Color.rgb(26,44,61);p.setStyle(Paint.Style.FILL);p.setShader(new RadialGradient(cx,cy-r*.2f,r,new int[]{top,Color.rgb(8,19,30)},null,Shader.TileMode.CLAMP));if(primary||danger)p.setShadowLayer(dpf(11),0,0,Color.argb(150,Color.red(accent),Color.green(accent),Color.blue(accent)));c.drawCircle(cx,cy,r,p);p.clearShadowLayer();p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dpf(primary||danger?2:1));p.setColor(primary||danger?accent:Color.rgb(61,91,119));c.drawCircle(cx,cy,r,p);drawGlyph(c,cx,cy,Math.min(w,h)*.26f,WHITE);}
        void drawGlyph(Canvas c,float cx,float cy,float s,int color){p.setColor(color);p.setStrokeWidth(Math.max(dpf(2),s*.11f));p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStyle(Paint.Style.STROKE);
            if("power".equals(kind)){c.drawArc(new RectF(cx-s,cy-s,cx+s,cy+s),-55,290,false,p);c.drawLine(cx,cy-s*1.08f,cx,cy-s*.12f,p);return;}
            if("refresh".equals(kind)){c.drawArc(new RectF(cx-s,cy-s,cx+s,cy+s),-40,260,false,p);path.reset();path.moveTo(cx+s*.9f,cy-s*.65f);path.lineTo(cx+s*.9f,cy);path.lineTo(cx+s*.28f,cy-s*.12f);p.setStyle(Paint.Style.FILL);c.drawPath(path,p);return;}
            if("settings".equals(kind)){c.drawCircle(cx,cy,s*.82f,p);c.drawCircle(cx,cy,s*.26f,p);for(int i=0;i<8;i++){double a=i*Math.PI/4; c.drawLine(cx+(float)Math.cos(a)*s*.8f,cy+(float)Math.sin(a)*s*.8f,cx+(float)Math.cos(a)*s*1.05f,cy+(float)Math.sin(a)*s*1.05f,p);}return;}
            if("monitor".equals(kind)){c.drawRoundRect(new RectF(cx-s*1.05f,cy-s*.68f,cx+s*1.05f,cy+s*.52f),s*.13f,s*.13f,p);c.drawLine(cx,cy+s*.52f,cx,cy+s*.85f,p);c.drawLine(cx-s*.5f,cy+s*.85f,cx+s*.5f,cy+s*.85f,p);return;}
            if("signal".equals(kind)){p.setStyle(Paint.Style.FILL);for(int i=0;i<4;i++){float bw=s*.18f,bh=s*(.38f+i*.3f),left=cx-s*.8f+i*s*.43f;c.drawRoundRect(new RectF(left,cy+s*.65f-bh,left+bw,cy+s*.65f),bw/2,bw/2,p);}return;}
            if(kind.startsWith("vol")||"mute".equals(kind)){p.setStyle(Paint.Style.FILL);path.reset();path.moveTo(cx-s*1.1f,cy-s*.4f);path.lineTo(cx-s*.55f,cy-s*.4f);path.lineTo(cx+s*.08f,cy-s*.9f);path.lineTo(cx+s*.08f,cy+s*.9f);path.lineTo(cx-s*.55f,cy+s*.4f);path.lineTo(cx-s*1.1f,cy+s*.4f);path.close();c.drawPath(path,p);p.setStyle(Paint.Style.STROKE);if("mute".equals(kind)){c.drawLine(cx+s*.35f,cy-s*.65f,cx+s*1.15f,cy+s*.65f,p);c.drawLine(cx+s*1.15f,cy-s*.65f,cx+s*.35f,cy+s*.65f,p);}else{c.drawLine(cx+s*.38f,cy,cx+s*1.05f,cy,p);if("volUp".equals(kind))c.drawLine(cx+s*.72f,cy-s*.34f,cx+s*.72f,cy+s*.34f,p);}return;}
            if("play".equals(kind)){p.setStyle(Paint.Style.FILL);path.reset();path.moveTo(cx-s*.85f,cy-s*.9f);path.lineTo(cx+s*.05f,cy);path.lineTo(cx-s*.85f,cy+s*.9f);path.close();c.drawPath(path,p);float bw=s*.22f;c.drawRoundRect(new RectF(cx+s*.34f,cy-s*.82f,cx+s*.34f+bw,cy+s*.82f),bw/2,bw/2,p);c.drawRoundRect(new RectF(cx+s*.75f,cy-s*.82f,cx+s*.75f+bw,cy+s*.82f),bw/2,bw/2,p);return;}
            if("prev".equals(kind)||"next".equals(kind)){boolean n="next".equals(kind);float d=n?1:-1;p.setStyle(Paint.Style.FILL);float x=cx+d*s*.82f;c.drawRoundRect(new RectF(x-s*.08f,cy-s*.78f,x+s*.08f,cy+s*.78f),s*.08f,s*.08f,p);path.reset();path.moveTo(cx-d*s*.67f,cy-s*.82f);path.lineTo(cx+d*s*.56f,cy);path.lineTo(cx-d*s*.67f,cy+s*.82f);path.close();c.drawPath(path,p);}
        }
    }
}
