from pathlib import Path
import re

main=Path('mode-convoi-build/android/app/src/main/java/ch/piiwii/modeconvoi/MainActivity.java')
s=main.read_text()

# Friendly server status on the welcome screen.
needle='        content.addView(profile);\n\n        sectionLabel(content,"REJOINDRE");'
replacement='''        content.addView(profile);\n\n        LinearLayout serverCard=cardBox();\n        TextView serverTitle=text("●  Connexion Mode Convoi",14,true,muted);\n        serverCard.addView(serverTitle);\n        TextView serverSub=text("Vérification du serveur…",12,false,muted);\n        serverSub.setPadding(0,dp(3),0,0); serverCard.addView(serverSub);\n        content.addView(serverCard);\n        checkServerStatus(serverTitle,serverSub);\n\n        sectionLabel(content,"REJOINDRE");'''
if needle in s:
    s=s.replace(needle,replacement,1)

# Check backend before create/join so failures are immediate and understandable.
s=s.replace('''        runBusy("Création…",()->{\n            JSONObject participant=''', '''        runBusy("Création…",()->{\n            ConvoyApi.get(prefs.get("serverUrl",DEFAULT_SERVER),"/health");\n            JSONObject participant=''',1)
s=s.replace('''        runBusy("Connexion…",()->{\n            JSONObject body=''', '''        runBusy("Connexion…",()->{\n            ConvoyApi.get(prefs.get("serverUrl",DEFAULT_SERVER),"/health");\n            JSONObject body=''',1)

# Match the WordPress backend endpoint/event name.
s=s.replace('/general-stop",b,prefs.get("adminKey","")', '/emergency-stop",b,prefs.get("adminKey","")')
s=s.replace('e.optString("type").equals("general-stop")', 'e.optString("type").equals("emergency-stop")')

s=s.replace('new AlertDialog.Builder(this).setTitle("Rejoindre le convoi").setMessage("Scanner ce QR avec un autre téléphone ou utiliser le code.")', 'new AlertDialog.Builder(this).setTitle("QR d’invitation").setMessage("Sur l’autre téléphone : Mode Convoi → Scanner un QR. Le code reste disponible en secours.")')
s=s.replace('String msg="Rejoins mon convoi « "+prefs.get("convoyName","Mode Convoi")+" »\\nCode : "+code+"\\nLien : modeconvoi://join/"+code;', 'String msg="Rejoins mon convoi « "+prefs.get("convoyName","Mode Convoi")+" »\\nOuvre Mode Convoi puis scanne mon QR, ou saisis le code : "+code+"\\nLien : modeconvoi://join/"+code;')

old='private <T> void runBusy(String label,Throwing<T> work,Done<T> done){toast(label);io.execute(()->{try{T r=work.run();ui.post(()->done.accept(r));}catch(Exception e){ui.post(()->toast(e.getMessage()==null?"Erreur":e.getMessage()));}});}'
new='private <T> void runBusy(String label,Throwing<T> work,Done<T> done){toast(label);io.execute(()->{try{T r=work.run();ui.post(()->done.accept(r));}catch(Exception e){ui.post(()->toast(humanError(e)));}});}'
if old in s:
    s=s.replace(old,new,1)

marker='    private GradientDrawable roundBg('
helpers='''    private String humanError(Exception e){\n        if(e instanceof ConvoyApi.ApiException){\n            int c=((ConvoyApi.ApiException)e).statusCode;\n            if(c==404)return "Serveur Mode Convoi introuvable ou plugin non activé";\n            if(c==401)return "Session du convoi expirée";\n            if(c==403)return "Action réservée à l’administrateur";\n            if(c>=500)return "Serveur Mode Convoi temporairement indisponible";\n        }\n        String m=e.getMessage()==null?"":e.getMessage();\n        String l=m.toLowerCase(Locale.ROOT);\n        if(l.contains("unable to resolve host")||l.contains("unknownhost"))return "Pas de connexion internet";\n        if(l.contains("timed out")||l.contains("timeout"))return "Le serveur Mode Convoi ne répond pas";\n        if(l.contains("ssl")||l.contains("certificate"))return "Connexion sécurisée au serveur impossible";\n        if(l.contains("mode convoi"))return m;\n        return m.isEmpty()?"Connexion Mode Convoi impossible":m;\n    }\n\n    private void checkServerStatus(TextView title,TextView sub){\n        io.execute(()->{\n            try{\n                JSONObject h=ConvoyApi.get(prefs.get("serverUrl",DEFAULT_SERVER),"/health");\n                boolean ok=h.optBoolean("ok",false);\n                ui.post(()->{title.setText(ok?"●  Connexion Mode Convoi":"●  Serveur indisponible");title.setTextColor(ok?Color.rgb(91,196,62):danger);sub.setText(ok?"Serveur en ligne — prêt à créer ou rejoindre un convoi":"Le serveur ne répond pas");});\n            }catch(Exception e){\n                ui.post(()->{title.setText("●  Serveur indisponible");title.setTextColor(danger);sub.setText(humanError(e));});\n            }\n        });\n    }\n\n'''
if marker in s and 'private void checkServerStatus(' not in s:
    s=s.replace(marker,helpers+marker,1)

s=s.replace('Mode Convoi 0.3.4','Mode Convoi 0.3.5')
main.write_text(s)

# Make API errors robust when WordPress/proxy returns non-JSON HTML.
api=Path('mode-convoi-build/android/app/src/main/java/ch/piiwii/modeconvoi/ConvoyApi.java')
a=api.read_text()
a=a.replace('c.setRequestProperty("Content-Type", "application/json; charset=utf-8");', 'c.setRequestProperty("Content-Type", "application/json; charset=utf-8");\n        c.setRequestProperty("User-Agent", "ModeConvoi-Android/0.3.5");')
oldread='''        JSONObject o = sb.length() == 0 ? new JSONObject() : new JSONObject(sb.toString());\n        if (code < 200 || code >= 300) throw new ApiException(code, o.optString("error", "HTTP " + code));\n        return o;'''
newread='''        String raw=sb.toString().trim();\n        JSONObject o;\n        try { o = raw.isEmpty() ? new JSONObject() : new JSONObject(raw); }\n        catch (Exception parse) {\n            if(code==404) throw new ApiException(code,"Serveur Mode Convoi introuvable ou plugin non activé");\n            throw new ApiException(code,"Réponse invalide du serveur Mode Convoi");\n        }\n        if (code < 200 || code >= 300) throw new ApiException(code, o.optString("error", "HTTP " + code));\n        return o;'''
if oldread in a:
    a=a.replace(oldread,newread,1)
api.write_text(a)

svc=Path('mode-convoi-build/android/app/src/main/java/ch/piiwii/modeconvoi/LocationShareService.java')
x=svc.read_text().replace('type.equals("general-stop")','type.equals("emergency-stop")')
svc.write_text(x)

print('0.3.5 reliability patch applied')
