package ch.piiwii.notificationwidget;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.Notification;
import android.content.Context;
import android.app.PendingIntent;
public class NotificationMonitor extends NotificationListenerService{
 static volatile NotificationMonitor active;
 static class Pick{
  final String id;final long time;final int priority;final PendingIntent intent;
  Pick(String i,long t,int p,PendingIntent pi){id=i;time=t;priority=p;intent=pi;}
 }
 @Override public void onListenerConnected(){super.onListenerConnected();active=this;WidgetProvider.refresh(this);}
 @Override public void onListenerDisconnected(){active=null;WidgetProvider.refresh(this);super.onListenerDisconnected();}
 @Override public void onNotificationPosted(StatusBarNotification s){WidgetProvider.refresh(this);}
 @Override public void onNotificationRemoved(StatusBarNotification s){WidgetProvider.refresh(this);}
 static Pick best(Context c){
  if(active==null)return null;
  StatusBarNotification[] all;
  try{all=active.getActiveNotifications();}catch(Exception e){return null;}
  Pick best=null;if(all==null)return null;
  for(StatusBarNotification s:all){
   try{
    String id=Categories.classify(s);
    if(id==null||!Categories.enabled(c,id))continue;
    Notification n=s.getNotification();
    int rank=Categories.priority(id);
    if((n.flags&Notification.FLAG_ONGOING_EVENT)!=0&&!"call".equals(id))rank=Math.min(rank,25);
    if(best==null||rank>best.priority||(rank==best.priority&&s.getPostTime()>best.time))
       best=new Pick(id,s.getPostTime(),rank,n.contentIntent);
   }catch(Exception ignored){}
  }return best;
 }
}