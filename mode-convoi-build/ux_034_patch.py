from pathlib import Path
import re

p=Path('mode-convoi-build/android/app/src/main/java/ch/piiwii/modeconvoi/MainActivity.java')
s=p.read_text()

# Public HTTPS backend: no local LAN IP in normal operation.
s=s.replace('public class MainActivity extends Activity {', 'public class MainActivity extends Activity {\n    private static final String DEFAULT_SERVER = "https://piiwii.ch/wp-json/mode-convoi/v1";', 1)

s=s.replace('prefs = new AppPrefs(this);\n        applyPalette();', '''prefs = new AppPrefs(this);
        String savedServer=prefs.get("serverUrl","");
        if(savedServer.isEmpty() || savedServer.startsWith("http://192.168.") || savedServer.startsWith("http://10.") || savedServer.startsWith("http://172.")) prefs.put("serverUrl",DEFAULT_SERVER);
        applyPalette();''', 1)

s=s.replace('EditText server=input("Serveur",prefs.get("serverUrl","http://192.168.1.25:8787"));', 'EditText server=input("Serveur",prefs.get("serverUrl",DEFAULT_SERVER));')

start=s.find('    private void advancedSettingsDialog(){')
if start != -1:
    end=s.find('\n    private void themeDialog(){', start)
    if end != -1:
        replacement='''    private void advancedSettingsDialog(){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(20),dp(8),dp(20),0);
        box.addView(text("Connexion Mode Convoi",17,true,fg));
        TextView help=text("Serveur internet sécurisé actif. Aucun réglage réseau n’est nécessaire.",13,false,muted); help.setPadding(0,dp(6),0,dp(10)); box.addView(help);
        new AlertDialog.Builder(this).setTitle("Connexion").setView(box).setNegativeButton("Fermer",null).setPositiveButton("Réinitialiser",(d,w)->{prefs.put("serverUrl",DEFAULT_SERVER);toast("Connexion Mode Convoi réinitialisée");}).show();
    }
'''
        s=s[:start]+replacement+s[end:]

s=s.replace('Mode Convoi 0.3.2','Mode Convoi 0.3.4')
s=s.replace('Mode Convoi 0.3.3','Mode Convoi 0.3.4')

p.write_text(s)
print('0.3.4 public-backend patch applied')
