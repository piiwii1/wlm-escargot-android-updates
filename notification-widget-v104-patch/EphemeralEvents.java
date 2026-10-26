package ch.piiwii.notificationwidget;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.service.notification.StatusBarNotification;

/** Persists only a notification category/source and a short expiration, never message text. */
final class EphemeralEvents {
  static final String MODE_KEY="display_mode";
  static final String SECONDS_KEY="ephemeral_seconds";
  private static final String ID="ephemeral_id", PACKAGE="ephemeral_pkg", KEY="ephemeral_key";
  private static final String EXPIRES="ephemeral_expires", POSTED="ephemeral_posted";
  private EphemeralEvents(){}
  static boolean enabled(Context c){return "ephemeral".equals(Categories.prefs(c).getString(MODE_KEY,"priority"));}
  static int duration(Context c){return Math.max(3,Math.min(120,Categories.prefs(c).getInt(SECONDS_KEY,10)));}
  static synchronized void onPosted(Context c, StatusBarNotification sbn) {
    if(!enabled(c)||sbn==null)return;
    Notification notification=sbn.getNotification();
    if(notification==null)return;
    if((notification.flags&(Notification.FLAG_GROUP_SUMMARY|Notification.FLAG_ONGOING_EVENT|Notification.FLAG_FOREGROUND_SERVICE))!=0)return;
    String category=Categories.classify(sbn);
    if(category==null||!Categories.enabled(c,category))return;
    if("charging".equals(category)||"full".equals(category)||"low".equals(category))return;
    SharedPreferences pref=Categories.prefs(c);
    String key=sbn.getKey();
    long posted=sbn.getPostTime();
    if(key.equals(pref.getString(KEY,""))&&posted<=pref.getLong(POSTED,0))return;
    long expires=System.currentTimeMillis()+duration(c)*1000L;
    pref.edit().putString(ID,category).putString(PACKAGE,sbn.getPackageName())
      .putString(KEY,key).putLong(POSTED,posted).putLong(EXPIRES,expires).apply();
    WidgetProvider.scheduleRefresh(c,expires);
  }
  static synchronized Item current(Context c){
    SharedPreferences pref=Categories.prefs(c);
    String id=pref.getString(ID,"");
    if(id.isEmpty()||!enabled(c))return null;
    if(System.currentTimeMillis()>=pref.getLong(EXPIRES,0)){clear(c);return null;}
    if(!Categories.enabled(c,id))return null;
    return new Item(id,pref.getString(PACKAGE,""),NotificationMonitor.intentForKey(pref.getString(KEY,"")));
  }
  static synchronized void clear(Context c){
    Categories.prefs(c).edit().remove(ID).remove(PACKAGE).remove(KEY).remove(POSTED).remove(EXPIRES).apply();
  }
  static final class Item{
    final String id,packageName;
    final PendingIntent intent;
    Item(String id,String pkg,PendingIntent intent){this.id=id;packageName=pkg;this.intent=intent;}
  }
}