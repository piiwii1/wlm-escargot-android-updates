package ch.piiwii.suchard;
import android.Manifest;import android.app.Activity;import android.content.*;import android.content.pm.PackageManager;import android.graphics.Color;import android.graphics.drawable.GradientDrawable;import android.os.*;import android.provider.Settings;import android.net.Uri;import android.view.*;import android.widget.*;
public class MainActivity extends Activity {
 public static int BROWN=0xff62392f,PINK=0xffcb4e85,DARK=0xff262526,GRAY=0xff747071;
 public static int dp(android.content.Context c,int n){return (int)(c.getResources().getDisplayMetrics().density*n+0.5f);}
 public static TextView txt(android.content.Context c,String s,int size,int color,boolean bold){TextView t=new TextView(c);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,1);return t;}
 public static GradientDrawable bg(android.content.Context c,int col,int radius){GradientDrawable d=new GradientDrawable();d.setColor(col);d.setCornerRadius(dp(c,radius));return d;}
 public static TextView btn(android.content.Context c,String s,int col,int text){TextView b=txt(c,s,13,text,true);b.setGravity(Gravity.CENTER);b.setPadding(dp(c,11),dp(c,12),dp(c,11),dp(c,12));b.setBackground(bg(c,col,12));return b;}
 public static void gap(android.content.Context c,LinearLayout p,int height){View v=new View(c);p.addView(v,new LinearLayout.LayoutParams(1,dp(c,height)));}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BROWN);Notify.init(this);Notify.schedule(this);draw();if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},23);}
 void draw(){
 ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(0xfffaf7f5);LinearLayout root=new LinearLayout(this);root.setOrientation(1);scroll.addView(root);setContentView(scroll);
 LinearLayout banner=new LinearLayout(this);banner.setOrientation(1);banner.setPadding(dp(this,22),dp(this,25),dp(this,22),dp(this,27));
 banner.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff3e2620,0xff98505c,0xffc9718c}));root.addView(banner);
 banner.addView(txt(this,"PIIWII  ·  SORTIE DU SAMEDI",12,0xffffe5d8,true));gap(this,banner,13);
 banner.addView(txt(this,"Suchard\n200 ans",36,Color.WHITE,true));gap(this,banner,11);
 banner.addView(txt(this,"17 OCTOBRE 2026   ·   SION → NEUCHÂTEL",13,Color.WHITE,true));gap(this,banner,12);
 banner.addView(txt(this,"🍫 12 étapes     🚗 GPS direct",14,Color.WHITE,false));
 LinearLayout content=new LinearLayout(this);content.setOrientation(1);content.setPadding(dp(this,15),dp(this,16),dp(this,15),dp(this,26));root.addView(content);
 LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(this,15),dp(this,14),dp(this,15),dp(this,15));box.setBackground(bg(this,Color.WHITE,15));content.addView(box);
 box.addView(txt(this,"🔔 Notifications de départ",18,DARK,true));gap(this,box,8);
 box.addView(txt(this,"Chaque rappel propose « Y ALLER » pour lancer Google Maps.",13,GRAY,false));gap(this,box,10);
 boolean on=getSharedPreferences("suchard",0).getBoolean("on",true);
 TextView toggle=btn(this,on?"✓ RAPPELS ACTIVÉS":"ACTIVER LES RAPPELS",on?BROWN:PINK,Color.WHITE);box.addView(toggle);
 toggle.setOnClickListener(v->{getSharedPreferences("suchard",0).edit().putBoolean("on",!on).apply();Notify.schedule(this);draw();});
 gap(this,box,8);LinearLayout row=new LinearLayout(this);box.addView(row);
 TextView test=btn(this,"🔔 TESTER",0xfff4ebe7,BROWN);row.addView(test,new LinearLayout.LayoutParams(0,dp(this,45),1));
 test.setOnClickListener(v->{if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},23);else Notify.show(this,0,true);});
 TextView exact=btn(this,"⏱ PRÉCISION",0xfff4ebe7,BROWN);LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(0,dp(this,45),1);ep.leftMargin=dp(this,7);row.addView(exact,ep);
 exact.setOnClickListener(v->{if(Build.VERSION.SDK_INT>=31){try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));}catch(Exception e){Toast.makeText(this,"Autorise les alarmes dans les paramètres Android.",Toast.LENGTH_LONG).show();}}});
 gap(this,content,19);content.addView(txt(this,"PROGRAMME DU JOUR",14,BROWN,true));gap(this,content,7);
 content.addView(txt(this,"Bouton GPS pour chaque étape ; coche les visites terminées.",13,GRAY,false));gap(this,content,12);
 for(int i=0;i<Route.ALL.length;i++)Cards.draw(this,content,i,()->draw());
 gap(this,content,8);content.addView(txt(this,"Sugus géants : position précise non confirmée. Visites guidées complètes. Boudry retiré.\nSources : suchard2026.ch · lecaracrose.ch",12,GRAY,false));
 }
}
