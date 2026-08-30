from pathlib import Path
import re

p=Path('mode-convoi-build/android/app/src/main/java/ch/piiwii/modeconvoi/MainActivity.java')
s=p.read_text()

# Add state fields.
s=s.replace('    private View talkiePttButton;\n    private final Runnable talkieAutoStop',
'''    private View talkiePttButton;\n    private boolean talkiePermissionRequestPending=false;\n    private boolean talkieFingerDown=false;\n    private final Runnable talkieAutoStop''')

# Permission callback: explicit visual state instead of silent/ambiguous flow.
old='''        } else if(req==REQ_AUDIO){\n            if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED) toast("Micro autorisé — maintiens le bouton pour parler");\n            else toast("Autorise le micro pour utiliser le talkie-walkie");\n        }'''
new='''        } else if(req==REQ_AUDIO){\n            talkiePermissionRequestPending=false;\n            if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED){\n                setTalkieIdleVisual("✓ Micro autorisé · maintiens à nouveau pour parler",Color.rgb(90,200,120));\n                toast("Micro autorisé — le talkie-walkie est prêt");\n            } else {\n                setTalkieIdleVisual("✕ Autorisation micro refusée",danger);\n                toast("Autorise le micro dans les réglages Android pour utiliser le talkie-walkie");\n            }\n        }'''
if old not in s:
    raise SystemExit('permission callback pattern missing')
s=s.replace(old,new)

# Replace talkie section + recorder lifecycle up to sendTalkieMessage.
pat=r'    private void addTalkieWalkieSection\(\)\{.*?(?=    private void sendTalkieMessage\(File file,long duration,boolean automatic\))'
repl=r'''    private void addTalkieWalkieSection(){
        sectionLabel(content,"TALKIE-WALKIE");
        LinearLayout card=cardBox();
        TextView hint=text("Maintiens pour parler · relâche pour envoyer",12,false,muted);hint.setGravity(Gravity.CENTER);hint.setPadding(0,0,0,dp(8));card.addView(hint);

        TextView ptt=text("🎙️\nMAINTENIR POUR PARLER",17,true,Color.rgb(20,22,24));
        ptt.setGravity(Gravity.CENTER);ptt.setPadding(dp(12),dp(10),dp(12),dp(10));ptt.setBackground(roundBg(accent,accent,20,0));
        ptt.setClickable(true);ptt.setFocusable(true);ptt.setSoundEffectsEnabled(true);
        LinearLayout.LayoutParams pttLp=new LinearLayout.LayoutParams(-1,dp(92));pttLp.setMargins(0,dp(2),0,dp(8));card.addView(ptt,pttLp);talkiePttButton=ptt;

        talkieState=text("● Prêt",12,true,Color.rgb(90,200,120));talkieState.setGravity(Gravity.CENTER);card.addView(talkieState,new LinearLayout.LayoutParams(-1,dp(28)));
        TextView diagnostic=text("Le bouton doit devenir rouge dès que le micro démarre.",11,false,muted);diagnostic.setGravity(Gravity.CENTER);diagnostic.setPadding(0,0,0,dp(6));card.addView(diagnostic);

        Button receive=ghostButton(prefs.getBool("talkieReceive",true)?"🔊  RÉCEPTION AUTOMATIQUE : OUI":"🔇  RÉCEPTION AUTOMATIQUE : NON");
        receive.setOnClickListener(v->{boolean on=!prefs.getBool("talkieReceive",true);prefs.putBool("talkieReceive",on);receive.setText(on?"🔊  RÉCEPTION AUTOMATIQUE : OUI":"🔇  RÉCEPTION AUTOMATIQUE : NON");toast(on?"Réception talkie activée":"Réception talkie coupée");});card.addView(receive);

        ptt.setOnTouchListener((v,e)->{
            int action=e.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN){
                talkieFingerDown=true;v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                setTalkiePressedVisual("● Ouverture du micro…",accent);
                if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
                    talkiePermissionRequestPending=true;setTalkiePressedVisual("● Autorisation micro…",accent);
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_AUDIO);return true;
                }
                startTalkieRecording();return true;
            }
            if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){
                talkieFingerDown=false;
                if(talkieRecording)stopTalkieRecording(false);
                else if(!talkiePermissionRequestPending)setTalkieIdleVisual("● Prêt",Color.rgb(90,200,120));
                v.performClick();return true;
            }
            return true;
        });
        ptt.setOnClickListener(v->{});
        content.addView(card);
    }
    private void setTalkiePressedVisual(String message,int color){
        if(talkiePttButton!=null)talkiePttButton.setBackground(roundBg(danger,danger,20,0));
        if(talkieState!=null){talkieState.setText(message);talkieState.setTextColor(color);}
    }
    private void setTalkieIdleVisual(String message,int color){
        if(talkiePttButton!=null)talkiePttButton.setBackground(roundBg(accent,accent,20,0));
        if(talkieState!=null){talkieState.setText(message);talkieState.setTextColor(color);}
    }
    private void startTalkieRecording(){
        if(talkieRecording)return;
        if(!prefs.hasActiveConvoy()){setTalkieIdleVisual("✕ Aucun convoi actif",danger);return;}
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            talkiePermissionRequestPending=true;setTalkiePressedVisual("● Autorisation micro…",accent);requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_AUDIO);return;
        }
        try{
            releaseTalkieRecorder();
            if(talkieFile!=null)try{talkieFile.delete();}catch(Exception ignored){}
            talkieFile=new File(getCacheDir(),"talkie-"+System.currentTimeMillis()+".m4a");
            talkieRecorder=Build.VERSION.SDK_INT>=31?new MediaRecorder(this):new MediaRecorder();
            talkieRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            talkieRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            talkieRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            talkieRecorder.setAudioEncodingBitRate(64000);
            talkieRecorder.setOutputFile(talkieFile.getAbsolutePath());
            talkieRecorder.prepare();
            talkieRecorder.start();
            talkieRecording=true;talkieStartedAt=System.currentTimeMillis();ui.postDelayed(talkieAutoStop,15000);
            setTalkiePressedVisual("● PARLE…",Color.WHITE);
        }catch(Exception ex){
            talkieRecording=false;releaseTalkieRecorder();
            if(talkieFile!=null)try{talkieFile.delete();}catch(Exception ignored){}
            String reason=ex.getMessage();if(reason==null||reason.trim().isEmpty())reason=ex.getClass().getSimpleName();
            setTalkieIdleVisual("✕ Micro indisponible",danger);toast("Micro : "+reason);
        }
    }
    private void stopTalkieRecording(boolean automatic){
        if(!talkieRecording){if(!talkiePermissionRequestPending)setTalkieIdleVisual("● Prêt",Color.rgb(90,200,120));return;}
        talkieRecording=false;ui.removeCallbacks(talkieAutoStop);long duration=System.currentTimeMillis()-talkieStartedAt;
        boolean stopped=true;try{talkieRecorder.stop();}catch(Exception ex){stopped=false;}releaseTalkieRecorder();
        if(!stopped||duration<300||talkieFile==null||!talkieFile.exists()||talkieFile.length()<96){
            if(talkieFile!=null)talkieFile.delete();setTalkieIdleVisual(stopped?"● Trop court — maintiens un peu plus":"✕ Enregistrement invalide",stopped?muted:danger);return;
        }
        if(talkieState!=null){talkieState.setText("↑ Envoi…");talkieState.setTextColor(accent);}if(talkiePttButton!=null)talkiePttButton.setBackground(roundBg(accent,accent,20,0));
        sendTalkieMessage(talkieFile,duration,automatic);
    }
    private void releaseTalkieRecorder(){try{if(talkieRecorder!=null){talkieRecorder.reset();talkieRecorder.release();}}catch(Exception ignored){}talkieRecorder=null;}

'''
s,n=re.subn(pat,lambda m: repl,s,flags=re.S)
if n!=1:
    raise SystemExit(f'talkie block replacement count={n}')

p.write_text(s)
print('0.3.15 talkie fix applied')
