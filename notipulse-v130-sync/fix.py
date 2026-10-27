from pathlib import Path
import sys

root = Path(sys.argv[1] if len(sys.argv)>1 else 'piiwii-notification-widget-v129')
j = root/'app/src/main/java/ch/piiwii/notificationwidget'
w = j/'WidgetProvider.java'
s = w.read_text(encoding='utf-8')

needle = '''      String selected=id,sourcePkg=pkg,sourceKey=key;
      PendingIntent sourceIntent=click;'''
replacement = '''      String selected=id,sourcePkg=pkg,sourceKey=key;
      PendingIntent sourceIntent=click;
      // Single-, double- and triple-slot widgets use one sorted active pool.
      // In ephemeral mode, an expired overlay never hides persistent alerts.
      if(powerCategory(c,battery)==null&&!activePreview&&
          (!ephemeral||EphemeralEvents.current(c)==null)){
        NotificationMonitor.Pick active=firstForWidget(c,widgetId);
        if(active!=null){selected=active.id;sourcePkg=active.packageName;
          sourceKey=active.key;sourceIntent=active.intent;}
      }'''
assert s.count(needle)==1,'Unexpected WidgetProvider singleton implementation'
s=s.replace(needle,replacement)

needle = '''    if(WidgetProfiles.rotating(c,widgetId)){
      ArrayList<NotificationMonitor.Pick> unique=new ArrayList<>();'''
replacement = '''    {
      ArrayList<NotificationMonitor.Pick> unique=new ArrayList<>();'''
assert s.count(needle)==1,'Unexpected multi-widget dedup implementation'
s=s.replace(needle,replacement)

needle = 'WidgetProfiles.rotating(c,widgetId)&&alerts.size()>available&&available>0'
replacement = 'WidgetProfiles.rotating(c,widgetId)&&alerts.size()>1&&available>0'
assert s.count(needle)==1,'Unexpected multi-slot rotation implementation'
s=s.replace(needle,replacement)

needle='"rest".equals(selected)?"Aucune alerte":'
replacement='"rest".equals(selected)?(NotificationMonitor.instance==null?"Accès aux notifications indisponible":"Aucune notification"):'
assert s.count(needle)==1,'Unexpected idle text'
s=s.replace(needle,replacement)
w.write_text(s,encoding='utf-8')

profiles=j/'WidgetProfiles.java';s=profiles.read_text(encoding='utf-8')
needle='return Categories.prefs(c).getLong(key(widgetId,"rotation_anchor"),0);'
assert s.count(needle)==1
s=s.replace(needle,'return Categories.prefs(c).getLong("shared_rotation_anchor",0);')
profiles.write_text(s,encoding='utf-8')

ui=j/'MainActivity.java';s=ui.read_text(encoding='utf-8')
s=s.replace('"Widget 1 : événement temporaire. Widgets 2/3 : la charge et les dernières notifications disposent de leurs propres cases."',
            '"Une alerte temporaire passe devant, puis les notifications encore actives restent visibles sur les trois widgets."')
s=s.replace('"Widget 1 : priorité classique. Widgets 2/3 : la recharge et les notifications restent visibles ensemble."',
            '"Les trois widgets partagent le même classement. La recharge garde sa priorité et chaque widget conserve ses filtres."')
s=s.replace('      editor.putLong(WidgetProfiles.key(widgetId,"rotation_anchor"),System.currentTimeMillis());',
            '      // Rotation commune : modifier un profil ne désynchronise plus les autres widgets.')
ui.write_text(s,encoding='utf-8')

build=root/'app/build.gradle';s=build.read_text(encoding='utf-8')
needle="versionCode 30; versionName '1.0.29'"
assert s.count(needle)==1,'Unexpected source version'
build.write_text(s.replace(needle,"versionCode 31; versionName '1.0.30'"),encoding='utf-8')
print('PASS NotiPulse 1.0.30: shared active alerts, dedup, synchronized rotation, versionCode 31')
