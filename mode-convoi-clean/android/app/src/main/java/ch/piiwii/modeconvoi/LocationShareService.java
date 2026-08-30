package ch.piiwii.modeconvoi;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.util.Base64;
import android.os.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.*;

public class LocationShareService extends Service implements LocationListener {
    public static final String CHANNEL = "convoy_location";
    private static final String EVENTS_CHANNEL = "convoy_alerts_v2";
    public static final String PREF_GPS_LAT = "gpsLat";
    public static final String PREF_GPS_LON = "gpsLon";
    public static final String PREF_GPS_ACC = "gpsAccuracy";
    public static final String PREF_GPS_FIX_AT = "gpsLastFixAt";
    public static final String PREF_GPS_SENT_AT = "gpsLastSentAt";
    public static final String PREF_GPS_ERROR = "gpsLastError";
    private LocationManager lm;
    private AppPrefs prefs;
    private final ScheduledExecutorService io = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService voiceIo = Executors.newSingleThreadScheduledExecutor();
    private long lastSent = 0;
    private Location lastLocation;
    private volatile boolean eventPollingStarted = false;
    private volatile boolean voicePollingStarted = false;
    private volatile long voiceRetryAt = 0;
    private final ConcurrentLinkedQueue<JSONObject> voiceQueue = new ConcurrentLinkedQueue<>();
    private volatile boolean voicePlaying = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

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
        prefs.put(PREF_GPS_LAT, String.valueOf(loc.getLatitude()));
        prefs.put(PREF_GPS_LON, String.valueOf(loc.getLongitude()));
        prefs.put(PREF_GPS_ACC, String.valueOf(loc.hasAccuracy()?loc.getAccuracy():-1));
        prefs.putLong(PREF_GPS_FIX_AT, t);
        prefs.remove(PREF_GPS_ERROR);
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

    private void startVoicePolling() {
        if (voicePollingStarted) return;
        voicePollingStarted = true;
        voiceIo.scheduleWithFixedDelay(this::pollVoice, 2, 2500, TimeUnit.MILLISECONDS);
    }

    private void pollVoice() {
        if (!prefs.hasActiveConvoy()) return;
        if (System.currentTimeMillis() < voiceRetryAt) return;
        final String base=prefs.get("serverUrl", ""), code=prefs.get("code", ""), id=prefs.get("participantId", ""), token=prefs.get("token", "");
        if (base.isEmpty() || code.isEmpty() || id.isEmpty() || token.isEmpty()) return;
        try {
            long after=prefs.getLong("lastVoiceId",0);
            String path="/api/convoys/"+code+"/voice?participantId="+URLEncoder.encode(id,"UTF-8")+"&token="+URLEncoder.encode(token,"UTF-8")+"&after="+after;
            JSONObject r=ConvoyApi.get(base,path);JSONArray messages=r.optJSONArray("messages");long max=after;boolean receive=prefs.getBool("talkieReceive",true);
            if(messages!=null)for(int i=0;i<messages.length();i++){
                JSONObject v=messages.optJSONObject(i);if(v==null)continue;long vid=v.optLong("id");max=Math.max(max,vid);if(!receive||id.equals(v.optString("participantId")))continue;voiceQueue.offer(v);
            }
            prefs.putLong("lastVoiceId",max);
            prefs.remove("talkieLastError");
            voiceRetryAt=0;
            if(!voiceQueue.isEmpty())mainHandler.post(this::playNextVoice);
        } catch(Exception e) {
            // IMPORTANT: the talkie-walkie is optional. A failure here must never
            // erase a valid convoy session. Core session validity is checked by pollEvents().
            if(e instanceof ConvoyApi.ApiException){
                int sc=((ConvoyApi.ApiException)e).statusCode;
                if(sc==404){
                    prefs.put("talkieLastError","Talkie indisponible sur le serveur");
                    voiceRetryAt=System.currentTimeMillis()+60000;
                }else if(sc==401){
                    prefs.put("talkieLastError","Talkie : authentification temporairement refusée");
                    voiceRetryAt=System.currentTimeMillis()+10000;
                }else{
                    prefs.put("talkieLastError","Talkie : erreur serveur "+sc);
                    voiceRetryAt=System.currentTimeMillis()+10000;
                }
            }else{
                String m=e.getMessage();
                prefs.put("talkieLastError",(m==null||m.trim().isEmpty())?"Talkie : connexion impossible":"Talkie : "+m.trim());
                voiceRetryAt=System.currentTimeMillis()+5000;
            }
        }
    }

    private void playNextVoice(){
        if(voicePlaying)return;JSONObject v=voiceQueue.poll();if(v==null)return;String audio=v.optString("audioBase64","");if(audio.isEmpty()){playNextVoice();return;}
        voicePlaying=true;File f=null;
        try{
            byte[] data=Base64.decode(audio,Base64.DEFAULT);f=new File(getCacheDir(),"talkie-in-"+v.optLong("id")+".m4a");try(FileOutputStream out=new FileOutputStream(f)){out.write(data);}final File ff=f;
            MediaPlayer mp=new MediaPlayer();mp.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());mp.setDataSource(ff.getAbsolutePath());
            mp.setOnCompletionListener(x->{try{x.release();}catch(Exception ignored){}ff.delete();voicePlaying=false;playNextVoice();});
            mp.setOnErrorListener((x,w,e)->{try{x.release();}catch(Exception ignored){}ff.delete();voicePlaying=false;playNextVoice();return true;});
            mp.prepare();mp.start();
        }catch(Exception e){if(f!=null)f.delete();voicePlaying=false;playNextVoice();}
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
            if(type.equals("status") || type.equals("rally") || type.equals("leave") || type.equals("join") || type.equals("remove") || type.equals("rename") || type.equals("close") || type.equals("role") || type.equals("emergency-stop") || type.equals("status-clear-auto")) {
                notifyEvent(e);
            }
        }
        prefs.putLong("lastEventId",max);
    }

    private void notifyEvent(JSONObject e) {
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

    @Override public void onDestroy() {
        try { lm.removeUpdates(this); } catch (Exception ignored) {}
        io.shutdownNow();
        voiceIo.shutdownNow();
        super.onDestroy();
    }
    @Override public android.os.IBinder onBind(Intent intent) { return null; }
}
