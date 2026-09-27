package ch.piiwii.foirevalais;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    static final int PURPLE=Color.rgb(36,20,59), PINK=Color.rgb(215,25,104), GOLD=Color.rgb(244,197,66), BG=Color.rgb(247,245,250), MUTED=Color.rgb(108,99,120);
    LinearLayout content, dayRow; TextView title; int screen=0, selectedDay=0; SharedPreferences prefs;
    final String[] days={"Ven 2","Sam 3","Dim 4","Lun 5","Mar 6","Mer 7","Jeu 8","Ven 9","Sam 10","Dim 11"};
    final ArrayList<Event> events=new ArrayList<>();

    @Override public void onCreate(Bundle b){super.onCreate(b); prefs=getSharedPreferences("foire",MODE_PRIVATE); seed(); createChannel(); buildShell(); if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},50); showHome();}
    void seed(){
        events.add(new Event(1,0,"Ouverture de la 66e Foire du Valais","09:30","21:00","Martigny Expo","Foire","Ouverture générale de la Foire du Valais 2026. Les horaires de certains espaces peuvent différer.",false));
        events.add(new Event(2,1,"Cortège de la Foire du Valais","13:45","15:00","Départ : Poste de Martigny","Traditions","Fanfares, groupes folkloriques, costumes, animations et surprises. Événement gratuit et ouvert à tous.",true));
        events.add(new Event(3,3,"Café croissant · Conférence · Apéro","09:30","11:30","Espace Live","Seniors","Journée des Seniors — conférence Magazine Générations et moment convivial.",true));
        events.add(new Event(4,3,"Programme de l’Épicentre","09:00","11:00","Espace Tribus","Tribus","Animation de la Journée des Seniors par l’Épicentre.",true));
        events.add(new Event(5,3,"Concert Fanfare Les Gars du Rhône","11:30","12:15","Espace Tribus","Concert","Concert dans le cadre de la Journée des Seniors.",true));
        events.add(new Event(6,3,"Rencontre & grand jeu de l’oie des générations","13:30","15:00","Espace Live","Seniors","Rencontre intergénérationnelle et grand jeu de l’oie.",true));
        events.add(new Event(7,3,"Programme de l’Épicentre","13:30","15:00","Espace Tribus","Tribus","Deuxième séquence d’animations de l’Épicentre.",true));
        events.add(new Event(8,3,"Remise du Prix Senior Bravo","15:15","15:35","Espace Live","Seniors","Remise du Prix Bravo de la Fédération valaisanne des retraités.",true));
        events.add(new Event(9,3,"Dance for Fun — démonstration & danse","15:00","16:15","Espace Tribus","Animation","Démonstration du groupe Dance for Fun, avec invitation à danser.",true));
        events.add(new Event(10,3,"Concert Fanfare Les Gars du Rhône","15:45","16:30","Espace Live","Concert","Concert de clôture de la Journée des Seniors.",true));
        events.add(new Event(11,7,"Le Rendez-vous de l’Horlogerie 2026","10:10","12:00","Salle Bonne de Bourbon","Conférence","Conférences et table ronde autour de l’industrie horlogère avec notamment Jean-Claude Biver.",true));
        for(int d=0;d<10;d++) events.add(new Event(100+d,d,"Programme complet de cette journée","—","—","Espace Live · Tribus · autres espaces","À confirmer","Les créneaux non encore publiés ou non vérifiés officiellement seront ajoutés sans inventer d’horaires.",false));
    }
    void buildShell(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        LinearLayout head=new LinearLayout(this); head.setGravity(Gravity.CENTER_VERTICAL); head.setPadding(dp(18),dp(12),dp(16),dp(12)); head.setBackgroundColor(PURPLE);
        TextView logo=txt("FV",20,Color.WHITE,true); logo.setGravity(Gravity.CENTER); logo.setBackground(round(PINK,14)); logo.setPadding(dp(10),dp(8),dp(10),dp(8)); head.addView(logo,new LinearLayout.LayoutParams(dp(52),dp(48)));
        LinearLayout ht=new LinearLayout(this); ht.setOrientation(LinearLayout.VERTICAL); ht.setPadding(dp(12),0,0,0); TextView a=txt("FOIRE DU VALAIS",19,Color.WHITE,true); TextView s=txt("Martigny · 2–11 octobre 2026",12,0xFFE4DDED,false); ht.addView(a);ht.addView(s);head.addView(ht,new LinearLayout.LayoutParams(0,dp(52),1)); root.addView(head);
        title=txt("",23,PURPLE,true); title.setPadding(dp(18),dp(14),dp(18),dp(8)); root.addView(title);
        HorizontalScrollView hsv=new HorizontalScrollView(this); hsv.setHorizontalScrollBarEnabled(false); dayRow=new LinearLayout(this); dayRow.setPadding(dp(10),0,dp(10),dp(8)); hsv.addView(dayRow); root.addView(hsv);
        ScrollView scroll=new ScrollView(this); content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(14),dp(4),dp(14),dp(18));scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(4),dp(6),dp(4),dp(6));nav.setBackgroundColor(Color.WHITE);
        addNav(nav,"⌂\nAccueil",0);addNav(nav,"☰\nProgramme",1);addNav(nav,"★\nMes choix",2);addNav(nav,"⌖\nCarte",3);addNav(nav,"•••\nPlus",4); root.addView(nav,new LinearLayout.LayoutParams(-1,dp(68))); setContentView(root);
    }
    void addNav(LinearLayout n,String t,int i){TextView v=txt(t,12,i==0?PINK:MUTED,i==0);v.setGravity(Gravity.CENTER);v.setOnClickListener(x->{screen=i;if(i==0)showHome();else if(i==1)showProgram();else if(i==2)showChoices();else if(i==3)showMap();else showMore();});n.addView(v,new LinearLayout.LayoutParams(0,-1,1));}
    void dayButtons(boolean show){dayRow.removeAllViews(); if(!show)return; for(int i=0;i<10;i++){final int d=i; TextView b=txt(days[i],13,i==selectedDay?Color.WHITE:PURPLE,true);b.setGravity(Gravity.CENTER);b.setPadding(dp(12),dp(8),dp(12),dp(8));b.setBackground(round(i==selectedDay?PINK:0xFFEAE5EF,18));b.setOnClickListener(v->{selectedDay=d;showProgram();});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(40));lp.setMargins(dp(4),0,dp(4),0);dayRow.addView(b,lp);}}
    void clear(String t,boolean days){title.setText(t);content.removeAllViews();dayButtons(days);}
    void showHome(){clear("Ton compagnon pour les 10 jours",false); cardHero(); section("À ne pas manquer"); for(Event e:events) if(e.verified&&(e.id==2||e.id==11||e.id==5)) eventCard(e); section("Construis ton programme"); info("🔔 Choisis chaque animation qui t’intéresse. Tu peux activer une alerte 15 minutes avant, indépendamment pour chaque événement.");}
    void cardHero(){LinearLayout c=box(PURPLE); TextView k=txt("66e FOIRE DU VALAIS",12,GOLD,true);TextView big=txt("2 → 11 OCTOBRE\nMARTIGNY",28,Color.WHITE,true);TextView sub=txt("Concerts · Tribus · animations · traditions · conférences",13,0xFFE8E0F0,false);c.addView(k);c.addView(big);c.addView(sub);content.addView(c,margin(0,4,0,14));}
    void showProgram(){clear("Programme · "+days[selectedDay]+" octobre",true); for(Event e:events) if(e.day==selectedDay)eventCard(e);}
    void showChoices(){clear("Mes choix",false);boolean any=false;for(Event e:events)if(prefs.getBoolean("fav"+e.id,false)){eventCard(e);any=true;}if(!any)info("☆ Tu n’as encore rien sélectionné. Ouvre Programme puis touche ☆ sur les événements qui t’intéressent.");}
    void showMap(){clear("Carte & repères",false);info("📍 Martigny Expo\nRue du Levant 91 · 1920 Martigny\n\nRepères principaux : Espace Live, Espace Tribus, Salle Bonne de Bourbon et zones d’animations. La carte détaillée sera enrichie avec le plan officiel 2026.");Button b=new Button(this);b.setText("Ouvrir Martigny Expo dans Maps");b.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("geo:0,0?q=Martigny+Expo+Rue+du+Levant+91"));startActivity(i);});content.addView(b);}
    void showMore(){clear("Infos pratiques",false);info("📅 2–11 octobre 2026\n📍 Martigny Expo, Rue du Levant 91\n\nCette version privilégie uniquement les informations 2026 vérifiées. Les événements encore non publiés officiellement sont signalés au lieu d’être inventés.\n\nVersion 1.0.0 · données embarquées hors connexion.");}
    void eventCard(Event e){LinearLayout c=box(Color.WHITE);LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);TextView time=txt(e.time,15,PINK,true);TextView tag=txt(e.category.toUpperCase(),10,e.verified?PURPLE:MUTED,true);tag.setGravity(Gravity.CENTER);tag.setPadding(dp(8),dp(4),dp(8),dp(4));tag.setBackground(round(e.verified?0xFFF0E8F4:0xFFEDEDED,12));top.addView(time,new LinearLayout.LayoutParams(0,-2,1));top.addView(tag);c.addView(top);
        TextView name=txt(e.name,18,PURPLE,true);name.setPadding(0,dp(7),0,dp(3));c.addView(name);c.addView(txt("📍 "+e.place+(e.end.equals("—")?"":"   ·   "+e.time+"–"+e.end),12,MUTED,false));TextView desc=txt(e.desc,13,0xFF493E52,false);desc.setPadding(0,dp(7),0,dp(9));c.addView(desc);
        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER_VERTICAL); boolean fav=prefs.getBoolean("fav"+e.id,false); TextView star=txt(fav?"★ Ça m’intéresse":"☆ Ça m’intéresse",13,fav?PINK:PURPLE,true);star.setPadding(dp(10),dp(8),dp(10),dp(8));star.setBackground(round(fav?0xFFFFE6F0:0xFFF1EDF5,16));star.setOnClickListener(v->{boolean n=!prefs.getBoolean("fav"+e.id,false);prefs.edit().putBoolean("fav"+e.id,n).apply();if(n&&e.verified&&!e.time.equals("—"))schedule(e,15); if(screen==2)showChoices();else if(screen==1)showProgram();else showHome();}); actions.addView(star);
        if(fav&&e.verified&&!e.time.equals("—")){TextView bell=txt(" 🔔 15 min",12,PURPLE,true);bell.setPadding(dp(8),0,0,0);actions.addView(bell);} c.addView(actions);content.addView(c,margin(0,5,0,8));}
    void schedule(Event e,int before){try{Calendar cal=Calendar.getInstance();cal.set(2026,Calendar.OCTOBER,2+e.day,Integer.parseInt(e.time.substring(0,2)),Integer.parseInt(e.time.substring(3,5)),0);cal.add(Calendar.MINUTE,-before);if(cal.getTimeInMillis()<System.currentTimeMillis())return;Intent i=new Intent(this,EventReceiver.class);i.putExtra("title",e.name);i.putExtra("sub",e.place+" · "+e.time);PendingIntent pi=PendingIntent.getBroadcast(this,e.id,i,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms())am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),pi);else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),pi);}catch(Exception ignored){}}
    void createChannel(){if(Build.VERSION.SDK_INT>=26)((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel(EventReceiver.CHANNEL,"Mes événements",NotificationManager.IMPORTANCE_HIGH));}
    void section(String s){TextView v=txt(s,18,PURPLE,true);v.setPadding(dp(2),dp(8),0,dp(5));content.addView(v);}void info(String s){LinearLayout b=box(0xFFF0EBF5);b.addView(txt(s,14,PURPLE,false));content.addView(b,margin(0,4,0,10));}
    LinearLayout box(int color){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(15),dp(14),dp(15),dp(14));l.setBackground(round(color,18));l.setElevation(dp(1));return l;} TextView txt(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}GradientDrawable round(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}LinearLayout.LayoutParams margin(int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(l),dp(t),dp(r),dp(b));return p;}int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}    
    static class Event{int id,day;String name,time,end,place,category,desc;boolean verified;Event(int id,int day,String n,String t,String e,String p,String c,String d,boolean v){this.id=id;this.day=day;name=n;time=t;end=e;place=p;category=c;desc=d;verified=v;}}
}
