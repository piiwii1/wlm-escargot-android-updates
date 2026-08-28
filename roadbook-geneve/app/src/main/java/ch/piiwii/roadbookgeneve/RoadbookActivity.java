package ch.piiwii.roadbookgeneve;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class RoadbookActivity extends Activity {
    private static final int BG = Color.rgb(9,14,23);
    private static final int CARD = Color.rgb(22,30,43);
    private static final int SOFT = Color.rgb(29,39,55);
    private static final int TEXT = Color.rgb(244,247,251);
    private static final int MUTED = Color.rgb(158,170,188);
    private static final int ACCENT = Color.rgb(255,181,71);
    private static final int BLUE = Color.rgb(90,166,235);
    private static final int GREEN = Color.rgb(82,211,143);
    private final Handler handler = new Handler();
    private TextView nextTitle, countdown, nextTime, alertStatus;
    private Button alertToggle;
    private LinearLayout programRows;

    private int dp(float v){ return Math.round(v*getResources().getDisplayMetrics().density); }
    private GradientDrawable bg(int c,float r){ GradientDrawable d=new GradientDrawable(); d.setColor(c); d.setCornerRadius(dp(r)); return d; }
    private TextView tv(String s,float z,int c){ TextView v=new TextView(this); v.setText(s); v.setTextSize(z); v.setTextColor(c); v.setIncludeFontPadding(false); return v; }
    private Button btn(String s,int c){ Button b=new Button(this); b.setText(s); b.setTextColor(Color.WHITE); b.setTextSize(13); b.setAllCaps(false); b.setMinHeight(0); b.setMinimumHeight(0); b.setBackground(bg(c,12)); return b; }
    private LinearLayout.LayoutParams mw(){ return new LinearLayout.LayoutParams(-1,-2); }

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        AlarmReceiver.createChannel(this);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            int top,bottom;
            if(Build.VERSION.SDK_INT>=30){ Insets i=insets.getInsets(WindowInsets.Type.systemBars()); top=i.top; bottom=i.bottom; }
            else { top=insets.getSystemWindowInsetTop(); bottom=insets.getSystemWindowInsetBottom(); }
            v.setPadding(0,top,0,bottom);
            return insets;
        });
        root.requestApplyInsets();

        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout c=new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16),dp(14),dp(16),dp(22));

        LinearLayout header=new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout names=new LinearLayout(this); names.setOrientation(LinearLayout.VERTICAL);
        TextView date=tv("SAMEDI 29 AOÛT",11.5f,ACCENT); date.setTypeface(Typeface.DEFAULT,Typeface.BOLD); names.addView(date,mw());
        TextView title=tv("Roadbook Genève",27,TEXT); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); names.addView(title,mw());
        header.addView(names,new LinearLayout.LayoutParams(0,-2,1));
        TextView ver=tv("v1.1.1",11,MUTED); ver.setPadding(dp(10),dp(6),dp(10),dp(6)); ver.setBackground(bg(CARD,20)); header.addView(ver);
        LinearLayout.LayoutParams hp=mw(); hp.bottomMargin=dp(14); c.addView(header,hp);

        LinearLayout next=new LinearLayout(this); next.setOrientation(LinearLayout.VERTICAL); next.setPadding(dp(17),dp(15),dp(17),dp(15)); next.setBackground(bg(SOFT,18));
        TextView lab=tv("PROCHAINE ÉTAPE",10.5f,GREEN); lab.setTypeface(Typeface.DEFAULT,Typeface.BOLD); next.addView(lab,mw());
        nextTitle=tv("—",19,TEXT); nextTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD); LinearLayout.LayoutParams nlp=mw(); nlp.topMargin=dp(7); next.addView(nextTitle,nlp);
        countdown=tv("—",32,ACCENT); countdown.setTypeface(Typeface.MONOSPACE,Typeface.BOLD); next.addView(countdown,mw());
        nextTime=tv("",12.5f,MUTED); next.addView(nextTime,mw());
        c.addView(next,mw());

        LinearLayout alerts=new LinearLayout(this); alerts.setOrientation(LinearLayout.HORIZONTAL); alerts.setGravity(Gravity.CENTER_VERTICAL); alerts.setPadding(dp(14),dp(12),dp(14),dp(12)); alerts.setBackground(bg(CARD,15));
        LinearLayout al=new LinearLayout(this); al.setOrientation(LinearLayout.VERTICAL); TextView alt=tv("Rappels",15.5f,TEXT); alt.setTypeface(Typeface.DEFAULT,Typeface.BOLD); al.addView(alt,mw()); alertStatus=tv("",12,MUTED); al.addView(alertStatus,mw()); alerts.addView(al,new LinearLayout.LayoutParams(0,-2,1));
        alertToggle=btn("Activer",Color.rgb(48,118,84)); alertToggle.setOnClickListener(v->toggleAlerts()); alerts.addView(alertToggle,new LinearLayout.LayoutParams(dp(86),dp(40)));
        Button test=btn("Test",Color.rgb(49,66,88)); LinearLayout.LayoutParams tlp=new LinearLayout.LayoutParams(dp(62),dp(40)); tlp.leftMargin=dp(7); test.setOnClickListener(v->testNotification()); alerts.addView(test,tlp);
        LinearLayout.LayoutParams alp=mw(); alp.topMargin=dp(10); c.addView(alerts,alp);

        TextView prog=tv("Programme",20,TEXT); prog.setTypeface(Typeface.DEFAULT,Typeface.BOLD); LinearLayout.LayoutParams plp=mw(); plp.topMargin=dp(20); plp.bottomMargin=dp(8); c.addView(prog,plp);
        LinearLayout progCard=new LinearLayout(this); progCard.setOrientation(LinearLayout.VERTICAL); progCard.setPadding(dp(14),dp(4),dp(14),dp(4)); progCard.setBackground(bg(CARD,17)); programRows=new LinearLayout(this); programRows.setOrientation(LinearLayout.VERTICAL); progCard.addView(programRows,mw()); c.addView(progCard,mw()); rebuildProgram();

        addChoices(c);
        TextView foot=tv("Touchez une heure pour la modifier. Les rappels se recalculent automatiquement.",11.5f,MUTED); foot.setGravity(Gravity.CENTER); LinearLayout.LayoutParams flp=mw(); flp.topMargin=dp(16); c.addView(foot,flp);

        scroll.addView(c,new ScrollView.LayoutParams(-1,-2)); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);
        updateAlertStatus(); handler.post(tick);
    }

    private void rebuildProgram(){
        programRows.removeAllViews();
        for(int i=0;i<Itinerary.STOPS.length;i++){
            Itinerary.Stop s=Itinerary.STOPS[i];
            LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.TOP); row.setPadding(0,dp(11),0,dp(11));
            TextView time=tv(Itinerary.getTime(this,s),16.5f,s.fixedEvent?ACCENT:BLUE); time.setTypeface(Typeface.MONOSPACE,Typeface.BOLD); time.setGravity(Gravity.CENTER); time.setPadding(dp(5),dp(7),dp(5),dp(7)); time.setBackground(bg(Color.rgb(32,43,60),10)); time.setOnClickListener(v->editTime(s)); row.addView(time,new LinearLayout.LayoutParams(dp(66),-2));
            LinearLayout details=new LinearLayout(this); details.setOrientation(LinearLayout.VERTICAL); LinearLayout.LayoutParams dlp=new LinearLayout.LayoutParams(0,-2,1); dlp.leftMargin=dp(10);
            TextView t=tv(s.title,15.5f,TEXT); t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); details.addView(t,mw());
            TextView note=tv(s.note,12.5f,MUTED); LinearLayout.LayoutParams nlp=mw(); nlp.topMargin=dp(4); details.addView(note,nlp);
            if(s.destination!=null){ Button gps=btn("GPS → "+s.gpsLabel,Color.rgb(36,91,136)); LinearLayout.LayoutParams glp=new LinearLayout.LayoutParams(-2,dp(38)); glp.topMargin=dp(8); gps.setOnClickListener(v->navigate(s.destination)); details.addView(gps,glp); }
            row.addView(details,dlp); programRows.addView(row,mw());
            if(i<Itinerary.STOPS.length-1){ View div=new View(this); div.setBackgroundColor(Color.rgb(43,53,69)); LinearLayout.LayoutParams x=new LinearLayout.LayoutParams(-1,dp(1)); x.leftMargin=dp(72); programRows.addView(div,x); }
        }
    }

    private void addChoices(LinearLayout c){
        TextView sec=tv("Après 22h20",20,TEXT); sec.setTypeface(Typeface.DEFAULT,Typeface.BOLD); LinearLayout.LayoutParams slp=mw(); slp.topMargin=dp(20); slp.bottomMargin=dp(8); c.addView(sec,slp);
        LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(14),dp(14),dp(14),dp(14)); card.setBackground(bg(Color.rgb(32,27,47),17));
        TextView d=tv("Tu décides sur le moment selon la motivation.",12.5f,MUTED); card.addView(d,mw());
        Button classic=btn("22h30 · Rester pour le Classique",Color.rgb(112,83,173)); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(46)); lp.topMargin=dp(10); classic.setOnClickListener(v->{ Itinerary.prefs(this).edit().putBoolean("stay_classic",true).apply(); updateCountdown(); Toast.makeText(this,"Classique 22h30 retenu",Toast.LENGTH_SHORT).show(); }); card.addView(classic,lp);
        Button aromat=btn("Nuit de l'Aromat · Romont",Color.rgb(176,143,44)); lp=new LinearLayout.LayoutParams(-1,dp(46)); lp.topMargin=dp(8); aromat.setOnClickListener(v->{ Itinerary.prefs(this).edit().putBoolean("stay_classic",false).apply(); navigate(Itinerary.AROMAT_DESTINATION); }); card.addView(aromat,lp);
        TextView where=tv("Maison St-Charles · Rue du Château 126 · dès 16h",11.5f,MUTED); where.setGravity(Gravity.CENTER); lp=mw(); lp.topMargin=dp(5); card.addView(where,lp);
        Button home=btn("Retour Sion",Color.rgb(47,101,76)); lp=new LinearLayout.LayoutParams(-1,dp(46)); lp.topMargin=dp(9); home.setOnClickListener(v->{ Itinerary.prefs(this).edit().putBoolean("stay_classic",false).apply(); navigate(Itinerary.HOME_DESTINATION); }); card.addView(home,lp);
        Button other=btn("Autre destination…",Color.rgb(54,57,75)); lp=new LinearLayout.LayoutParams(-1,dp(42)); lp.topMargin=dp(8); other.setOnClickListener(v->otherDestination()); card.addView(other,lp);
        c.addView(card,mw());
    }

    private void editTime(Itinerary.Stop s){ String[] p=Itinerary.getTime(this,s).split(":"); new TimePickerDialog(this,(v,h,m)->{ Itinerary.setTime(this,s,h,m); if(AlarmScheduler.isEnabled(this)) AlarmScheduler.scheduleAll(this); rebuildProgram(); updateCountdown(); },Integer.parseInt(p[0]),Integer.parseInt(p[1]),true).show(); }
    private void otherDestination(){ EditText input=new EditText(this); input.setSingleLine(true); input.setHint("Lieu ou adresse"); input.setText(Itinerary.prefs(this).getString("other_destination","")); new AlertDialog.Builder(this).setTitle("Autre destination").setView(input).setNegativeButton("Annuler",null).setPositiveButton("GPS",(d,w)->{ String x=input.getText().toString().trim(); if(!x.isEmpty()){ Itinerary.prefs(this).edit().putString("other_destination",x).putBoolean("stay_classic",false).apply(); navigate(x); }}).show(); }

    private void toggleAlerts(){ if(AlarmScheduler.isEnabled(this)){ AlarmScheduler.setEnabled(this,false); Toast.makeText(this,"Rappels désactivés",Toast.LENGTH_SHORT).show(); } else enableAlerts(); updateAlertStatus(); }
    private void enableAlerts(){ AlarmScheduler.setEnabled(this,true); if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},501); if(Build.VERSION.SDK_INT>=31&&!AlarmScheduler.canExact(this)){ try{ startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName()))); }catch(Exception ignored){} } else AlarmScheduler.scheduleAll(this); }
    private void testNotification(){ if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){ requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},502); return; } sendBroadcast(new Intent(this,AlarmReceiver.class).putExtra("title","Test Roadbook Genève").putExtra("text","Parfait : les notifications fonctionnent.").putExtra("notification_id",9090)); }
    private void updateAlertStatus(){ if(alertStatus==null)return; if(AlarmScheduler.isEnabled(this)){ alertStatus.setText("Actifs"); alertStatus.setTextColor(GREEN); alertToggle.setText("Couper"); alertToggle.setBackground(bg(Color.rgb(128,65,65),12)); } else { alertStatus.setText("Inactifs"); alertStatus.setTextColor(MUTED); alertToggle.setText("Activer"); alertToggle.setBackground(bg(Color.rgb(48,118,84),12)); }}
    private void navigate(String destination){ try{ Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse("google.navigation:q="+Uri.encode(destination))); i.setPackage("com.google.android.apps.maps"); startActivity(i); }catch(ActivityNotFoundException e){ startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/maps/dir/?api=1&destination="+Uri.encode(destination)))); }}

    private final Runnable tick=new Runnable(){ @Override public void run(){ updateCountdown(); handler.postDelayed(this,1000); }};
    private void updateCountdown(){
        long now=System.currentTimeMillis(), best=Long.MAX_VALUE; Itinerary.Stop next=null;
        for(Itinerary.Stop s:Itinerary.STOPS){ long when=Itinerary.getTimeMillis(this,s); if(when>=now&&when<best){ best=when; next=s; }}
        boolean stay=Itinerary.prefs(this).getBoolean("stay_classic",false); long classic=Itinerary.classicMillis(); boolean cNext=stay&&classic>=now&&classic<best;
        if(next==null&&!cNext){ nextTitle.setText("Fin de soirée libre"); countdown.setText("À toi de voir"); countdown.setTextSize(23); nextTime.setText("Classique, Romont, Sion ou autre sortie."); return; }
        if(cNext){ best=classic; nextTitle.setText("Rêve d'Eau — Classique"); nextTime.setText("Prévu à 22:30 · Jardin Anglais"); } else { nextTitle.setText(next.title); nextTime.setText("Prévu à "+Itinerary.getTime(this,next)+" · heure Suisse"); }
        countdown.setTextSize(32); long sec=Math.max(0,best-now)/1000, days=sec/86400, hh=(sec%86400)/3600, mm=(sec%3600)/60, ss=sec%60; countdown.setText(days>0?String.format(Locale.ROOT,"%dj %02d:%02d:%02d",days,hh,mm,ss):String.format(Locale.ROOT,"%02d:%02d:%02d",hh,mm,ss));
    }

    @Override protected void onResume(){ super.onResume(); if(AlarmScheduler.isEnabled(this))AlarmScheduler.scheduleAll(this); if(nextTitle!=null){ updateAlertStatus(); updateCountdown(); }}
    @Override public void onRequestPermissionsResult(int r,String[]p,int[]g){ super.onRequestPermissionsResult(r,p,g); if(AlarmScheduler.isEnabled(this))AlarmScheduler.scheduleAll(this); updateAlertStatus(); }
    @Override protected void onDestroy(){ handler.removeCallbacks(tick); super.onDestroy(); }
}
