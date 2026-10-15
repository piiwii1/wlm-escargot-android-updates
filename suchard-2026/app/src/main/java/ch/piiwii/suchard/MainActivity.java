package ch.piiwii.suchard;
import android.Manifest;import android.app.*;import android.content.*;import android.content.pm.PackageManager;import android.graphics.*;import android.graphics.drawable.*;import android.os.*;import android.view.*;import android.widget.*;
public class MainActivity extends Activity {
 public static final int BROWN=0xff573226,PINK=0xffd05280,DARK=0xff302623,GRAY=0xff766c68,GOLD=0xffe9b971,CREAM=0xfffff8f2;
 private ScrollView scroll;
 public static int dp(Context c,int n){return (int)(c.getResources().getDisplayMetrics().density*n+0.5f);}
 public static TextView txt(Context c,String s,int size,int color,boolean bold){TextView v=new TextView(c);v.setText(s);v.setTextSize(size);v.setTextColor(color);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
 public static GradientDrawable bg(Context c,int col,int radius){GradientDrawable d=new GradientDrawable();d.setColor(col);d.setCornerRadius(dp(c,radius));return d;}
 public static TextView btn(Context c,String s,int col,int text){TextView b=txt(c,s,13,text,true);b.setGravity(Gravity.CENTER);b.setBackground(bg(c,col,12));b.setPadding(dp(c,12),dp(c,11),dp(c,12),dp(c,11));return b;}
 public static void gap(Context c,LinearLayout p,int h){View v=new View(c);p.addView(v,new LinearLayout.LayoutParams(1,dp(c,h)));}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BROWN);getWindow().setNavigationBarColor(CREAM);Notify.init(this);Notify.schedule(this);render();if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},23);}
 @Override protected void onResume(){super.onResume();if(scroll!=null){Notify.schedule(this);}}
 int getNext(){
   boolean[] done=new boolean[Route.ALL.length];for(int i=0;i<done.length;i++)done[i]=getSharedPreferences("suchard",0).getBoolean("done"+i,false);
   java.util.Calendar now=java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Europe/Zurich"));
   int day=now.get(java.util.Calendar.DAY_OF_MONTH),month=now.get(java.util.Calendar.MONTH),year=now.get(java.util.Calendar.YEAR);
   int todayMinutes=now.get(java.util.Calendar.HOUR_OF_DAY)*60+now.get(java.util.Calendar.MINUTE);
   if(year!=2026||month!=java.util.Calendar.OCTOBER||day!=17){for(int i=0;i<done.length;i++)if(!done[i])return i;return -1;}
   int first=0;for(int i=0;i<Route.ALL.length;i++){String[] p=Route.ALL[i].time.split(":");int at=Integer.parseInt(p[0])*60+Integer.parseInt(p[1]);if(at<=todayMinutes+7)first=i;}
   for(int i=first;i<done.length;i++)if(!done[i])return i;return -1;
 }
 public void render(){
   scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(CREAM);
   LinearLayout page=new LinearLayout(this);page.setOrientation(1);scroll.addView(page);setContentView(scroll);
   Hero.draw(this,page,getNext());
   LinearLayout body=new LinearLayout(this);body.setPadding(dp(this,16),dp(this,18),dp(this,16),dp(this,30));body.setOrientation(1);page.addView(body);
   Tools.draw(this,body,()->render());
   gap(this,body,18);
   TextView title=txt(this,"TON PARCOURS DU SAMEDI",13,BROWN,true);body.addView(title);
   gap(this,body,7);
   body.addView(txt(this,"Appuie sur Y ALLER pour lancer le GPS. Chaque visite peut être cochée au fur et à mesure.",13,GRAY,false));
   gap(this,body,13);
   for(int i=0;i<Route.ALL.length;i++)Cards.draw(this,body,i,()->render());
   body.addView(txt(this,"Organisé pour le 17 octobre 2026 · horaires indicatifs hors rendez-vous officiels.\nVisites guidées complètes · Pas d'arrêt imposé à Boudry.\nImages : suchard2026.ch · Le Carac Rose",12,GRAY,false));
 }
}
