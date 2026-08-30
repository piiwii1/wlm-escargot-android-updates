from pathlib import Path

p=Path('mode-convoi-build/android/app/src/main/java/ch/piiwii/modeconvoi/MainActivity.java')
s=p.read_text()

old='TextView diagnostic=text("Le bouton doit devenir rouge dès que le micro démarre.",11,false,muted);'
new='TextView diagnostic=text("Contour rouge sans convoi · vert pendant la parole.",11,false,muted);'
if old not in s:
    raise SystemExit('diagnostic text not found')
s=s.replace(old,new)

old='''            if(action==MotionEvent.ACTION_DOWN){\n                talkieFingerDown=true;v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);\n                setTalkiePressedVisual("● Ouverture du micro…",accent);\n                if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){\n                    talkiePermissionRequestPending=true;setTalkiePressedVisual("● Autorisation micro…",accent);\n                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_AUDIO);return true;\n                }\n                startTalkieRecording();return true;\n            }'''
new='''            if(action==MotionEvent.ACTION_DOWN){\n                talkieFingerDown=true;v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);\n                if(!prefs.hasActiveConvoy()){\n                    setTalkiePressedVisual("✕ Aucun convoi actif",danger);\n                    return true;\n                }\n                setTalkiePressedVisual("● Ouverture du micro…",accent);\n                if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){\n                    talkiePermissionRequestPending=true;setTalkiePressedVisual("● Autorisation micro…",accent);\n                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_AUDIO);return true;\n                }\n                startTalkieRecording();return true;\n            }'''
if old not in s:
    raise SystemExit('touch block not found')
s=s.replace(old,new)

old='''    private void setTalkiePressedVisual(String message,int color){\n        if(talkiePttButton!=null)talkiePttButton.setBackground(roundBg(danger,danger,20,0));\n        if(talkieState!=null){talkieState.setText(message);talkieState.setTextColor(color);}\n    }'''
new='''    private void setTalkiePressedVisual(String message,int color){\n        if(talkiePttButton!=null){\n            boolean active=prefs.hasActiveConvoy();\n            boolean speaking=active&&talkieRecording;\n            int outline=speaking?Color.rgb(52,199,89):(active?accent:danger);\n            int stroke=speaking||!active?4:2;\n            talkiePttButton.setBackground(roundBg(accent,outline,20,stroke));\n        }\n        if(talkieState!=null){talkieState.setText(message);talkieState.setTextColor(color);}\n    }'''
if old not in s:
    raise SystemExit('pressed visual block not found')
s=s.replace(old,new)

old='if(!prefs.hasActiveConvoy()){setTalkieIdleVisual("✕ Aucun convoi actif",danger);return;}'
new='if(!prefs.hasActiveConvoy()){setTalkiePressedVisual("✕ Aucun convoi actif",danger);return;}'
if old not in s:
    raise SystemExit('inactive convoy guard not found')
s=s.replace(old,new)

p.write_text(s)
print('Mode Convoi 0.3.16 talkie outline patch applied')
