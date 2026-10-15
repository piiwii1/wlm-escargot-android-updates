package ch.piiwii.suchard;
import android.app.Activity;
import android.content.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
final class Hero {
 static void draw(Activity a,LinearLayout page,int next,Runnable programme,Runnable reminders){
  LinearLayout hero=new LinearLayout(a);
  hero.setOrientation(1);hero.setPadding(MainActivity.dp(a,18),MainActivity.dp(a,18),MainActivity.dp(a,18),MainActivity.dp(a,19));
  hero.setBackgroundColor(MainActivity.BROWN);page.addView(hero);
  LinearLayout header=new LinearLayout(a);header.setGravity(Gravity.CENTER_VERTICAL);hero.addView(header);
  ImageView logo=new ImageView(a);logo.setImageResource(R.drawable.icon_suchard);
  logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
  header.addView(logo,new LinearLayout.LayoutParams(MainActivity.dp(a,60),MainActivity.dp(a,60)));
  LinearLayout name=new LinearLayout(a);name.setOrientation(1);
  LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(0,-2,1);np.leftMargin=MainActivity.dp(a,12);header.addView(name,np);
  name.addView(MainActivity.txt(a,"PIIWII  ·  17 OCTOBRE 2026",11,0xfff9d4bc,true));
  name.addView(MainActivity.txt(a,"Suchard 200 ans",23,Color.WHITE,true));
  TextView poster=MainActivity.btn(a,"⋯",0xff865748,Color.WHITE);
  poster.setContentDescription("Infos pratiques, checklist et affiche");
  poster.setTextSize(11);poster.setPadding(MainActivity.dp(a,9),MainActivity.dp(a,5),MainActivity.dp(a,9),MainActivity.dp(a,5));header.addView(poster);
  poster.setOnClickListener(v->DayKit.menu(a));
  MainActivity.gap(a,hero,16);
  LinearLayout card=new LinearLayout(a);card.setOrientation(1);card.setPadding(MainActivity.dp(a,16),MainActivity.dp(a,15),MainActivity.dp(a,16),MainActivity.dp(a,16));
  card.setBackground(MainActivity.bg(a,0xfffff6ef,19));hero.addView(card);
  int complete=0;
  for(int i=0;i<Route.ALL.length;i++)if(a.getSharedPreferences("suchard",0).getBoolean("done"+i,false))complete++;
  LinearLayout progressRow=new LinearLayout(a);progressRow.setGravity(Gravity.CENTER_VERTICAL);card.addView(progressRow);
  progressRow.addView(MainActivity.txt(a,"TA JOURNÉE À NEUCHÂTEL",12,MainActivity.BROWN,true));
  TextView progressLabel=MainActivity.txt(a,complete+" / "+Route.ALL.length,12,MainActivity.GRAY,true);
  progressLabel.setGravity(Gravity.RIGHT);progressRow.addView(progressLabel,new LinearLayout.LayoutParams(0,-2,1));
  MainActivity.gap(a,card,7);
  ProgressBar progress=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal);
  progress.setMax(Route.ALL.length);progress.setProgress(complete);
  progress.setProgressTintList(android.content.res.ColorStateList.valueOf(MainActivity.PINK));
  progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xffe9dad1));
  card.addView(progress,new LinearLayout.LayoutParams(-1,MainActivity.dp(a,7)));
  MainActivity.gap(a,card,15);
  if(next>=0){
   Route.Stop step=Route.ALL[next];
   card.addView(MainActivity.txt(a,"PROCHAINE ÉTAPE  •  "+step.time,12,MainActivity.PINK,true));
   MainActivity.gap(a,card,5);
   card.addView(MainActivity.txt(a,step.emoji+"  "+step.title,21,MainActivity.DARK,true));
   MainActivity.gap(a,card,2);
   card.addView(MainActivity.txt(a,step.place,14,MainActivity.GRAY,false));
   String remaining=untilStep(step);
   if(remaining!=null){
    MainActivity.gap(a,card,8);
    card.addView(MainActivity.txt(a,remaining,12,MainActivity.BROWN,true));
   }
   MainActivity.gap(a,card,13);
   TextView gps=MainActivity.btn(a,step.destination.isEmpty()?"☀  TEMPS LIBRE":"↗  Y ALLER",MainActivity.PINK,Color.WHITE);
   card.addView(gps);
   MainActivity.gap(a,card,6);
   TextView finished=MainActivity.txt(a,"✓  Marquer cette étape terminée",12,MainActivity.GRAY,true);
   finished.setGravity(Gravity.CENTER);
   finished.setPadding(MainActivity.dp(a,9),MainActivity.dp(a,9),MainActivity.dp(a,9),MainActivity.dp(a,9));
   card.addView(finished);
   finished.setOnClickListener(v->{Notify.markDone(a,next,true);((MainActivity)a).render();});
   gps.setContentDescription("Ouvrir le GPS pour "+step.title);
   gps.setOnClickListener(v->{if(!step.destination.isEmpty())Gps.start(a,next);else Toast.makeText(a,"Promenade au volant libre, à votre rythme.",Toast.LENGTH_SHORT).show();});
  }else{
   card.addView(MainActivity.txt(a,"🎉 Le parcours est terminé !",19,MainActivity.DARK,true));
  }
  MainActivity.gap(a,hero,11);
  LinearLayout quick=new LinearLayout(a);quick.setGravity(Gravity.CENTER_VERTICAL);hero.addView(quick);
  TextView schedule=MainActivity.btn(a,"☷  PROGRAMME",0xffffe5d7,MainActivity.BROWN);
  quick.addView(schedule,new LinearLayout.LayoutParams(0,MainActivity.dp(a,45),1));
  schedule.setOnClickListener(v->programme.run());
  TextView alerts=MainActivity.btn(a,"🔔  RAPPELS",0xff785247,Color.WHITE);
  LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,MainActivity.dp(a,45),1);
  ap.leftMargin=MainActivity.dp(a,8);quick.addView(alerts,ap);
  alerts.setOnClickListener(v->reminders.run());
 }
 private static String untilStep(Route.Stop step){
  java.util.Calendar now=java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Europe/Zurich"));
  if(now.get(java.util.Calendar.YEAR)!=2026||now.get(java.util.Calendar.MONTH)!=java.util.Calendar.OCTOBER||now.get(java.util.Calendar.DAY_OF_MONTH)!=17)return null;
  String[] parts=step.time.split(":");
  int when=Integer.parseInt(parts[0])*60+Integer.parseInt(parts[1]);
  int current=now.get(java.util.Calendar.HOUR_OF_DAY)*60+now.get(java.util.Calendar.MINUTE);
  int remaining=when-current;
  if(remaining<0&&remaining>=-10)return "● C'est le moment";
  if(remaining<0||remaining>180)return null;
  if(remaining==0)return "● C'est maintenant";
  if(remaining<60)return "⏱ Dans "+remaining+" min";
  return "⏱ Dans "+(remaining/60)+" h"+(remaining%60>0?" "+(remaining%60)+" min":"");
 }

}
