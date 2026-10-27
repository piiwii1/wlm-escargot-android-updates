from pathlib import Path
import sys
root=Path(sys.argv[1]); java=root/'app/src/main/java/ch/piiwii/notificationwidget'
def change(file,old,new):
    path=java/file if file.endswith('.java') else root/file
    s=path.read_text(encoding='utf8')
    assert s.count(old)==1,(file,s.count(old),old[:60])
    path.write_text(s.replace(old,new),encoding='utf8')

change('WidgetProfiles.java',
'''  /** Only a single-slot widget offers a visible rest pictogram. Default preserves the old preference. */
  static boolean showIdleIcon(Context c,int widgetId){
    return Categories.prefs(c).getBoolean(key(widgetId,"show_idle_icon"),Categories.enabled(c,"rest"));
  }''',
'''  /** All widget models may show a rest pictogram; each widget has its own preference. */
  static boolean showIdleIcon(Context c,int widgetId){
    return Categories.prefs(c).getBoolean(key(widgetId,"show_idle_icon"),true);
  }''')

change('WidgetProvider.java',
'''    List<NotificationMonitor.Pick> slots=MultiSlotPolicy.compose(capacity,charging,alerts);
    Bundle options=manager.getAppWidgetOptions(widgetId);''',
'''    List<NotificationMonitor.Pick> slots=MultiSlotPolicy.compose(capacity,charging,alerts);
    // One optional rest icon in the FIRST slot when no notifications exist.
    // Every other empty position remains invisible.
    if(slots.isEmpty()&&WidgetProfiles.showIdleIcon(c,widgetId))
      slots.add(new NotificationMonitor.Pick("rest",null,0,0,null,"idle"));
    Bundle options=manager.getAppWidgetOptions(widgetId);''')

change('MainActivity.java',
'''    final Switch[] idleChoice={null};
    if(WidgetProvider.slotsFor(this,widgetId)==1){
      sectionTitle(body,"Quand il n'y a aucune notification",
        "Choisis si ton widget simple affiche l'icône de repos ou reste totalement transparent.");
      Switch idleSwitch=new Switch(this);
      idleSwitch.setText("Afficher l'icône de repos");idleSwitch.setTextColor(white);
      idleSwitch.setChecked(draft.showIdleIcon);body.addView(idleSwitch,lp(12));
      idleSwitch.setOnCheckedChangeListener((buttonView,checked)->draft.showIdleIcon=checked);
      idleChoice[0]=idleSwitch;
    }else{
      addText(body,"Les emplacements sans notification restent toujours transparents, sans icône ni texte Libre.",12,muted,12);
    }''',
'''    sectionTitle(body,"Quand il n'y a aucune notification",
      "Afficher l'icône de repos dans la première case de ce widget. Les autres cases restent transparentes.");
    final Switch idleSwitch=new Switch(this);
    idleSwitch.setText("Afficher l'icône de repos");idleSwitch.setTextColor(white);
    idleSwitch.setChecked(draft.showIdleIcon);body.addView(idleSwitch,lp(12));
    idleSwitch.setOnCheckedChangeListener((buttonView,checked)->draft.showIdleIcon=checked);''')

change('MainActivity.java',
'''       draft.showIdleIcon=true;
       if(idleChoice[0]!=null)idleChoice[0].setChecked(true);''',
'''       draft.showIdleIcon=true;
       idleSwitch.setChecked(true);''')

change('MainActivity.java',
'''      if(WidgetProvider.slotsFor(this,widgetId)==1)
        editor.putBoolean(WidgetProfiles.key(widgetId,"show_idle_icon"),draft.showIdleIcon);''',
'''      editor.putBoolean(WidgetProfiles.key(widgetId,"show_idle_icon"),draft.showIdleIcon);''')

change('app/build.gradle',"versionCode 32; versionName '1.0.31'","versionCode 33; versionName '1.0.32'")
widget=(java/'WidgetProvider.java').read_text()
main=(java/'MainActivity.java').read_text()
assert 'slots.isEmpty()&&WidgetProfiles.showIdleIcon(c,widgetId)' in widget
assert 'remote.setViewVisibility(panel,View.INVISIBLE)' in widget
assert 'remote.setTextViewText(labels[index],"")' in widget
assert '"Libre"' not in widget
assert 'idleChoice' not in main
assert 'final Switch idleSwitch=new Switch(this);' in main
assert 'editor.putBoolean(WidgetProfiles.key(widgetId,"show_idle_icon"),draft.showIdleIcon);' in main
assert 'getBoolean(key(widgetId,"show_idle_icon"),true)' in (java/'WidgetProfiles.java').read_text()
print('PASS v1.0.32: selectable rest icon in slot 1 on all three widget sizes; remaining slots empty')
