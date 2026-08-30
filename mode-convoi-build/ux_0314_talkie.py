from pathlib import Path

root=Path('mode-convoi-build/android')
main=root/'app/src/main/java/ch/piiwii/modeconvoi/MainActivity.java'
s=main.read_text()

def once(old,new,label):
    global s
    if old not in s:
        raise SystemExit(f'{label}: pattern missing')
    s=s.replace(old,new,1)

once('import android.graphics.drawable.GradientDrawable;\n','import android.graphics.drawable.GradientDrawable;\nimport android.media.MediaRecorder;\n','media recorder import')
once('import java.io.InputStream;\n','import java.io.InputStream;\nimport java.io.File;\nimport java.io.FileInputStream;\n','file imports')
once('private static final int REQ_LOCATION = 1001, REQ_NOTIF = 1002, REQ_VEHICLE_IMAGE = 2001;','private static final int REQ_LOCATION = 1001, REQ_NOTIF = 1002, REQ_AUDIO = 1003, REQ_VEHICLE_IMAGE = 2001;','audio request code')
once('    private WebView fullScreenMapView;\n    private boolean fullScreenMapReady=false;\n','''    private WebView fullScreenMapView;\n    private boolean fullScreenMapReady=false;\n    private MediaRecorder talkieRecorder;\n    private File talkieFile;\n    private boolean talkieRecording=false;\n    private long talkieStartedAt=0;\n    private TextView talkieState;\n    private View talkiePttButton;\n    private final Runnable talkieAutoStop=()->{ if(talkieRecording) stopTalkieRecording(true); };\n''','talkie fields')
once('@Override protected void onDestroy() { io.shutdownNow(); super.onDestroy(); }','@Override protected void onDestroy() { ui.removeCallbacks(talkieAutoStop); if(talkieRecording){try{talkieRecorder.stop();}catch(Exception ignored){} talkieRecording=false;} releaseTalkieRecorder(); io.shutdownNow(); super.onDestroy(); }','destroy recorder')
once('''        snapshotArea=new LinearLayout(this); snapshotArea.setOrientation(LinearLayout.VERTICAL); content.addView(snapshotArea,new LinearLayout.LayoutParams(-1,-2)); refreshSnapshotArea();\n        sectionLabel(content,"ACTIONS RAPIDES");''','''        snapshotArea=new LinearLayout(this); snapshotArea.setOrientation(LinearLayout.VERTICAL); content.addView(snapshotArea,new LinearLayout.LayoutParams(-1,-2)); refreshSnapshotArea();\n        addTalkieWalkieSection();\n        sectionLabel(content,"ACTIONS RAPIDES");''','home talkie')
once('''        if(req==REQ_LOCATION){\n            if(hasLocationPermission()){ startShareService(); requestNotificationPermissionIfNeeded(); if("map".equals(currentPage)) pushMap(); }\n            else toast("La localisation est nécessaire pour apparaître dans le convoi");\n        }\n    }''','''        if(req==REQ_LOCATION){\n            if(hasLocationPermission()){ startShareService(); requestNotificationPermissionIfNeeded(); if("map".equals(currentPage)) pushMap(); }\n            else toast("La localisation est nécessaire pour apparaître dans le convoi");\n        } else if(req==REQ_AUDIO){\n            if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED) toast("Micro autorisé — maintiens le bouton pour parler");\n            else toast("Autorise le micro pour utiliser le talkie-walkie");\n        }\n    }''','audio permission result')

marker='    private void startPolling(){ if(polling)return; polling=true; pollOnce(); }'
if marker not in s: raise SystemExit('startPolling marker missing')
methods=r'''    private void addTalkieWalkieSection(){
        sectionLabel(content,"TALKIE-WALKIE");
        LinearLayout card=cardBox();
        TextView hint=text("Maintiens pour parler · relâche pour envoyer",12,false,muted);hint.setGravity(Gravity.CENTER);hint.setPadding(0,0,0,dp(8));card.addView(hint);
        LinearLayout ptt=new LinearLayout(this);ptt.setOrientation(LinearLayout.VERTICAL);ptt.setGravity(Gravity.CENTER);ptt.setPadding(dp(10),dp(10),dp(10),dp(8));ptt.setBackground(roundBg(accent,accent,20,0));
        TextView mic=text("🎙️",34,false,Color.rgb(20,22,24));mic.setGravity(Gravity.CENTER);ptt.addView(mic,new LinearLayout.LayoutParams(-1,dp(45)));
        TextView label=text("MAINTENIR POUR PARLER",14,true,Color.rgb(20,22,24));label.setGravity(Gravity.CENTER);ptt.addView(label,new LinearLayout.LayoutParams(-1,dp(28)));
        LinearLayout.LayoutParams pttLp=new LinearLayout.LayoutParams(-1,dp(84));pttLp.setMargins(0,dp(2),0,dp(8));card.addView(ptt,pttLp);talkiePttButton=ptt;
        talkieState=text("● Prêt",11,true,Color.rgb(90,200,120));talkieState.setGravity(Gravity.CENTER);card.addView(talkieState,new LinearLayout.LayoutParams(-1,dp(24)));
        Button receive=ghostButton(prefs.getBool("talkieReceive",true)?"🔊  RÉCEPTION AUTOMATIQUE : OUI":"🔇  RÉCEPTION AUTOMATIQUE : NON");
        receive.setOnClickListener(v->{boolean on=!prefs.getBool("talkieReceive",true);prefs.putBool("talkieReceive",on);receive.setText(on?"🔊  RÉCEPTION AUTOMATIQUE : OUI":"🔇  RÉCEPTION AUTOMATIQUE : NON");toast(on?"Réception talkie activée":"Réception talkie coupée");});card.addView(receive);
        ptt.setOnTouchListener((v,e)->{
            if(e.getAction()==MotionEvent.ACTION_DOWN){startTalkieRecording();return true;}
            if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL){stopTalkieRecording(false);return true;}
            return true;
        });
        content.addView(card);
    }
    private void startTalkieRecording(){
        if(talkieRecording||!prefs.hasActiveConvoy())return;
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_AUDIO);return;}
        try{
            talkieFile=new File(getCacheDir(),"talkie-"+System.currentTimeMillis()+".m4a");
            talkieRecorder=Build.VERSION.SDK_INT>=31?new MediaRecorder(this):new MediaRecorder();
            talkieRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);talkieRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);talkieRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);talkieRecorder.setAudioChannels(1);talkieRecorder.setAudioSamplingRate(16000);talkieRecorder.setAudioEncodingBitRate(32000);talkieRecorder.setOutputFile(talkieFile.getAbsolutePath());talkieRecorder.prepare();talkieRecorder.start();
            talkieRecording=true;talkieStartedAt=System.currentTimeMillis();ui.postDelayed(talkieAutoStop,15000);
            if(talkiePttButton!=null)talkiePttButton.setBackground(roundBg(danger,danger,20,0));if(talkieState!=null){talkieState.setText("● PARLE…");talkieState.setTextColor(danger);}
        }catch(Exception ex){releaseTalkieRecorder();toast("Impossible d’ouvrir le micro");}
    }
    private void stopTalkieRecording(boolean automatic){
        if(!talkieRecording)return;talkieRecording=false;ui.removeCallbacks(talkieAutoStop);long duration=System.currentTimeMillis()-talkieStartedAt;
        try{talkieRecorder.stop();}catch(Exception ignored){}releaseTalkieRecorder();
        if(talkiePttButton!=null)talkiePttButton.setBackground(roundBg(accent,accent,20,0));
        if(duration<350||talkieFile==null||!talkieFile.exists()||talkieFile.length()<64){if(talkieFile!=null)talkieFile.delete();if(talkieState!=null){talkieState.setText("● Prêt");talkieState.setTextColor(Color.rgb(90,200,120));}return;}
        if(talkieState!=null){talkieState.setText("↑ Envoi…");talkieState.setTextColor(accent);}sendTalkieMessage(talkieFile,duration,automatic);
    }
    private void releaseTalkieRecorder(){try{if(talkieRecorder!=null)talkieRecorder.release();}catch(Exception ignored){}talkieRecorder=null;}
    private void sendTalkieMessage(File file,long duration,boolean automatic){
        io.execute(()->{
            try{
                if(file.length()>120000)throw new Exception("Message vocal trop volumineux");
                byte[] data=new byte[(int)file.length()];try(FileInputStream in=new FileInputStream(file)){int off=0,n;while(off<data.length&&(n=in.read(data,off,data.length-off))>0)off+=n;}
                JSONObject b=authBody().put("audioBase64",Base64.encodeToString(data,Base64.NO_WRAP)).put("durationMs",Math.min(15000,duration));
                ConvoyApi.post(prefs.get("serverUrl",""),"/api/convoys/"+prefs.get("code","")+"/voice",b,null);
                ui.post(()->{if(talkieState!=null){talkieState.setText(automatic?"✓ Envoyé · limite 15 s":"✓ Envoyé");talkieState.setTextColor(Color.rgb(90,200,120));ui.postDelayed(()->{if(talkieState!=null){talkieState.setText("● Prêt");talkieState.setTextColor(Color.rgb(90,200,120));}},1500);}});
            }catch(Exception ex){ui.post(()->{if(talkieState!=null){talkieState.setText("✕ Envoi impossible");talkieState.setTextColor(danger);}toast(ex.getMessage()==null?"Envoi talkie impossible":ex.getMessage());});}
            finally{file.delete();}
        });
    }

'''
s=s.replace(marker,methods+marker,1)
main.write_text(s)

prefs=root/'app/src/main/java/ch/piiwii/modeconvoi/AppPrefs.java'
a=prefs.read_text()
old='remove("code", "convoyName", "participantId", "token", "adminKey", "lastEventId");'
if old not in a: raise SystemExit('AppPrefs clearSession pattern missing')
a=a.replace(old,'remove("code", "convoyName", "participantId", "token", "adminKey", "lastEventId", "lastVoiceId");',1)
prefs.write_text(a)

manifest=root/'app/src/main/AndroidManifest.xml'
m=manifest.read_text()
old='    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />'
if old not in m: raise SystemExit('manifest permission marker missing')
m=m.replace(old,old+'\n    <uses-permission android:name="android.permission.RECORD_AUDIO" />',1)
manifest.write_text(m)

service=root/'app/src/main/java/ch/piiwii/modeconvoi/LocationShareService.java'
ss=service.read_text()
def sonce(old,new,label):
    global ss
    if old not in ss: raise SystemExit(f'{label}: missing')
    ss=ss.replace(old,new,1)
sonce('import android.location.*;\n','import android.location.*;\nimport android.media.AudioAttributes;\nimport android.media.MediaPlayer;\nimport android.util.Base64;\n','service media imports')
sonce('import java.net.URLEncoder;\n','import java.net.URLEncoder;\nimport java.io.File;\nimport java.io.FileOutputStream;\nimport java.util.concurrent.ConcurrentLinkedQueue;\n','service file imports')
sonce('    private volatile boolean eventPollingStarted = false;\n','''    private volatile boolean eventPollingStarted = false;\n    private volatile boolean voicePollingStarted = false;\n    private final ConcurrentLinkedQueue<JSONObject> voiceQueue = new ConcurrentLinkedQueue<>();\n    private volatile boolean voicePlaying = false;\n    private final Handler mainHandler = new Handler(Looper.getMainLooper());\n''','service fields')
sonce('        startEventPolling();\n        return START_STICKY;','        startEventPolling();\n        startVoicePolling();\n        return START_STICKY;','service start voice')
marker='    private void pollEvents() {'
if marker not in ss: raise SystemExit('pollEvents marker missing')
voice=r'''    private void startVoicePolling() {
        if (voicePollingStarted) return;
        voicePollingStarted = true;
        io.scheduleWithFixedDelay(this::pollVoice, 1, 1500, TimeUnit.MILLISECONDS);
    }

    private void pollVoice() {
        if (!prefs.hasActiveConvoy()) return;
        final String base=prefs.get("serverUrl", ""), code=prefs.get("code", ""), id=prefs.get("participantId", ""), token=prefs.get("token", "");
        if (base.isEmpty() || code.isEmpty() || id.isEmpty() || token.isEmpty()) return;
        try {
            long after=prefs.getLong("lastVoiceId",0);
            String path="/api/convoys/"+code+"/voice?participantId="+URLEncoder.encode(id,"UTF-8")+"&token="+URLEncoder.encode(token,"UTF-8")+"&after="+after;
            JSONObject r=ConvoyApi.get(base,path);JSONArray messages=r.optJSONArray("messages");long max=after;boolean receive=prefs.getBool("talkieReceive",true);
            if(messages!=null)for(int i=0;i<messages.length();i++){
                JSONObject v=messages.optJSONObject(i);if(v==null)continue;long vid=v.optLong("id");max=Math.max(max,vid);if(!receive||id.equals(v.optString("participantId")))continue;voiceQueue.offer(v);
            }
            prefs.putLong("lastVoiceId",max);if(!voiceQueue.isEmpty())mainHandler.post(this::playNextVoice);
        } catch(Exception e) {
            if(e instanceof ConvoyApi.ApiException){int sc=((ConvoyApi.ApiException)e).statusCode;if(sc==401||sc==404){prefs.clearSession();stopSelf();}}
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

'''
ss=ss.replace(marker,voice+marker,1)
service.write_text(ss)

print('Mode Convoi 0.3.14 talkie-walkie patch applied')
