from pathlib import Path
base=Path("piiwii-notification-widget-v104/app/src/main/java/ch/piiwii/notificationwidget")
p=base/"NotificationMonitor.java"
s=p.read_text(encoding="utf-8")
assert "private boolean powerRegistered=false;" in s
s=s.replace("import android.os.Build;", "import android.os.Build;\nimport android.os.Handler;\nimport android.os.Looper;")
s=s.replace("  private boolean powerRegistered=false;", """  private boolean powerRegistered=false;
  private final Handler mainHandler=new Handler(Looper.getMainLooper());
  private Runnable ephemeralExpiry;""")
s=s.replace("    if(powerRegistered)unregisterReceiver(chargingReceiver);", """    if(ephemeralExpiry!=null)mainHandler.removeCallbacks(ephemeralExpiry);
    if(powerRegistered)unregisterReceiver(chargingReceiver);""")
old="  @Override public void onNotificationPosted(StatusBarNotification s){WidgetProvider.refresh(this);}"
assert old in s
s=s.replace(old,"""  @Override public void onNotificationPosted(StatusBarNotification s){
    EphemeralEvents.onPosted(this,s);
    WidgetProvider.refresh(this);
    if(ephemeralExpiry!=null)mainHandler.removeCallbacks(ephemeralExpiry);
    if(EphemeralEvents.enabled(this)){
      long expiry=Categories.prefs(this).getLong("ephemeral_expires",0);
      if(expiry>System.currentTimeMillis()){
        ephemeralExpiry=()->WidgetProvider.refresh(NotificationMonitor.this);
        mainHandler.postDelayed(ephemeralExpiry,Math.max(100,expiry-System.currentTimeMillis()+120));
      }
    }
  }""")
needle="  static Pick best(Context c){"
assert needle in s
s=s.replace(needle,"""  static PendingIntent intentForKey(String key){
    NotificationMonitor monitor=instance;
    if(monitor==null || key==null || key.isEmpty())return null;
    try{
      StatusBarNotification[] all=monitor.getActiveNotifications();
      if(all!=null)for(StatusBarNotification sbn:all){
        if(key.equals(sbn.getKey()))return sbn.getNotification().contentIntent;
      }
    }catch(RuntimeException ignored){}
    return null;
  }
"""+needle)
p.write_text(s,encoding="utf-8")
p=base/"WidgetProvider.java"
s=p.read_text(encoding="utf-8")
assert 'Categories.prefs(c).edit().remove("preview_id").remove("preview_until").apply();' in s
s=s.replace('Categories.prefs(c).edit().remove("preview_id").remove("preview_until").apply();',
   'Categories.prefs(c).edit().remove("preview_id").remove("preview_until").apply();\n    if(action!=null&&action.equals(Intent.ACTION_BOOT_COMPLETED))EphemeralEvents.clear(c);',1)
a=s.index("    // Charge has absolute priority;")
b=s.index('    if(id==null)id="rest";',a)
s=s[:a]+"""    boolean ephemeral=EphemeralEvents.enabled(c);
    long previewEnd=Categories.prefs(c).getLong("preview_until",0L);
    String preview=Categories.prefs(c).getString("preview_id","");
    boolean activePreview=!preview.isEmpty()&&System.currentTimeMillis()<previewEnd;
    if(!activePreview&&!preview.isEmpty())
      Categories.prefs(c).edit().remove("preview_id").remove("preview_until").apply();

    if(ephemeral){
      // Charging is the persistent background; new alerts temporarily overlay it.
      if(activePreview){id=preview;scheduleRefresh(c,previewEnd);}
      else{
        EphemeralEvents.Item event=EphemeralEvents.current(c);
        if(event!=null){
          id=event.id;pkg=event.packageName;click=event.intent;
          scheduleRefresh(c,Categories.prefs(c).getLong("ephemeral_expires",0));
        }
      }
      if(id==null){
        if(battery.plugged&&battery.full&&Categories.enabled(c,"full"))id="full";
        else if(battery.plugged&&Categories.enabled(c,"charging"))id="charging";
        else if(battery.percent<=15&&Categories.enabled(c,"low"))id="low";
      }
    }else{
      // Original priority mode: physical charging stays above everything.
      if(battery.plugged&&battery.full&&Categories.enabled(c,"full"))id="full";
      else if(battery.plugged&&Categories.enabled(c,"charging"))id="charging";
      if(id==null){
        if(activePreview){id=preview;scheduleRefresh(c,previewEnd);}
        else{
          NotificationMonitor.Pick pick=NotificationMonitor.best(c);
          if(pick!=null){id=pick.id;pkg=pick.packageName;click=pick.intent;}
          if(battery.percent<=15&&Categories.enabled(c,"low")&&
             (pick==null||Categories.priority(c,"low")>pick.rank)){
            id="low";click=null;pkg=null;
          }
        }
      }
    }
"""+s[b:]
assert "private static void scheduleRefresh" in s
s=s.replace("private static void scheduleRefresh", "static void scheduleRefresh")
p.write_text(s,encoding="utf-8")
q=base/"EphemeralEvents.java"
assert q.exists()
print("1.0.4 notification modes and charging overlay: patched")
