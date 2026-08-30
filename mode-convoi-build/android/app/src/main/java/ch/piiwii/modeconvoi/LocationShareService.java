package ch.piiwii.modeconvoi;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.util.concurrent.*;

public class LocationShareService extends Service implements LocationListener {
    public static final String CHANNEL = "convoy_location";
    private static final String EVENTS_CHANNEL = "convoy_events";
    private LocationManager lm;
    private AppPrefs prefs;
    private final ScheduledExecutorService io = Executors.newSingleThreadScheduledExecutor();
    private long lastSent = 0;
    private Location lastLocation;
    private volatile boolean eventPollingStarted = false;

    @Override public void onCreate() {
        super.onCreate();
        prefs = new AppPrefs(this);
        createChannels();
        lm = (LocationManager) getSystemService(LOCATION_SERVICE);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!prefs.hasActiveConvoy()) { stopSelf(); return START_NOT_STICKY; }
        startForeground(4101, buildNotification(-1));
        startLocation();
        startEventPolling();
        return START_STICKY;
    }

    private void startLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        try { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 3, this); } catch (Exception ignored) {}
        try { lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 8000, 10, this); } catch (Exception ignored) {}
    }

    @Override public void onLocationChanged(Location loc) {
        long t = System.currentTimeMillis();
        float speed = loc.hasSpeed() ? loc.getSpeed() : 0f;
        long interval = speed > 1.5f ? 5000 : 18000;
        if (lastLocation != null && loc.getAccuracy() > 80 && lastLocation.getAccuracy() < loc.getAccuracy()) return;
        lastLocation = loc;
        if (t - lastSent < interval) return;
        lastSent = t;
        send(loc);
    }

    private void send(Location l) {
        final String base=prefs.get("serverUrl", ""), code=prefs.get("code", ""), id=prefs.get("participantId", ""), token=prefs.get("token", "");
        if (base.isEmpty() || code.isEmpty() || id.isEmpty() || token.isEmpty()) return;
        io.execute(() -> {
            try {
                JSONObject b = new JSONObject();
                b.put("participantId", id).put("token", token).put("lat", l.getLatitude()).put("lon", l.getLongitude())
                        .put("accuracy", l.hasAccuracy()?l.getAccuracy():JSONObject.NULL)
                        .put("speed", l.hasSpeed()?l.getSpeed():JSONObject.NULL)
                        .put("bearing", l.hasBearing()?l.getBearing():JSONObject.NULL)
                        .put("deviceTime", l.getTime());
                ConvoyApi.post(base, "/api/convoys/"+code+"/location", b, null);
            } catch (Exception ignored) {}
        });
    }

    private void startEventPolling() {
        if (eventPollingStarted) return;
        eventPollingStarted = true;
        io.scheduleWithFixedDelay(this::pollEvents, 2, 8, TimeUnit.SECONDS);
    }

    private void pollEvents() {
        if (!prefs.hasActiveConvoy()) { stopSelf(); return; }
        final String base=prefs.get("serverUrl", ""), code=prefs.get("code", ""), id=prefs.get("participantId", ""), token=prefs.get("token", "");
        if (base.isEmpty() || code.isEmpty() || id.isEmpty() || token.isEmpty()) return;
        try {
            String path="/api/convoys/"+code+"?participantId="+URLEncoder.encode(id,"UTF-8")+"&token="+URLEncoder.encode(token,"UTF-8");
            JSONObject snapshot=ConvoyApi.get(base,path);
            processEvents(snapshot);
            JSONArray participants=snapshot.optJSONArray("participants");
            int count=participants==null?0:participants.length();
            getSystemService(NotificationManager.class).notify(4101, buildNotification(count));
        } catch (Exception e) {
            if(e instanceof ConvoyApi.ApiException){
                int sc=((ConvoyApi.ApiException)e).statusCode;
                if(sc==401||sc==404){prefs.clearSession();stopSelf();}
            }
        }
    }

    private void processEvents(JSONObject snapshot) {
        JSONArray events=snapshot.optJSONArray("events");
        if(events==null)return;
        long last=prefs.getLong("lastEventId",0),max=last;
        String me=prefs.get("participantId","");
        for(int i=0;i<events.length();i++){
            JSONObject e=events.optJSONObject(i); if(e==null)continue;
            long eventId=e.optLong("id"); max=Math.max(max,eventId);
            if(eventId<=last || me.equals(e.optString("participantId")))continue;
            String type=e.optString("type");
            if(type.equals("status") || type.equals("rally") || type.equals("leave") || type.equals("join") || type.equals("remove") || type.equals("rename") || type.equals("close") || type.equals("status-clear-auto")) {
                notifyEvent(e);
            }
        }
        prefs.putLong("lastEventId",max);
    }

    private void notifyEvent(JSONObject e) {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        Intent open=new Intent(this,MainActivity.class);
        PendingIntent pi=PendingIntent.getActivity(this,(int)(e.optLong("id")%100000),open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification n=new Notification.Builder(this,EVENTS_CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(e.optString("participantName","Mode Convoi"))
                .setContentText(e.optString("label","Nouvelle information"))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();
        getSystemService(NotificationManager.class).notify((int)(10000 + e.optLong("id")%100000),n);
    }

    private Notification buildNotification(int participants) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        String text = participants > 0 ? "Position partagée avec "+Math.max(0,participants-1)+" autre"+(participants>2?"s":"")+" participant"+(participants>2?"s":"") : "Position partagée avec le convoi";
        return new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("Mode Convoi actif")
                .setContentText(text)
                .setOngoing(true).setContentIntent(pi).build();
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm=getSystemService(NotificationManager.class);
            NotificationChannel c = new NotificationChannel(CHANNEL, "Partage de position", NotificationManager.IMPORTANCE_LOW);
            c.setDescription("Indique que le partage de position du convoi est actif");
            nm.createNotificationChannel(c);
            NotificationChannel e = new NotificationChannel(EVENTS_CHANNEL, "Informations du convoi", NotificationManager.IMPORTANCE_HIGH);
            e.setDescription("Statuts importants et points de regroupement");
            nm.createNotificationChannel(e);
        }
    }

    @Override public void onDestroy() {
        try { lm.removeUpdates(this); } catch (Exception ignored) {}
        io.shutdownNow();
        super.onDestroy();
    }
    @Override public android.os.IBinder onBind(Intent intent) { return null; }
}
