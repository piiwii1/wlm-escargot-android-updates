from pathlib import Path
import sys
r=Path(sys.argv[1])
j=r/'app/src/main/java/ch/piiwii/notificationwidget'
def swap(name,a,b):
 p=(j/name) if name.endswith('.java') else r/name
 s=p.read_text()
 if s.count(a)!=1:raise ValueError(f'{name}: expected one occurrence of {a[:45]} found {s.count(a)}')
 p.write_text(s.replace(a,b))
swap('WidgetProfiles.java','  static String tapMode(Context c,int widgetId){','''  static boolean showIdleIcon(Context c,int widgetId){
    return Categories.prefs(c).getBoolean(key(widgetId,"show_idle_icon"),Categories.enabled(c,"rest"));
  }
  static String tapMode(Context c,int widgetId){''')
swap('WidgetProfiles.java','    edit.remove(key(widgetId,"tap_mode"));','''    edit.remove(key(widgetId,"tap_mode"));
    edit.remove(key(widgetId,"show_idle_icon"));''')
swap('WidgetProvider.java','''        image=IconRenderer.render(c,selected,size,widgetId);
        RemoteViews remote=new RemoteViews(c.getPackageName(),layout);
        remote.setImageViewBitmap(R.id.icon_image,image);
        boolean idleOff="rest".equals(selected)&&!Categories.enabled(c,"rest");
        remote.setViewVisibility(R.id.icon_image,idleOff?View.INVISIBLE:View.VISIBLE);
        if(variant!=WidgetVariantRules.COMPACT)
          remote.setTextViewText(R.id.icon_label,idleOff?"":("rest".equals(selected)?(NotificationMonitor.instance==null?"Accès aux notifications indisponible":"Aucune notification"):(reminded?"Rappel · ":"")+Categories.label(selected)));''','''        boolean idleOff="rest".equals(selected)&&!WidgetProfiles.showIdleIcon(c,widgetId);
        if(!idleOff)image=IconRenderer.render(c,selected,size,widgetId);
        RemoteViews remote=new RemoteViews(c.getPackageName(),layout);
        if(!idleOff)remote.setImageViewBitmap(R.id.icon_image,image);
        remote.setViewVisibility(R.id.icon_image,idleOff?View.INVISIBLE:View.VISIBLE);
        if(variant!=WidgetVariantRules.COMPACT){
          remote.setTextViewText(R.id.icon_label,idleOff?"":("rest".equals(selected)?(NotificationMonitor.instance==null?"Accès aux notifications indisponible":"Aucune notification"):(reminded?"Rappel · ":"")+Categories.label(selected)));
          remote.setViewVisibility(R.id.icon_label,idleOff?View.INVISIBLE:View.VISIBLE);
        }''')
swap('WidgetProvider.java','''    // Retain the user's selected idle icon even with zero active notifications.
    if(slots.isEmpty()&&Categories.enabled(c,"rest"))
      slots.add(new NotificationMonitor.Pick("rest",null,0,0,null,"idle"));
''','')
swap('WidgetProvider.java','''      remote.setViewVisibility(panel,View.VISIBLE);
      remote.setViewVisibility(labels[index],labelsVisible?View.VISIBLE:View.GONE);
      if(index>=slots.size()){
        boolean hidden=!Categories.enabled(c,"rest");
        remote.setViewVisibility(images[index],hidden?View.INVISIBLE:View.VISIBLE);
        remote.setImageViewResource(images[index],R.drawable.widget_slot_empty_icon);
        remote.setTextViewText(labels[index],hidden?"":"Libre");
        remote.setOnClickPendingIntent(panel,launch(c,null));
        continue;
      }
      NotificationMonitor.Pick item=slots.get(index);''','''      if(index>=slots.size()){
        remote.setViewVisibility(panel,View.INVISIBLE);
        remote.setViewVisibility(images[index],View.INVISIBLE);
        remote.setViewVisibility(labels[index],View.INVISIBLE);
        remote.setTextViewText(labels[index],"");
        continue;
      }
      remote.setViewVisibility(panel,View.VISIBLE);
      remote.setViewVisibility(images[index],View.VISIBLE);
      remote.setViewVisibility(labels[index],labelsVisible?View.VISIBLE:View.GONE);
      NotificationMonitor.Pick item=slots.get(index);''')
swap('MainActivity.java','''    boolean rotate;
    Set<String> selected;
    WidgetDraft(String category,String color,int scale,Set<String> selected,boolean rotate,int interval,String tapMode){''','''    boolean rotate, showIdleIcon;
    Set<String> selected;
    WidgetDraft(String category,String color,int scale,Set<String> selected,boolean rotate,int interval,String tapMode,boolean showIdleIcon){''')
swap('MainActivity.java','      this.rotate=rotate;this.interval=interval;this.tapMode=tapMode;','''      this.rotate=rotate;this.interval=interval;this.tapMode=tapMode;this.showIdleIcon=showIdleIcon;''')
swap('MainActivity.java','''       WidgetProfiles.rotating(this,widgetId),WidgetProfiles.rotationSeconds(this,widgetId),WidgetProfiles.tapMode(this,widgetId));''','''       WidgetProfiles.rotating(this,widgetId),WidgetProfiles.rotationSeconds(this,widgetId),
       WidgetProfiles.tapMode(this,widgetId),WidgetProfiles.showIdleIcon(this,widgetId));''')
swap('MainActivity.java','''    fill[0].run();
    addText(body,"",8,muted,7);
    sectionTitle(body,"Rotation des notifications",''','''    fill[0].run();
    final Switch[] idleChoice={null};
    if(WidgetProvider.slotsFor(this,widgetId)==1){
      sectionTitle(body,"Quand il n'y a aucune notification",
        "Affiche l'icône de repos ou laisse ton widget entièrement transparent.");
      Switch idleSwitch=new Switch(this);
      idleSwitch.setText("Afficher l'icône de repos");idleSwitch.setTextColor(white);
      idleSwitch.setChecked(draft.showIdleIcon);body.addView(idleSwitch,lp(12));
      idleSwitch.setOnCheckedChangeListener((buttonView,checked)->draft.showIdleIcon=checked);
      idleChoice[0]=idleSwitch;
    }else{
      addText(body,"Les emplacements sans notification sont toujours transparents.",12,muted,12);
    }
    addText(body,"",8,muted,7);
    sectionTitle(body,"Rotation des notifications",''')
swap('MainActivity.java','''       draft.rotate=true;draft.interval=60;draft.tapMode=TapModePolicy.NOTIFICATION;draft.selected.clear();for(String[] cat:Categories.ALL)draft.selected.add(cat[0]);''','''       draft.rotate=true;draft.interval=60;draft.tapMode=TapModePolicy.NOTIFICATION;
       draft.showIdleIcon=true;if(idleChoice[0]!=null)idleChoice[0].setChecked(true);
       draft.selected.clear();for(String[] cat:Categories.ALL)draft.selected.add(cat[0]);''')
swap('MainActivity.java','''      editor.putString(WidgetProfiles.key(widgetId,"tap_mode"),draft.tapMode);''','''      editor.putString(WidgetProfiles.key(widgetId,"tap_mode"),draft.tapMode);
      if(WidgetProvider.slotsFor(this,widgetId)==1)
        editor.putBoolean(WidgetProfiles.key(widgetId,"show_idle_icon"),draft.showIdleIcon);''')
swap('app/build.gradle',"versionCode 31; versionName '1.0.30'","versionCode 32; versionName '1.0.31'")
w=(j/'WidgetProvider.java').read_text()
assert '"Libre"' not in w and 'remote.setViewVisibility(panel,View.INVISIBLE)' in w
assert 'versionCode 32' in (r/'app/build.gradle').read_text()
print('NotiPulse 1.0.31: idle mode configurable for single widgets, empty multi-slots invisible')
