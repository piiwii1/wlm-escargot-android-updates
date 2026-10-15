package ch.piiwii.suchard;
import android.Manifest;import android.app.*;import android.content.*;import android.content.pm.PackageManager;import android.os.*;import java.util.*;
public final class Notify {
 static final String CHANNEL="piiwii_suchard_departs";
 public static void init(Context c){
   if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=new NotificationChannel(CHANNEL,"Suchard 2026 - rappels de départ",NotificationManager.IMPORTANCE_HIGH);ch.setDescription("Notifications avec navigation GPS");ch.enableVibration(true);((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(ch);}
 }
 public static void schedule(Context c){
   AlarmManager mgr=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
   boolean enabled=c.getSharedPreferences("suchard",Context.MODE_PRIVATE).getBoolean("on",true);
   for(int i=0;i<Route.ALL.length;i++){
     Intent it=new Intent(c,Alarm.class).setAction("reminder_"+i).putExtra("id",i);
     PendingIntent pi=PendingIntent.getBroadcast(c,100+i,it,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
     mgr.cancel(pi);Route.Stop s=Route.ALL[i];if(!enabled||!s.reminder)continue;
     Calendar cal=Calendar.getInstance(TimeZone.getTimeZone("Europe/Zurich"));
     cal.set(2026,Calendar.OCTOBER,17,s.hour,s.minute,0);cal.set(Calendar.MILLISECOND,0);
     if(cal.getTimeInMillis()<=System.currentTimeMillis())continue;
     try{
       if(Build.VERSION.SDK_INT<31||mgr.canScheduleExactAlarms())
         mgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),pi);
       else mgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),pi);
     }catch(SecurityException e){mgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),pi);}
   }
 }
 public static void show(Context c,int id,boolean test){
   if(id<0||id>=Route.ALL.length)return;
   if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
   init(c);Route.Stop s=Route.ALL[id];
   String reminderTime=String.format(Locale.FRANCE,"%02d:%02d",s.hour,s.minute);
   String whenText=reminderTime.equals(s.time)?"Étape à "+s.time:"Rappel "+reminderTime+" · étape "+s.time;
   Intent view=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("focus_step",id).setAction("show_step_"+id);
   PendingIntent open=PendingIntent.getActivity(c,500+id,view,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   Intent nav=new Intent(c,Gps.class).setAction("nav_"+id).putExtra("id",id);
   PendingIntent go=PendingIntent.getActivity(c,700+id,nav,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   Notification.Builder n=new Notification.Builder(c,CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_info)
     .setContentTitle(test?"✅ Rappel de test : OK":"C’est le moment : "+s.title)
     .setContentText(whenText+" · "+s.place).setStyle(new Notification.BigTextStyle().bigText(whenText+" · "+s.note))
     .setContentIntent(open).setAutoCancel(true).setColor(MainActivity.PINK)
     .setCategory(Notification.CATEGORY_REMINDER);
   if(!s.destination.isEmpty())n.addAction(new Notification.Action.Builder(android.R.drawable.ic_menu_directions,"Y ALLER",go).build());
   n.addAction(new Notification.Action.Builder(android.R.drawable.ic_menu_view,"VOIR L'ÉTAPE",open).build());
   ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(test?2000:1000+id,n.build());
 }
 private Notify(){}
}
