from pathlib import Path
import re
project=Path("piiwii-notification-widget-v112")
src=project/"app/src/main/java/ch/piiwii/notificationwidget"
monitor=src/"NotificationMonitor.java"
s=monitor.read_text()
assert "final String id,packageName;" in s
s=s.replace("final String id,packageName;", "final String id,packageName,key;",1)
old="""      this.id=id;this.packageName=pkg;this.when=when;this.rank=rank;this.intent=intent;
    }"""
new="""      this(id,pkg,when,rank,intent,"");
    }
    Pick(String id,String pkg,long when,int rank,PendingIntent intent,String key){
      this.id=id;this.packageName=pkg;this.when=when;this.rank=rank;
      this.intent=intent;this.key=key==null?"":key;
    }"""
assert old in s
s=s.replace(old,new,1)
needle="best=new Pick(id,s.getPackageName(),s.getPostTime(),rank,n.contentIntent);"
assert needle in s
s=s.replace(needle,"best=new Pick(id,s.getPackageName(),s.getPostTime(),rank,n.contentIntent,s.getKey());")
monitor.write_text(s)
event=src/"EphemeralEvents.java"
s=event.read_text()
needle='return new Item(id,pref.getString(PACKAGE,""),NotificationMonitor.intentForKey(pref.getString(KEY,"")));'
assert needle in s
s=s.replace(needle,'String key=pref.getString(KEY,"");\n    return new Item(id,pref.getString(PACKAGE,""),NotificationMonitor.intentForKey(key),key);')
s=s.replace("final String id,packageName;","final String id,packageName,key;",1)
needle="Item(String id,String pkg,PendingIntent intent){this.id=id;packageName=pkg;this.intent=intent;}"
assert needle in s
s=s.replace(needle,"Item(String id,String pkg,PendingIntent intent,String key){this.id=id;packageName=pkg;this.intent=intent;this.key=key;}")
event.write_text(s)
widget=src/"WidgetProvider.java"
s=widget.read_text()
assert "String id=null,pkg=null;" in s
s=s.replace("String id=null,pkg=null;","String id=null,pkg=null,key=null;",1)
s=s.replace("id=event.id;pkg=event.packageName;click=event.intent;","id=event.id;pkg=event.packageName;key=event.key;click=event.intent;",1)
s=s.replace("if(pick!=null){id=pick.id;pkg=pick.packageName;click=pick.intent;}","if(pick!=null){id=pick.id;pkg=pick.packageName;key=pick.key;click=pick.intent;}",1)
s=s.replace('id="low";click=null;pkg=null;','id="low";click=null;pkg=null;key=null;',1)
assert 'if(click==null)click=launch(c,pkg);' in s
s=s.replace('if(click==null)click=launch(c,pkg);','click=WidgetClickActivity.create(c,id,pkg,key,click);',1)
widget.write_text(s)
manifest=project/"app/src/main/AndroidManifest.xml"
s=manifest.read_text()
needle='  <activity android:name=".MainActivity"'
assert needle in s
s=s.replace(needle,'''  <activity android:name=".WidgetClickActivity" android:exported="false"
    android:excludeFromRecents="true" android:noHistory="true"
    android:theme="@android:style/Theme.Translucent.NoTitleBar" />
  <activity android:name=".MainActivity"''',1)
manifest.write_text(s)
main=src/"MainActivity.java"
s=main.read_text().replace("WIDGET  ·  1.0.11","WIDGET  ·  1.0.12")
needle='    LinearLayout classifyCard=card();screen.addView(classifyCard,lp(14));'
assert needle in s
s=s.replace(needle,'''    LinearLayout clickCard=card();screen.addView(clickCard,lp(14));
    subtitle(clickCard,"Ouverture au toucher","Vérifie où le widget ouvre la dernière notification.");
    addText(clickCard,WidgetClickActivity.diagnostic(this),13,muted,10);
    Button clickCheck=button("Contrôler le dernier toucher",false);
    clickCard.addView(clickCheck);
    clickCheck.setOnClickListener(v->new AlertDialog.Builder(this)
      .setTitle("Diagnostic du toucher")
      .setMessage(WidgetClickActivity.diagnostic(this)+"\n\n"+
        "Si la notification a disparu, le widget essaie d'ouvrir son application.")
      .setPositiveButton("OK",null).show());
'''+needle,1)
# Correct escaping in the Java string: the Python text is parsed before insertion.
s=s.replace('WidgetClickActivity.diagnostic(this)+"\n\n"+','WidgetClickActivity.diagnostic(this)+"\\n\\n"+')
main.write_text(s)
gradle=project/"app/build.gradle"
s=gradle.read_text()
assert "versionCode 12; versionName '1.0.11'" in s
gradle.write_text(s.replace("versionCode 12; versionName '1.0.11'","versionCode 13; versionName '1.0.12'"))
readme=project/"README.md"
readme.write_text("# 1.0.12 — Toucher de widget robuste, fallback et diagnostic, quatre pages préservées.\n"+readme.read_text())
screen=s=main.read_text()
render=screen[screen.index("  private void renderScreen(){"):screen.index("  void showHome(){")]
for node in ("showHome();","showCategories();","showAppearance();","showTests();","root.addView(navSurface,navPosition)","setContentView(root);"):
    assert node in render,node
assert len(re.findall(r'void show(Home|Categories|Appearance|Tests)\(\)',screen))==4
print("PASS 1.0.12: 4 pages, click routing, manifest, diagnostic.")
