from pathlib import Path
import re

main=Path('mode-convoi-build/android/app/src/main/java/ch/piiwii/modeconvoi/MainActivity.java')
s=main.read_text()

s=s.replace('    private boolean polling;\n', '    private boolean polling;\n    private int consecutivePollFailures = 0;\n    private long lastSuccessfulSyncAt = 0;\n')

old='''        String savedServer=prefs.get("serverUrl","");\n        if(savedServer.isEmpty() || savedServer.startsWith("http://192.168.") || savedServer.startsWith("http://10.") || savedServer.startsWith("http://172.")) prefs.put("serverUrl",DEFAULT_SERVER);'''
new='''        String savedServer=prefs.get("serverUrl","");\n        boolean oldLocalServer=savedServer.startsWith("http://192.168.") || savedServer.startsWith("http://10.") || savedServer.startsWith("http://172.");\n        if(savedServer.isEmpty() || oldLocalServer) prefs.put("serverUrl",DEFAULT_SERVER);\n        if(oldLocalServer && prefs.hasActiveConvoy()) { prefs.clearSession(); snapshot=null; }'''
if old in s: s=s.replace(old,new,1)
else: raise SystemExit('server migration block not found')

s=s.replace('prefs.hasActiveConvoy()?"●  ACTIF":"INACTIF"', 'prefs.hasActiveConvoy()?"●  CONNEXION":"INACTIF"', 1)

needle='''        content.addView(settings);\n        if(prefs.hasActiveConvoy()){'''
replacement='''        content.addView(settings);\n\n        sectionLabel(content,"CONNEXION");\n        LinearLayout diagnostic=cardBox();\n        TextView diag=text("● Vérification du serveur…",13,true,muted); diagnostic.addView(diag);\n        TextView sync=text(lastSuccessfulSyncAt>0?"Dernière synchronisation : "+ageText(Math.max(0,System.currentTimeMillis()-lastSuccessfulSyncAt)):"Aucune synchronisation récente",12,false,muted); sync.setPadding(0,dp(4),0,dp(6)); diagnostic.addView(sync);\n        Button test=outlinedButton("↻   TESTER LA CONNEXION",accent); test.setOnClickListener(v->testServerConnectionDetailed(diag,sync)); diagnostic.addView(test);\n        content.addView(diagnostic);\n        testServerConnectionDetailed(diag,sync);\n\n        if(prefs.hasActiveConvoy()){'''
if needle in s: s=s.replace(needle,replacement,1)
else: raise SystemExit('more settings marker not found')

needle='''            convoy.addView(text("Code : "+prefs.get("code",""),13,false,muted));\n            Button qr=outlinedButton("▦   AFFICHER LE QR D’INVITATION",accent);'''
replacement='''            LinearLayout codeLine=new LinearLayout(this); codeLine.setGravity(Gravity.CENTER_VERTICAL);\n            codeLine.addView(text("Code : "+prefs.get("code",""),15,true,fg),new LinearLayout.LayoutParams(0,dp(44),1));\n            Button copy=smallButton("COPIER",card,accent); copy.setOnClickListener(v->copyConvoyCode()); codeLine.addView(copy,new LinearLayout.LayoutParams(dp(88),dp(40)));\n            convoy.addView(codeLine);\n            Button qr=outlinedButton("▦   AFFICHER LE QR D’INVITATION",accent);'''
if needle in s: s=s.replace(needle,replacement,1)
else: raise SystemExit('convoy code marker not found')

s=s.replace('cardTitle(content,"Mode Convoi 0.3.5",', 'cardTitle(content,"Mode Convoi 0.3.6",', 1)

old='''        new AlertDialog.Builder(this).setTitle("Rejoindre le convoi").setMessage("Scanner ce QR avec un autre téléphone ou utiliser le code.").setView(box).setPositiveButton("Fermer",null).show();'''
new='''        new AlertDialog.Builder(this).setTitle("QR d’invitation").setMessage("Sur l’autre téléphone : Mode Convoi → Scanner un QR. Le code reste disponible en secours.").setView(box).setNeutralButton("Copier le code",(d,w)->copyConvoyCode()).setPositiveButton("Fermer",null).show();'''
if old in s: s=s.replace(old,new,1)
else:
    old2='''        new AlertDialog.Builder(this).setTitle("QR d’invitation").setMessage("Sur l’autre téléphone : Mode Convoi → Scanner un QR. Le code reste disponible en secours.").setView(box).setPositiveButton("Fermer",null).show();'''
    if old2 in s: s=s.replace(old2,new,1)
    else: raise SystemExit('QR dialog marker not found')

s=re.sub(r'    private void shareConvoy\(\)\{.*?\n\n    private void sendStatus', '''    private void shareConvoy(){String code=prefs.get("code","");String msg="Rejoins mon convoi « "+prefs.get("convoyName","Mode Convoi")+" »\\nOuvre Mode Convoi puis scanne mon QR, ou saisis le code : "+code+"\\nLien : modeconvoi://join/"+code;Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,msg);startActivity(Intent.createChooser(i,"Partager le convoi"));}\n\n    private void copyConvoyCode(){String code=prefs.get("code","");if(code.isEmpty()){toast("Aucun convoi actif");return;}ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);cm.setPrimaryClip(ClipData.newPlainText("Code Mode Convoi",code));toast("Code "+code+" copié");}\n\n    private void sendStatus''', s, count=1, flags=re.S)

s=s.replace('''                    snapshot=s;processEvents();''', '''                    snapshot=s;processEvents(); consecutivePollFailures=0; lastSuccessfulSyncAt=System.currentTimeMillis();''',1)
s=s.replace('''                    if(connectionBadge!=null){connectionBadge.setText("● ACTIF");connectionBadge.setTextColor(Color.rgb(90,200,120));}\n                    if(polling)ui.postDelayed(this::pollOnce,3500);''', '''                    if(connectionBadge!=null){connectionBadge.setText("● CONNECTÉ");connectionBadge.setTextColor(Color.rgb(90,200,120));}\n                    if(polling)ui.postDelayed(this::pollOnce,3500);''',1)
s=s.replace('''                    if(connectionBadge!=null){connectionBadge.setText("● HORS LIGNE");connectionBadge.setTextColor(danger);}\n                    if(polling)ui.postDelayed(this::pollOnce,6000);''', '''                    consecutivePollFailures++;\n                    if(connectionBadge!=null){connectionBadge.setText(consecutivePollFailures<3?"● RECONNEXION":"● HORS LIGNE");connectionBadge.setTextColor(consecutivePollFailures<3?accent:danger);}\n                    long retry=Math.min(30000L,5000L*(1L<<Math.min(3,Math.max(0,consecutivePollFailures-1))));\n                    if(polling)ui.postDelayed(this::pollOnce,retry);''',1)

marker='    private String friendlyError(Throwable e){'
helper='''    private void testServerConnectionDetailed(TextView target,TextView sync){\n        target.setText("● Test en cours…"); target.setTextColor(muted);\n        io.execute(()->{\n            long started=System.currentTimeMillis();\n            try{\n                JSONObject h=ConvoyApi.get(prefs.get("serverUrl",DEFAULT_SERVER),"/health");\n                long latency=Math.max(1,System.currentTimeMillis()-started);\n                boolean ok=h.optBoolean("ok",false);\n                ui.post(()->{\n                    target.setText(ok?"● Serveur Mode Convoi disponible":"● Serveur Mode Convoi indisponible");\n                    target.setTextColor(ok?Color.rgb(90,200,120):danger);\n                    sync.setText(ok?"Réponse en "+latency+" ms":"Le serveur a répondu mais n’est pas prêt");\n                });\n            }catch(Exception e){\n                ui.post(()->{target.setText("● Connexion impossible");target.setTextColor(danger);sync.setText(friendlyError(e));});\n            }\n        });\n    }\n\n'''
if marker in s: s=s.replace(marker,helper+marker,1)
else: raise SystemExit('friendlyError marker not found')

main.write_text(s)
print('0.3.6 reliability/UX patch applied')
