package ch.piiwii.suchard;
import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
 public static final int BROWN=0xff573226,PINK=0xffd05280,DARK=0xff302623,GRAY=0xff766c68,GOLD=0xffe9b971,CREAM=0xfffff8f2;
 private ScrollView scroll;
 private boolean showAll=false;
 private String lastRenderKey="";
 public static int dp(Context c,int n){return (int)(c.getResources().getDisplayMetrics().density*n+.5f);}
 public static TextView txt(Context c,String s,int size,int color,boolean bold){
  TextView t=new TextView(c);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
 }
 public static GradientDrawable bg(Context c,int color,int radius){
  GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;
 }
 public static TextView btn(Context c,String s,int color,int foreground){
  TextView b=txt(c,s,14,foreground,true);b.setGravity(Gravity.CENTER);
  b.setBackground(bg(c,color,14));b.setMinHeight(dp(c,48));b.setPadding(dp(c,14),dp(c,12),dp(c,14),dp(c,12));return b;
 }
 public static void gap(Context c,LinearLayout parent,int height){View v=new View(c);parent.addView(v,new LinearLayout.LayoutParams(1,dp(c,height)));}
 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  if(b!=null)showAll=b.getBoolean("expanded",false);
  getWindow().setStatusBarColor(CREAM);getWindow().setNavigationBarColor(CREAM);
  getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
  if(Build.VERSION.SDK_INT>=29)getWindow().setNavigationBarContrastEnforced(false);
  getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);
  Notify.init(this);Notify.schedule(this);render();
  int focus=getIntent().getIntExtra("focus_step",-1);if(focus>=0)focusStep(focus);
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
   requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},23);
 }
 @Override protected void onResume(){super.onResume();Notify.schedule(this);if(scroll!=null&&!nowKey().equals(lastRenderKey))keepPosition();}
 @Override protected void onSaveInstanceState(Bundle out){out.putBoolean("expanded",showAll);super.onSaveInstanceState(out);}
 private String nowKey(){java.util.Calendar c=java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Europe/Zurich"));return c.get(java.util.Calendar.YEAR)+"/"+c.get(java.util.Calendar.DAY_OF_YEAR)+"/"+c.get(java.util.Calendar.HOUR_OF_DAY)+":"+c.get(java.util.Calendar.MINUTE);}
 private int getNext(){
  android.content.SharedPreferences p=getSharedPreferences("suchard",0);
  int pending=0;for(int i=0;i<Route.ALL.length;i++)if(!p.getBoolean("done"+i,false))pending++;
  if(pending==0)return -1;
  java.util.Calendar now=java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Europe/Zurich"));
  if(now.get(java.util.Calendar.YEAR)!=2026||now.get(java.util.Calendar.MONTH)!=java.util.Calendar.OCTOBER||now.get(java.util.Calendar.DAY_OF_MONTH)!=17){
   for(int i=0;i<Route.ALL.length;i++)if(!p.getBoolean("done"+i,false))return i;
  }
  int minute=now.get(java.util.Calendar.HOUR_OF_DAY)*60+now.get(java.util.Calendar.MINUTE);
  for(int i=0;i<Route.ALL.length;i++){
   if(p.getBoolean("done"+i,false))continue;
   String[] ts=Route.ALL[i].time.split(":");
   int at=Integer.parseInt(ts[0])*60+Integer.parseInt(ts[1]);
   if(at>=minute-10)return i;
  }
  for(int i=Route.ALL.length-1;i>=0;i--)if(!p.getBoolean("done"+i,false))return i;
  return -1;
 }
 private void keepPosition(){
  int y=scroll==null?0:scroll.getScrollY();render();
  if(scroll!=null)scroll.post(()->scroll.scrollTo(0,y));
 }
 private void openProgramme(){
  showAll=true;render();
  ScrollView current=scroll;
  current.post(()->{
   View target=current.findViewWithTag("program");
   if(target!=null){Rect r=new Rect();target.getDrawingRect(r);current.offsetDescendantRectToMyCoords(target,r);current.smoothScrollTo(0,Math.max(0,r.top-dp(this,8)));}
  });
 }
 private void focusStep(int id){
  if(id<0||id>=Route.ALL.length)return;
  showAll=true;render();
  ScrollView current=scroll;
  current.post(()->{
   View card=current.findViewWithTag("stop_"+id);
   if(card!=null){Rect r=new Rect();card.getDrawingRect(r);current.offsetDescendantRectToMyCoords(card,r);current.smoothScrollTo(0,Math.max(0,r.top-dp(this,8)));}
  });
 }
 @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);focusStep(intent.getIntExtra("focus_step",-1));}
 private void setupInsets(ScrollView view){
  if(Build.VERSION.SDK_INT>=30){
   view.setClipToPadding(true);view.setClipChildren(true);
   view.setOnApplyWindowInsetsListener((v,insets)->{
    android.graphics.Insets upper=insets.getInsets(WindowInsets.Type.statusBars()|WindowInsets.Type.displayCutout());
    android.graphics.Insets lower=insets.getInsets(WindowInsets.Type.navigationBars());
    v.setPadding(0,Math.max(upper.top,dp(this,10)),0,Math.max(lower.bottom,dp(this,16)));
    return insets;
   });
   view.requestApplyInsets();
  }
 }
 public void render(){
  lastRenderKey=nowKey();
  ScrollView s=new ScrollView(this);
  s.setFillViewport(true);s.setBackgroundColor(CREAM);scroll=s;
  LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);
  s.addView(page);setContentView(s);setupInsets(s);
  int next=getNext();
  Hero.draw(this,page,next,()->openProgramme(),()->Tools.showDialog(this,()->keepPosition()));
  LinearLayout body=new LinearLayout(this);
  body.setPadding(dp(this,16),dp(this,14),dp(this,16),dp(this,30));
  body.setOrientation(LinearLayout.VERTICAL);page.addView(body);
  TextView section=txt(this,"LE PROGRAMME  •  SAMEDI 17 OCTOBRE",13,BROWN,true);section.setTag("program");body.addView(section);
  gap(this,body,6);
  int count=0;
  if(showAll){
   for(int i=0;i<Route.ALL.length;i++)Cards.draw(this,body,i,()->keepPosition());
   TextView less=btn(this,"AFFICHER MOINS",0xfff5ebe7,BROWN);body.addView(less);
   less.setOnClickListener(v->{showAll=false;render();});
  }else{
   int start=Math.max(0,next);
   if(next<0)start=Math.max(0,Route.ALL.length-2);
   for(int i=start;i<Route.ALL.length&&count<3;i++,count++)Cards.draw(this,body,i,()->keepPosition());
   gap(this,body,4);
   TextView more=btn(this,"VOIR LES 12 ÉTAPES  ↓",0xfff3e6df,BROWN);body.addView(more);
   more.setOnClickListener(v->openProgramme());
  }
  gap(this,body,16);
  TextView small=txt(this,"✓ Rappels et GPS conservés • Affiche et images hors ligne\nVisite de Boudry supprimée • Temps libre à 16h15",12,GRAY,false);
  small.setLineSpacing(dp(this,4),1f);body.addView(small);
 }
}
