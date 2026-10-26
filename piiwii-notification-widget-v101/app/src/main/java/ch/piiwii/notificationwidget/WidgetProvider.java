package ch.piiwii.notificationwidget;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.widget.RemoteViews;
public class WidgetProvider extends AppWidgetProvider{
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){refresh(c);}
 @Override public void onReceive(Context c,Intent i){super.onReceive(c,i);refresh(c);}
 static void refresh(Context c){
  AppWidgetManager m=AppWidgetManager.getInstance(c);
  int[] ids=m.getAppWidgetIds(new ComponentName(c,WidgetProvider.class));if(ids.length==0)return;
  // A physical charging connection overrides all other notifications.
  // When power is removed, recompute the best active notification.
  String id=null;
  PendingIntent click=null;
  if (isPhysicallyPluggedIn(c) && Categories.enabled(c,"charging")) {
    id="charging";
  } else {
    String demo=Categories.prefs(c).getString("preview_id","");
    if (!demo.isEmpty() && Categories.prefs(c).getLong("preview_until",0)>System.currentTimeMillis()) {
      id=demo;
    } else {
      NotificationMonitor.Pick p=NotificationMonitor.best(c);
      if (p!=null) { id=p.id; click=p.intent; }
    }
    if (id==null) id=battery(c);
  }
  String glyph=id==null?"⚪":Categories.glyph(c,id);
  if(click==null){
    Intent i=c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
    if(i==null)i=new Intent(c,MainActivity.class);
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
    click=PendingIntent.getActivity(c,0,i,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  }
  RemoteViews rv=new RemoteViews(c.getPackageName(),R.layout.widget);
  rv.setTextViewText(R.id.icon,glyph);
  rv.setOnClickPendingIntent(R.id.widget_root,click);
  m.updateAppWidget(ids,rv);
 }
 static boolean isPhysicallyPluggedIn(Context c) {
  Intent battery=c.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
  return battery!=null && battery.getIntExtra(BatteryManager.EXTRA_PLUGGED,0)!=0;
 }
 static String battery(Context c){
  Intent i=c.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
  if(i==null)return null;
  int level=i.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),status=i.getIntExtra(BatteryManager.EXTRA_STATUS,-1),plug=i.getIntExtra(BatteryManager.EXTRA_PLUGGED,0);
  if(level>=0&&level<=15&&Categories.enabled(c,"low"))return "low";
  if((status==BatteryManager.BATTERY_STATUS_FULL||level>=100&&plug!=0)&&Categories.enabled(c,"full"))return "full";
  if(plug!=0&&Categories.enabled(c,"charging"))return "charging";
  return null;
 }
}