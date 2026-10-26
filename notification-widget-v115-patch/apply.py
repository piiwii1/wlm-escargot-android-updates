from pathlib import Path
from xml.etree import ElementTree as ET
root=Path("piiwii-notification-widget-v115")
src=root/"app/src/main/java/ch/piiwii/notificationwidget"
def replace(file,old,new):
    p=src/file
    s=p.read_text(encoding="utf-8")
    assert s.count(old)==1,(file,old)
    p.write_text(s.replace(old,new,1),encoding="utf-8")
replace("WidgetProvider.java",
 'else if(battery.percent<=15&&Categories.enabled(c,"low"))id="low";',
 'else if(battery.percent>=0&&battery.percent<=15&&Categories.enabled(c,"low"))id="low";')
replace("NotificationMonitor.java",
 '    if(entries==null)return null;',
 '''    // A media session can exist even with zero posted notifications.
    if(entries==null){
      Pick spotify=SpotifyMedia.playingPick(c);
      return (spotify!=null&&WidgetProfiles.matches(filter,"spotify"))?spotify:null;
    }''')
replace("MainActivity.java",
 '    if(b.percent<=15&&Categories.enabled(this,"low"))return "low";',
 '    if(b.percent>=0&&b.percent<=15&&Categories.enabled(this,"low"))return "low";')
replace("MainActivity.java",'WIDGET  ·  1.0.14','WIDGET  ·  1.0.15')
report=r'''  /** On-device beta checklist: no message body, contact or banking text included. */
  String integrationReport(){
    android.content.SharedPreferences prefs=Categories.prefs(this);
    int count=AppWidgetManager.getInstance(this).getAppWidgetIds(
      new ComponentName(this,WidgetProvider.class)).length;
    return "PiiWii Notification Widget 1.0.15 — Diagnostic\n"+
      "Pages déclarées : "+tabs.length+"/4\n"+
      "Widgets installés : "+count+"\n"+
      "Catégories : "+Categories.ALL.length+"\n"+
      "Pictogrammes vectoriels : "+GraphicIcons.ALL.length+"\n"+
      "Mode : "+(EphemeralEvents.enabled(this)?"Éphémère":"Priorité")+"\n"+
      "Batterie : "+batterySummary()+"\n"+
      "Surveillance Android : "+(NotificationMonitor.instance!=null?"connectée":"déconnectée")+"\n"+
      "Spotify : "+SpotifyMedia.description(this)+"\n"+
      "Rafraîchissement : "+formattedAgo(prefs.getLong("last_widget_refresh",0))+"\n"+
      "Demande de reconnexion : "+formattedAgo(prefs.getLong("last_rebind_attempt",0))+"\n"+
      "Dernier toucher : "+prefs.getString("last_tap_result","aucun essai")+"\n\n"+
      "Tests à effectuer sur Samsung :\n"+
      "[ ] Accueil, Notifications, Apparence, Tests visibles\n"+
      "[ ] Brancher/débrancher le chargeur\n"+
      "[ ] Tester la batterie à 100 %\n"+
      "[ ] Recevoir appel et message privé\n"+
      "[ ] Spotify : lecture, pause, arrêt\n"+
      "[ ] Widget 1×1, 2×1 et 2×2\n"+
      "[ ] Toucher une vraie notification\n"+
      "[ ] Tester en veille et après redémarrage\n\n"+
      "Aucun texte privé de notification n'est inclus.";
  }
  void showIntegrationReport(){
    new AlertDialog.Builder(this).setTitle("Contrôle de la bêta — 1.0.15")
      .setMessage(integrationReport())
      .setPositiveButton("Partager",(dialog,which)->{
        Intent send=new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT,"Diagnostic PiiWii Notification Widget");
        send.putExtra(Intent.EXTRA_TEXT,integrationReport());
        startActivity(Intent.createChooser(send,"Partager le diagnostic"));
      })
      .setNegativeButton("Fermer",null).show();
  }
'''
replace("MainActivity.java",'  void showTests(){',report+'  void showTests(){')
replace("MainActivity.java",
 '    Button force=button("Actualiser le widget maintenant",false);',
 '''    Button checklist=button("Rapport et tests de la bêta",true);
    health.addView(checklist,lp(9));
    checklist.setOnClickListener(v->showIntegrationReport());
    Button force=button("Actualiser le widget maintenant",false);''')
gradle=root/"app/build.gradle"
s=gradle.read_text()
assert "versionCode 15; versionName '1.0.14'" in s
gradle.write_text(s.replace("versionCode 15; versionName '1.0.14'","versionCode 16; versionName '1.0.15'"))
readme=root/"README.md"
readme.write_text("# 1.0.15 — Beta candidate, not device-certified. Unknown battery fix, Spotify fallback, user-shared diagnostic.\n"+readme.read_text())
main=(src/"MainActivity.java").read_text()
monitor=(src/"NotificationMonitor.java").read_text()
provider=(src/"WidgetProvider.java").read_text()
for token in ("if(tab==0)showHome();","else if(tab==1)showCategories();","else if(tab==2)showAppearance();","else showTests();","root.addView(navSurface,navPosition)","void showIntegrationReport()"):
    assert token in main,token
assert "b.percent>=0&&b.percent<=15" in main
assert "battery.percent>=0&&battery.percent<=15" in provider
assert "if(entries==null){" in monitor and "SpotifyMedia.playingPick(c)" in monitor
assert len(list((root/"app/src/main/res/layout").glob("widget*.xml")))>=3
vectors=list((root/"app/src/main/res/drawable").glob("pic_*.xml"))
assert len(vectors)==24,len(vectors)
for xml in vectors: ET.parse(xml)
print("PASS: 4-page dispatch, low-battery sentinel, Spotify fallback, shareable report, 24 vector icons and 3 formats")
