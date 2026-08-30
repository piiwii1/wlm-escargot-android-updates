from pathlib import Path

# Patch LocationShareService: isolate voice polling from GPS/events and never let
# an optional talkie endpoint invalidate the whole convoy session.
p=Path('mode-convoi-build/android/app/src/main/java/ch/piiwii/modeconvoi/LocationShareService.java')
s=p.read_text()

repls=[
('private final ScheduledExecutorService io = Executors.newSingleThreadScheduledExecutor();',
 'private final ScheduledExecutorService io = Executors.newSingleThreadScheduledExecutor();\n    private final ScheduledExecutorService voiceIo = Executors.newSingleThreadScheduledExecutor();'),
('private volatile boolean voicePollingStarted = false;',
 'private volatile boolean voicePollingStarted = false;\n    private volatile long voiceRetryAt = 0;'),
('io.scheduleWithFixedDelay(this::pollVoice, 1, 1500, TimeUnit.MILLISECONDS);',
 'voiceIo.scheduleWithFixedDelay(this::pollVoice, 2, 2500, TimeUnit.MILLISECONDS);'),
('''    private void pollVoice() {\n        if (!prefs.hasActiveConvoy()) return;''',
 '''    private void pollVoice() {\n        if (!prefs.hasActiveConvoy()) return;\n        if (System.currentTimeMillis() < voiceRetryAt) return;'''),
('''            prefs.putLong("lastVoiceId",max);if(!voiceQueue.isEmpty())mainHandler.post(this::playNextVoice);\n        } catch(Exception e) {\n            if(e instanceof ConvoyApi.ApiException){int sc=((ConvoyApi.ApiException)e).statusCode;if(sc==401||sc==404){prefs.clearSession();stopSelf();}}\n        }''',
 '''            prefs.putLong("lastVoiceId",max);\n            prefs.remove("talkieLastError");\n            voiceRetryAt=0;\n            if(!voiceQueue.isEmpty())mainHandler.post(this::playNextVoice);\n        } catch(Exception e) {\n            // IMPORTANT: the talkie-walkie is optional. A failure here must never\n            // erase a valid convoy session. Core session validity is checked by pollEvents().\n            if(e instanceof ConvoyApi.ApiException){\n                int sc=((ConvoyApi.ApiException)e).statusCode;\n                if(sc==404){\n                    prefs.put("talkieLastError","Talkie indisponible sur le serveur");\n                    voiceRetryAt=System.currentTimeMillis()+60000;\n                }else if(sc==401){\n                    prefs.put("talkieLastError","Talkie : authentification temporairement refusée");\n                    voiceRetryAt=System.currentTimeMillis()+10000;\n                }else{\n                    prefs.put("talkieLastError","Talkie : erreur serveur "+sc);\n                    voiceRetryAt=System.currentTimeMillis()+10000;\n                }\n            }else{\n                String m=e.getMessage();\n                prefs.put("talkieLastError",(m==null||m.trim().isEmpty())?"Talkie : connexion impossible":"Talkie : "+m.trim());\n                voiceRetryAt=System.currentTimeMillis()+5000;\n            }\n        }'''),
('''        io.shutdownNow();\n        super.onDestroy();''',
 '''        io.shutdownNow();\n        voiceIo.shutdownNow();\n        super.onDestroy();''')
]
for old,new in repls:
    if old not in s:
        raise SystemExit('LocationShareService pattern missing: '+old[:80])
    s=s.replace(old,new,1)
p.write_text(s)

# Patch MainActivity: clear stale talkie errors on a fresh session and expose
# background talkie errors instead of silently appearing disconnected.
p=Path('mode-convoi-build/android/app/src/main/java/ch/piiwii/modeconvoi/MainActivity.java')
s=p.read_text()
old='''    private void saveSession(JSONObject r) {\n        prefs.put("code",r.optString("code")); prefs.put("convoyName",r.optString("convoyName")); prefs.put("participantId",r.optString("participantId")); prefs.put("token",r.optString("token"));'''
new='''    private void saveSession(JSONObject r) {\n        prefs.put("code",r.optString("code")); prefs.put("convoyName",r.optString("convoyName")); prefs.put("participantId",r.optString("participantId")); prefs.put("token",r.optString("token"));\n        prefs.remove("talkieLastError"); prefs.putLong("lastVoiceId",0);'''
if old not in s: raise SystemExit('saveSession pattern missing')
s=s.replace(old,new,1)

old='talkieState=text("● Prêt",12,true,Color.rgb(90,200,120));talkieState.setGravity(Gravity.CENTER);card.addView(talkieState,new LinearLayout.LayoutParams(-1,dp(28)));'
new='String talkieErr=prefs.get("talkieLastError",""); talkieState=text(talkieErr.isEmpty()?"● Prêt":"⚠ "+talkieErr,12,true,talkieErr.isEmpty()?Color.rgb(90,200,120):danger);talkieState.setGravity(Gravity.CENTER);card.addView(talkieState,new LinearLayout.LayoutParams(-1,dp(28)));'
if old not in s: raise SystemExit('talkieState pattern missing')
s=s.replace(old,new,1)

old='''                ConvoyApi.post(prefs.get("serverUrl",""),"/api/convoys/"+prefs.get("code","")+"/voice",b,null);\n                ui.post(()->{if(talkieState!=null){talkieState.setText(automatic?"✓ Envoyé · limite 15 s":"✓ Envoyé");talkieState.setTextColor(Color.rgb(90,200,120));ui.postDelayed(()->{if(talkieState!=null){talkieState.setText("● Prêt");talkieState.setTextColor(Color.rgb(90,200,120));}},1500);}});'''
new='''                ConvoyApi.post(prefs.get("serverUrl",""),"/api/convoys/"+prefs.get("code","")+"/voice",b,null);\n                prefs.remove("talkieLastError");\n                ui.post(()->{if(talkieState!=null){talkieState.setText(automatic?"✓ Envoyé · limite 15 s":"✓ Envoyé");talkieState.setTextColor(Color.rgb(90,200,120));ui.postDelayed(()->{if(talkieState!=null){talkieState.setText("● Prêt");talkieState.setTextColor(Color.rgb(90,200,120));}},1500);}});'''
if old not in s: raise SystemExit('talkie send success pattern missing')
s=s.replace(old,new,1)

p.write_text(s)
print('Mode Convoi 0.3.18 stability/talkie patch applied')
