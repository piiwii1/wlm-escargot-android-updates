from pathlib import Path
import re

ROOT=Path('mode-convoi-build/android/app/src/main')
java=ROOT/'java/ch/piiwii/modeconvoi'

# Dedicated monochrome small-notification icon. Android masks/tints small icons,
# so launcher/adaptive icons should not be used here.
drawable=ROOT/'res/drawable'
drawable.mkdir(parents=True, exist_ok=True)
(drawable/'ic_notification.xml').write_text('''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF"
        android:pathData="M3,6.5C3,5.12 4.12,4 5.5,4h13C19.88,4 21,5.12 21,6.5v8c0,1.38 -1.12,2.5 -2.5,2.5h-1.25a2.75,2.75 0,0 1,-5.25,0H9.25A2.75,2.75 0,0 1,4,17H3.5A2.5,2.5 0,0 1,1 14.5v-5C1,8.12 2.12,7 3.5,7H3zM6.5,15.5A1.5,1.5 0,1 0,6.5 18.5A1.5,1.5 0,0 0,6.5 15.5M14.5,15.5A1.5,1.5 0,1 0,14.5 18.5A1.5,1.5 0,0 0,14.5 15.5M5,7v5h14V7H5z"/>
</vector>''')

# MainActivity: use a fresh channel id so Android does not retain the old
# channel's user/device settings, and make foreground-generated alerts visible.
p=java/'MainActivity.java'
s=p.read_text()
s=s.replace('private static final String EVENTS_CHANNEL = "convoy_events";',
            'private static final String EVENTS_CHANNEL = "convoy_alerts_v2";',1)

old='''    private void notifyEvent(JSONObject e){NotificationManager nm=getSystemService(NotificationManager.class);Notification n=new Notification.Builder(this,EVENTS_CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(e.optString("participantName","Mode Convoi")).setContentText(e.optString("label","Nouvelle information")).setAutoCancel(true).build();nm.notify((int)(e.optLong("id")%Integer.MAX_VALUE),n);}'''
new='''    private void notifyEvent(JSONObject e){
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        NotificationManager nm=getSystemService(NotificationManager.class);
        String title=e.optString("participantName","Mode Convoi");
        String msg=e.optString("label","Nouvelle information");
        Intent open=new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi=PendingIntent.getActivity(this,(int)(e.optLong("id")%100000),open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=new Notification.Builder(this,EVENTS_CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(accent)
                .setContentTitle(title)
                .setContentText(msg)
                .setStyle(new Notification.BigTextStyle().bigText(msg))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_HIGH)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true);
        nm.notify((int)(10000+e.optLong("id")%100000),b.build());
    }'''
if old not in s: raise SystemExit('MainActivity notifyEvent pattern missing')
s=s.replace(old,new,1)

old='''    private void createEventChannel(){if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel(EVENTS_CHANNEL,"Informations du convoi",NotificationManager.IMPORTANCE_HIGH);c.setDescription("Statuts importants et point de regroupement");getSystemService(NotificationManager.class).createNotificationChannel(c);}}'''
new='''    private void createEventChannel(){
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel c=new NotificationChannel(EVENTS_CHANNEL,"Alertes Mode Convoi",NotificationManager.IMPORTANCE_HIGH);
            c.setDescription("Alertes des participants, regroupements et événements importants du convoi");
            c.enableVibration(true);c.setVibrationPattern(new long[]{0,220,110,260});
            c.enableLights(true);c.setLightColor(accent);
            c.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            c.setShowBadge(true);
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }'''
if old not in s: raise SystemExit('MainActivity createEventChannel pattern missing')
s=s.replace(old,new,1)

# Give a clear result when Android 13+ notification permission is refused.
old='''        } else if(req==REQ_AUDIO){'''
new='''        } else if(req==REQ_NOTIF){
            if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
                toast("Notifications refusées — les alertes écran verrouillé ne pourront pas s’afficher");
            else toast("Notifications Mode Convoi activées");
        } else if(req==REQ_AUDIO){'''
if old not in s: raise SystemExit('MainActivity permission result insertion point missing')
s=s.replace(old,new,1)
p.write_text(s)

# Background foreground-service alerts: same fresh channel and custom icon,
# public lockscreen visibility, high priority and explicit vibration/light.
p=java/'LocationShareService.java'
s=p.read_text()
s=s.replace('private static final String EVENTS_CHANNEL = "convoy_events";',
            'private static final String EVENTS_CHANNEL = "convoy_alerts_v2";',1)

pat=r'''    private void notifyEvent\(JSONObject e\) \{.*?\n    \}\n\n    private Notification buildNotification'''
m=re.search(pat,s,flags=re.S)
if not m: raise SystemExit('LocationShareService notifyEvent block missing')
replacement='''    private void notifyEvent(JSONObject e) {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        Intent open=new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi=PendingIntent.getActivity(this,(int)(e.optLong("id")%100000),open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        String title=e.optString("participantName","Mode Convoi");
        String msg=e.optString("label","Nouvelle information");
        Notification n=new Notification.Builder(this,EVENTS_CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(android.graphics.Color.rgb(255,181,20))
                .setContentTitle(title)
                .setContentText(msg)
                .setStyle(new Notification.BigTextStyle().bigText(msg))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_HIGH)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .build();
        getSystemService(NotificationManager.class).notify((int)(10000 + e.optLong("id")%100000),n);
    }

    private Notification buildNotification'''
s=s[:m.start()]+replacement+s[m.end():]

# Foreground-service status icon: use Mode Convoi notification glyph too.
s=s.replace('.setSmallIcon(android.R.drawable.ic_menu_mylocation)', '.setSmallIcon(R.drawable.ic_notification)',1)

pat=r'''    private void createChannels\(\) \{.*?\n    \}\n\n    @Override public void onDestroy'''
m=re.search(pat,s,flags=re.S)
if not m: raise SystemExit('LocationShareService createChannels block missing')
replacement='''    private void createChannels() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm=getSystemService(NotificationManager.class);
            NotificationChannel c = new NotificationChannel(CHANNEL, "Mode Convoi actif", NotificationManager.IMPORTANCE_LOW);
            c.setDescription("Partage de position actif pendant le trajet");
            c.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            c.setShowBadge(false);
            nm.createNotificationChannel(c);

            NotificationChannel e = new NotificationChannel(EVENTS_CHANNEL, "Alertes Mode Convoi", NotificationManager.IMPORTANCE_HIGH);
            e.setDescription("Alertes des participants, regroupements et événements importants du convoi");
            e.enableVibration(true);
            e.setVibrationPattern(new long[]{0,220,110,260});
            e.enableLights(true);
            e.setLightColor(android.graphics.Color.rgb(255,181,20));
            e.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            e.setShowBadge(true);
            nm.createNotificationChannel(e);
        }
    }

    @Override public void onDestroy'''
s=s[:m.start()]+replacement+s[m.end():]
p.write_text(s)

print('Mode Convoi 0.3.22 notifications patch applied')
