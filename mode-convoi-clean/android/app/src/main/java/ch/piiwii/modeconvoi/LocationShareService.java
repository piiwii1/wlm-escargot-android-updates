package ch.piiwii.modeconvoi;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.*;

public class LocationShareService extends Service implements LocationListener {
    public static final String CHANNEL = "convoy_location";
    public static final String PREF_GPS_LAT = "gpsLat";
    public static final String PREF_GPS_LON = "gpsLon";
    public static final String PREF_GPS_ACC = "gpsAccuracy";
    public static final String PREF_GPS_FIX_AT = "gpsLastFixAt";
    public static final String PREF_GPS_SENT_AT = "gpsLastSentAt";
    public static final String PREF_GPS_ERROR = "gpsLastError";
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
        // 0.3.19: the live talkie uses WebRTC; do not poll/store voice-note audio anymore.
        return START_STICKY;
    }

    private void startLocation() {
        if (!hasLocationPermission()) {
            prefs.put(PREF_GPS_ERROR, "Permission de localisation absente");
            return;
        }
        boolean anyProvider=false;
        try { if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){ lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 3, this); anyProvider=true; } } catch (Exception ignored) {}
        try { if(lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){ lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 5, this); anyProvider=true; } } catch (Exception ignored) {}
        if(!anyProvider) prefs.put(PREF_GPS_ERROR, "Localisation du téléphone désactivée");
        pushBestLastKnownLocation();
    }

    private boolean hasLocationPermission(){
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void pushBestLastKnownLocation(){
        if(!hasLocationPermission()) return;
        try{
            Location best=null;
            for(String provider:new String[]{LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER}){
                try{
                    Location x=lm.getLastKnownLocation(provider);
                    if(x!=null && (best==null || x.getTime()>best.getTime())) best=x;
                }catch(Exception ignored){}
            }
            if(best!=null && System.currentTimeMillis()-best.getTime()<10*60_000L) onLocationChanged(best);
        }catch(Exception ignored){}
    }

    @Override public void onLocationChanged(Location loc) {
        long t = System.currentTimeMillis();
        prefs.putLocationFix(loc.getLatitude(), loc.getLongitude(), loc.hasAccuracy()?loc.getAccuracy():-1, t);
        float speed = loc.hasSpeed() ? loc.getSpeed() : 0f;
        long interval = speed > 1.5f ? 5000 : 18000;
        if (lastLocation != null && loc.hasAccuracy() && lastLocation.hasAccuracy() && loc.getAccuracy() > 80 && lastLocation.getAccuracy() < loc.getAccuracy()) return;
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
                prefs.putLong(PREF_GPS_SENT_AT, System.currentTimeMillis());
                prefs.remove(PREF_GPS_ERROR);
            } catch (Exception e) {
                String m=e.getMessage();
                prefs.put(PREF_GPS_ERROR, (m==null||m.trim().isEmpty())?"Envoi de position impossible":m.trim());
            }
        });
    }

    private void startEventPolling() {
        if (eventPollingStarted) return;
        eventPollingStarted = true;
        io.scheduleWithFixedDelay(this::pollEvents, 2, 8, TimeUnit.SECONDS);
    }

    private void pollEvents() {
        if (!prefs.hasActiveConvoy()) { stopSelf(); return; }
        try {
            JSONObject snapshot=ConvoySnapshotRepository.getForBackground(prefs);
            ConvoyEventProcessor.process(this,prefs,snapshot);
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

    private Notification buildNotification(int participants) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        String text = participants > 0 ? "Position partagée avec "+Math.max(0,participants-1)+" autre"+(participants>2?"s":"")+" participant"+(participants>2?"s":"") : "Position partagée avec le convoi";
        return new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Mode Convoi actif")
                .setContentText(text)
                .setOngoing(true).setContentIntent(pi).build();
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm=getSystemService(NotificationManager.class);
            NotificationChannel c = new NotificationChannel(CHANNEL, "Mode Convoi actif", NotificationManager.IMPORTANCE_LOW);
            c.setDescription("Partage de position actif pendant le trajet");
            c.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            c.setShowBadge(false);
            nm.createNotificationChannel(c);

            NotificationHelper.ensureAlertChannel(this);
        }
    }

    @Override public void onDestroy() {
        try { lm.removeUpdates(this); } catch (Exception ignored) {}
        io.shutdownNow();
        super.onDestroy();
    }
    @Override public android.os.IBinder onBind(Intent intent) { return null; }
}
