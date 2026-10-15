package ch.piiwii.suchard;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import java.util.*;

public final class Notify {
 static final String CHANNEL="piiwii_suchard_departs";
 private static final String PREF="suchard";
 public static void init(Context c){
  if(Build.VERSION.SDK_INT>=26){
   NotificationChannel channel=new NotificationChannel(CHANNEL,"Suchard : départs et visites",NotificationManager.IMPORTANCE_HIGH);
   channel.setDescription("Rappels du samedi 17 octobre, avec GPS et étape terminée");
   channel.enableVibration(true);
   ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
  }
 }
 public static void markDone(Context c,int id,boolean complete){
  if(id<0||id>=Route.ALL.length)return;
  c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putBoolean("done"+id,complete).apply();
  if(complete)((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(1000+id);
  schedule(c);
 }
 public static void schedule(Context c){
  AlarmManager alarm=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
  android.content.SharedPreferences preferences=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
  boolean enabled=preferences.getBoolean("on",true);
  for(int i=0;i<Route.ALL.length;i++){
   Intent intent=new Intent(c,Alarm.class).setAction("reminder_"+i).putExtra("id",i);
   PendingIntent pending=PendingIntent.getBroadcast(c,100+i,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   alarm.cancel(pending);
   Route.Stop stop=Route.ALL[i];
   if(!enabled||!stop.reminder||preferences.getBoolean("done"+i,false))continue;
   Calendar when=Calendar.getInstance(TimeZone.getTimeZone("Europe/Zurich"));
   when.set(2026,Calendar.OCTOBER,17,stop.hour,stop.minute,0);
   when.set(Calendar.MILLISECOND,0);
   long millis=when.getTimeInMillis();
   if(millis<=System.currentTimeMillis())continue;
   try{
    if(Build.VERSION.SDK_INT<31||alarm.canScheduleExactAlarms())
     alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,millis,pending);
    else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,millis,pending);
   }catch(SecurityException ex){alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,millis,pending);}
  }
 }
 public static void show(Context c,int id,boolean test){
  if(id<0||id>=Route.ALL.length)return;
  if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
  if(!test&&(c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getBoolean("done"+id,false)
       ||!c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getBoolean("on",true)))return;
  init(c);
  Route.Stop s=Route.ALL[id];
  String alarmTime=String.format(Locale.FRANCE,"%02d:%02d",s.hour,s.minute);
  String description=alarmTime.equals(s.time)?"Étape prévue : "+s.time:"Rappel : "+alarmTime+" · Étape : "+s.time;
  Intent openApp=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
  openApp.setAction("show_step_"+id).putExtra("focus_step",id);
  PendingIntent open=PendingIntent.getActivity(c,500+id,openApp,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Intent gps=new Intent(c,Gps.class).setAction("navigate_"+id).putExtra("id",id);
  PendingIntent go=PendingIntent.getActivity(c,700+id,gps,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder builder=new Notification.Builder(c,CHANNEL)
   .setSmallIcon(android.R.drawable.ic_dialog_info)
   .setContentTitle(test?"✅ Test des notifications réussi":"Suchard : "+s.title)
   .setContentText(description+" · "+s.place)
   .setStyle(new Notification.BigTextStyle().bigText(description+"\n"+s.place+"\n"+s.note))
   .setContentIntent(open).setAutoCancel(true)
   .setWhen(System.currentTimeMillis()).setShowWhen(true)
   .setColor(MainActivity.PINK).setCategory(Notification.CATEGORY_REMINDER);
  if(!s.destination.isEmpty())
   builder.addAction(new Notification.Action.Builder(android.R.drawable.ic_menu_directions,"Y ALLER",go).build());
  builder.addAction(new Notification.Action.Builder(android.R.drawable.ic_menu_view,"VOIR L'ÉTAPE",open).build());
  if(!test){
   Intent done=new Intent(c,FinishStage.class).setAction("done_"+id).putExtra("id",id);
   PendingIntent finished=PendingIntent.getBroadcast(c,900+id,done,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   builder.addAction(new Notification.Action.Builder(android.R.drawable.ic_menu_save,"✓ FAIT",finished).build());
  }
  ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE))
   .notify(test?2000:1000+id,builder.build());
 }
 private Notify(){}
}
