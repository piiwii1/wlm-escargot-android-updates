from pathlib import Path
root=Path("piiwii-notification-widget-v104")
p=root/"app/src/main/java/ch/piiwii/notificationwidget/MainActivity.java"
s=p.read_text(encoding="utf-8")
assert "WIDGET  ·  1.0.3" in s
s=s.replace("WIDGET  ·  1.0.3","WIDGET  ·  1.0.4")
assert '    LinearLayout state=card();screen.addView(state,lp(16));' in s
s=s.replace('    LinearLayout state=card();screen.addView(state,lp(16));',
  '    showDisplayModeCard();\n    LinearLayout state=card();screen.addView(state,lp(16));',1)
s=s.replace('    hero.addView(label("Icône affichée selon la priorité et l’état du téléphone",12,muted,false),lp(5));',
 '    hero.addView(label(EphemeralEvents.enabled(this)?"Dernière alerte temporaire":"Notification prioritaire",12,muted,false),lp(5));')
s=s.replace('    addText(state,"Branché : icône de charge. À 100 % : batterie pleine. Débranché : retour aux notifications actives.",13,muted,0);',
 '''    addText(state,EphemeralEvents.enabled(this)?
      "Branché : la charge reste en fond. Un nouveau message la masque quelques secondes, puis elle réapparaît.":
      "Branché : l'icône de charge reste prioritaire. À 100 % : batterie pleine. Débranché : retour aux alertes.",13,muted,0);''')
start=s.index("  String currentId(){")
end=s.index("  void refreshPowerLabel(){",start)
s=s[:start]+"""  String currentId(){
    WidgetProvider.BatteryState b=WidgetProvider.readBattery(this);
    if(EphemeralEvents.enabled(this)){
      String demo=Categories.prefs(this).getString("preview_id","");
      if(!demo.isEmpty()&&System.currentTimeMillis()<Categories.prefs(this).getLong("preview_until",0))return demo;
      EphemeralEvents.Item recent=EphemeralEvents.current(this);
      if(recent!=null)return recent.id;
    }
    if(b.plugged&&b.full&&Categories.enabled(this,"full"))return "full";
    if(b.plugged&&Categories.enabled(this,"charging"))return "charging";
    if(!EphemeralEvents.enabled(this)){
      NotificationMonitor.Pick pick=NotificationMonitor.best(this);
      if(pick!=null)return pick.id;
    }
    if(b.percent<=15&&Categories.enabled(this,"low"))return "low";
    return "rest";
  }
  void showDisplayModeCard(){
    LinearLayout modeCard=card();screen.addView(modeCard,lp(16));
    subtitle(modeCard,"Mode d'affichage","Choisis comment le widget affiche les nouvelles alertes.");
    LinearLayout choices=row();modeCard.addView(choices,lp(12));
    boolean ephemeral=EphemeralEvents.enabled(this);
    Button priority=button("🏆  Priorité",!ephemeral);
    Button latest=button("⏳  Éphémère",ephemeral);
    LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(0,d(52),1);
    a.rightMargin=d(6);
    choices.addView(priority,a);
    choices.addView(latest,new LinearLayout.LayoutParams(0,d(52),1));
    priority.setOnClickListener(v->changeDisplayMode("priority"));
    latest.setOnClickListener(v->changeDisplayMode("ephemeral"));
    addText(modeCard,ephemeral?
      "La dernière notification s'affiche pendant la durée réglée. Pendant la recharge, elle passe devant 🔌 puis la charge revient automatiquement jusqu'au débranchement.":
      "La notification la plus importante reste visible. Pendant la recharge, 🔌 ou 🔋 reste au-dessus de toutes les autres.",
      13,muted,ephemeral?13:0);
    if(ephemeral){
      int seconds=EphemeralEvents.duration(this);
      TextView number=label("Durée d'affichage : "+seconds+" secondes",15,white,true);
      modeCard.addView(number,lp(6));
      SeekBar slider=new SeekBar(this);
      slider.setMax(117);
      slider.setProgress(seconds-3);
      modeCard.addView(slider,lp(3));
      LinearLayout hints=row();modeCard.addView(hints,lp(4));
      hints.addView(label("3 s",11,muted,false),new LinearLayout.LayoutParams(0,-2,1));
      TextView max=label("120 s",11,muted,false);max.setGravity(Gravity.RIGHT);
      hints.addView(max,new LinearLayout.LayoutParams(0,-2,1));
      slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
        @Override public void onProgressChanged(SeekBar b,int x,boolean user){
          if(!user)return;
          int n=x+3;
          number.setText("Durée d'affichage : "+n+" secondes");
          Categories.prefs(MainActivity.this).edit().putInt(EphemeralEvents.SECONDS_KEY,n).apply();
        }
        @Override public void onStartTrackingTouch(SeekBar b){}
        @Override public void onStopTrackingTouch(SeekBar b){}
      });
    }
  }
  void changeDisplayMode(String mode){
    if(mode.equals(Categories.prefs(this).getString(EphemeralEvents.MODE_KEY,"priority")))return;
    Categories.prefs(this).edit().putString(EphemeralEvents.MODE_KEY,mode)
      .remove("preview_id").remove("preview_until").apply();
    EphemeralEvents.clear(this);
    WidgetProvider.refresh(this);
    renderScreen();
  }
"""+s[end:]
needle='    addText(screen,"Touchez une icône pour choisir un pictogramme ou une image. Touchez « Priorité » pour classer son importance.",13,muted,14);'
assert needle in s
s=s.replace(needle,'''    LinearLayout switcher=card();screen.addView(switcher,lp(14));
    addText(switcher,"Mode actuel : "+(EphemeralEvents.enabled(this)?"Éphémère":"Priorité"),15,blue,7);
    Button modeButton=button("Modifier le mode et la durée",false);
    switcher.addView(modeButton);
    modeButton.setOnClickListener(v->{tab=0;renderScreen();});
'''+needle,1)
s=s.replace('    addText(screen,"Chaque test dure environ 10 secondes et se termine automatiquement. Une recharge réelle reste prioritaire.",13,muted,15);',
 '''    addText(screen,EphemeralEvents.enabled(this)?
      "En mode éphémère, les tests utilisent la durée choisie. Pendant la charge, les tests s'affichent temporairement puis l'icône de charge revient.":
      "Chaque test dure 10 secondes et se termine automatiquement. La recharge réelle reste prioritaire.",13,muted,15);''')
old='''        Categories.prefs(this).edit().putString("preview_id",entry[0]).putLong("preview_until",System.currentTimeMillis()+10000L).apply();
        WidgetProvider.refresh(this);handler.postDelayed(()->WidgetProvider.refresh(this),10500);
        new AlertDialog.Builder(this).setMessage("Test « "+entry[1]+" » activé. Regarde ton widget sur l'écran d'accueil. La recharge réelle reste prioritaire.").setPositiveButton("OK",null).show();'''
assert old in s
s=s.replace(old,'''        int seconds=EphemeralEvents.enabled(this)?EphemeralEvents.duration(this):10;
        long milliseconds=seconds*1000L;
        Categories.prefs(this).edit().putString("preview_id",entry[0])
          .putLong("preview_until",System.currentTimeMillis()+milliseconds).apply();
        WidgetProvider.refresh(this);
        handler.postDelayed(()->WidgetProvider.refresh(this),milliseconds+300);
        new AlertDialog.Builder(this).setMessage("Test « "+entry[1]+" » activé pendant "+seconds+" secondes. Regarde ton widget.").setPositiveButton("OK",null).show();''')
p.write_text(s,encoding="utf-8")
gradle=root/"app/build.gradle"
g=gradle.read_text(encoding="utf-8")
assert "versionCode 4; versionName '1.0.3'" in g
gradle.write_text(g.replace("versionCode 4; versionName '1.0.3'","versionCode 5; versionName '1.0.4'"),encoding="utf-8")
readme=root/"README.md"
readme.write_text("""# PiiWii Notification Widget 1.0.4 (versionCode 5)
Deux modes: Priorité (comportement existant) ou Éphémère (dernière notification reconnue pendant 3 à 120 secondes).
En mode éphémère, la recharge ou batterie pleine reste un état permanent de fond : une nouvelle alerte passe devant et la recharge revient après expiration.
Durée et mode modifiables sur l'accueil, accès direct depuis Notifications. Préférences, widgets et images existants conservés.
Les restrictions Doze peuvent retarder l'expiration de l'affichage. Aucun contenu des messages n'est stocké.
""" + readme.read_text(encoding="utf-8"),encoding="utf-8")
print("1.0.4 user display controls applied")
