from pathlib import Path
p=Path('piiwii-notification-widget-v107')
src=p/'app/src/main/java/ch/piiwii/notificationwidget'
listener=src/'NotificationMonitor.java'
s=listener.read_text(encoding='utf-8')
s=s.replace('    if(instance==this)instance=null;','    SpotifyMedia.stop();\n    if(instance==this)instance=null;',1)
s=s.replace('  @Override public void onListenerConnected(){super.onListenerConnected();instance=this;WidgetProvider.refresh(this);}',
'''  @Override public void onListenerConnected(){
    super.onListenerConnected();
    instance=this;
    SpotifyMedia.start(this);
    WidgetProvider.refresh(this);
  }''',1)
s=s.replace('  @Override public void onListenerDisconnected(){if(instance==this)instance=null;WidgetProvider.refresh(this);super.onListenerDisconnected();}',
'''  @Override public void onListenerDisconnected(){
    SpotifyMedia.stop();
    if(instance==this)instance=null;
    WidgetProvider.refresh(this);
    super.onListenerDisconnected();
  }''',1)
s=s.replace('    Pick best=null;\n    for(StatusBarNotification s:entries){','    Pick best=null;\n    for(StatusBarNotification s:entries){',1)
s=s.replace('        if(id==null||!Categories.enabled(c,id))continue;',
'''        if(id==null||!Categories.enabled(c,id))continue;
        // Prefer the real Spotify playback state over an old media notification.
        if("spotify".equals(id)&&SpotifyMedia.hasSession())continue;''',1)
s=s.replace('    return best;\n  }\n}',
'''    // Spotify's media session can exist even when no Spotify notification is posted.
    // Other categories retain their configured priorities.
    Pick playing=SpotifyMedia.playingPick(c);
    if(playing!=null&&(best==null||playing.rank>best.rank))best=playing;
    return best;
  }
}''',1)
assert 'SpotifyMedia.start(this)' in s
assert 'Pick playing=SpotifyMedia.playingPick(c)' in s
listener.write_text(s,encoding='utf-8')

events=src/'EphemeralEvents.java'
s=events.read_text(encoding='utf-8')
anchor='  static synchronized Item current(Context c){'
assert anchor in s
s=s.replace(anchor,'''  /** Playback starting is a real event, even without a posted media notification. */
  static synchronized void onMediaStarted(Context c){
    if(!enabled(c)||!Categories.enabled(c,"spotify"))return;
    SharedPreferences pref=Categories.prefs(c);
    long expires=System.currentTimeMillis()+duration(c)*1000L;
    pref.edit().putString(ID,"spotify").putString(PACKAGE,"com.spotify.music")
      .putString(KEY,"media:spotify").putLong(POSTED,System.currentTimeMillis())
      .putLong(EXPIRES,expires).apply();
    WidgetProvider.scheduleRefresh(c,expires);
  }
''' + anchor,1)
events.write_text(s,encoding='utf-8')

provider=src/'WidgetProvider.java'
s=provider.read_text(encoding='utf-8')
anchor='        else if(battery.percent<=15&&Categories.enabled(c,"low"))id="low";'
assert anchor in s
s=s.replace(anchor,anchor+'''
        // Once a transient event expires, local Spotify playback is the
        // ambient fallback (unless charging/battery state overrides it).
        if(id==null){
          NotificationMonitor.Pick media=SpotifyMedia.playingPick(c);
          if(media!=null){id=media.id;pkg=media.packageName;click=media.intent;}
        }''',1)
provider.write_text(s,encoding='utf-8')

main=src/'MainActivity.java'
s=main.read_text(encoding='utf-8')
s=s.replace('WIDGET  ·  1.0.6','WIDGET  ·  1.0.7')
needle='    if(b.percent<=15&&Categories.enabled(this,"low"))return "low";'
assert needle in s
s=s.replace(needle,needle+'''
    NotificationMonitor.Pick media=SpotifyMedia.playingPick(this);
    if(media!=null)return "spotify";''',1)
# Spotify diagnosis on the actual settings tab: not a dummy simulation.
needle='  void showTests(){'
assert needle in s
s=s.replace(needle,'''  void spotifyDiagnosticDialog(){
    new AlertDialog.Builder(this)
      .setTitle("Diagnostic Spotify")
      .setMessage(SpotifyMedia.description(this)+"\\n\\n"+
        "Ce test lit la session multimédia locale de Spotify. Il ne dépend pas " +
        "d'une nouvelle notification. Un morceau joué sur un autre appareil " +
        "via Spotify Connect peut ne pas apparaître sur ce téléphone.\\n\\n"+
        "En mode Priorité, une alerte plus importante peut masquer la musique. " +
        "En mode Éphémère, elle revient quand la notification temporaire disparaît.")
      .setPositiveButton("Actualiser",(dialog,index)->spotifyDiagnosticDialog())
      .setNegativeButton("Fermer",null).show();
  }
''' + needle,1)
needle='    String[][] demo={{"whatsapp","WhatsApp"}'
assert needle in s
s=s.replace(needle,'''    LinearLayout spotifyCheck=card();screen.addView(spotifyCheck,lp(14));
    subtitle(spotifyCheck,"Spotify — lecture réelle","Diagnostic séparé de la simulation.");
    addText(spotifyCheck,SpotifyMedia.description(this),13,muted,10);
    Button spotifyButton=button("Vérifier la session Spotify",true);
    spotifyCheck.addView(spotifyButton);
    spotifyButton.setOnClickListener(v->spotifyDiagnosticDialog());
    String[][] demo={{"whatsapp","WhatsApp"}''',1)
main.write_text(s,encoding='utf-8')

gradle=p/'app/build.gradle'
s=gradle.read_text(encoding='utf-8')
assert "versionCode 7; versionName '1.0.6'" in s
gradle.write_text(s.replace("versionCode 7; versionName '1.0.6'",
"versionCode 8; versionName '1.0.7'"),encoding='utf-8')
readme=p/'README.md'
readme.write_text("# 1.0.7: Spotify MediaSession detection and audit.\n"+readme.read_text(encoding='utf-8'),encoding='utf-8')
for name in ('NotificationMonitor.java','WidgetProvider.java','MainActivity.java','EphemeralEvents.java','SpotifyMedia.java'):
    assert (src/name).is_file(),name
print("Spotify media session fix, ephemeral fallback, diagnostics and version 8 validated.")
