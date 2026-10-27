from pathlib import Path
import sys
base=Path(sys.argv[1])
java=base/'app/src/main/java/ch/piiwii/notificationwidget'
def change(file,old,new):
    p=(java/file) if file.endswith('.java') else (base/file)
    txt=p.read_text()
    if txt.count(old)!=1:raise RuntimeError(str(p)+': expected 1 occurrence, got '+str(txt.count(old)))
    p.write_text(txt.replace(old,new))
change('NotificationMonitor.java',
'''    String recent=lastPackage.isEmpty()?"Aucune notification observée":
      (lastPackage+" → "+(lastClass.isEmpty()?"non reconnue":Categories.label(lastClass)));''',
'''    // A notification may have no recognized category. Never dereference a null one.
    String recent=NotificationDiagnosticPolicy.recent(lastPackage,lastClass,Categories.label(lastClass));''')
change('NotificationMonitor.java',
'''        lastPackage=s.getPackageName();
        lastClass=Categories.classify(this,s);''',
'''        lastPackage=s.getPackageName()==null?"":s.getPackageName();
        lastClass=Categories.classify(this,s);''')
(java/'NotificationDiagnosticPolicy.java').write_text('''package ch.piiwii.notificationwidget;
final class NotificationDiagnosticPolicy {
  private NotificationDiagnosticPolicy(){}
  static String recent(String pkg,String category,String categoryLabel){
    if(pkg==null||pkg.isEmpty())return "Aucune notification observée";
    String label=category==null||category.isEmpty()?"non reconnue":categoryLabel;
    if(label==null||label.isEmpty())label="non reconnue";
    return pkg+" → "+label;
  }
}
''')
change('MainActivity.java',
'''  void showTestsGuarded(){
    try{showTests();}
    catch(RuntimeException failure){''',
'''  void showTestsGuarded(){
    try{
      showTests();
      android.content.SharedPreferences p=Categories.prefs(this);
      String old=p.getString("last_runtime_error","");
      if(old!=null&&old.startsWith("Écran de tests"))
        p.edit().remove("last_runtime_error").putLong("last_tests_success",System.currentTimeMillis()).apply();
    }
    catch(RuntimeException failure){''')
change('MainActivity.java',
r'''    return "NotiPulse 1.0.37 — Diagnostic\n"+''',
r'''    String version="1.0.41";
    try{version=getPackageManager().getPackageInfo(getPackageName(),0).versionName;}
    catch(Exception ignored){}
    return "NotiPulse "+version+" — Diagnostic\n"+''')
change('MainActivity.java',
r'''"Pictogrammes vectoriels : "+GraphicIcons.ALL.length+"\n"+''',
r'''"Pictogrammes disponibles : "+GraphicIcons.ALL.length+"\n"+''')
change('GraphicIcons.java',
'''      try{Categories.prefs(c).edit().putString("last_pictogram_error",
          key+" ("+failure.getClass().getSimpleName()+")").apply();}catch(RuntimeException ignored){}
      return false;''',
'''      try{Categories.prefs(c).edit().putString("last_pictogram_error",
          key+" ("+failure.getClass().getSimpleName()+")").apply();}catch(RuntimeException ignored){}
      if("whatsapp".equals(key)){
        try{
          Drawable safe=c.getDrawable(R.drawable.pic_chat);
          if(safe!=null){
            safe=safe.mutate();safe.setTint(tint);
            safe.setBounds(Math.round(left),Math.round(top),Math.round(left+size),Math.round(top+size));
            safe.draw(canvas);return true;
          }
        }catch(RuntimeException ignored){}
      }
      return false;''')
change('app/build.gradle',"versionCode 41; versionName '1.0.40'","versionCode 42; versionName '1.0.41'")
(base/'tests/NotificationDiagnosticPolicyRegression.java').write_text('''package ch.piiwii.notificationwidget;
final class NotificationDiagnosticPolicyRegression {
  static int count=0;
  static void test(String actual,String expected){count++;if(!expected.equals(actual))throw new AssertionError(expected+" != "+actual);}
  public static void main(String[] args){
    test(NotificationDiagnosticPolicy.recent("",null,"Notification"),"Aucune notification observée");
    test(NotificationDiagnosticPolicy.recent(null,"sms","SMS"),"Aucune notification observée");
    test(NotificationDiagnosticPolicy.recent("com.whatsapp",null,"Notification"),"com.whatsapp → non reconnue");
    test(NotificationDiagnosticPolicy.recent("com.whatsapp","","Notification"),"com.whatsapp → non reconnue");
    test(NotificationDiagnosticPolicy.recent("com.whatsapp","whatsapp","WhatsApp"),"com.whatsapp → WhatsApp");
    test(NotificationDiagnosticPolicy.recent("com.whatsapp","whatsapp",null),"com.whatsapp → non reconnue");
    test(NotificationDiagnosticPolicy.recent("com.whatsapp","whatsapp",""),"com.whatsapp → non reconnue");
    System.out.println("PASS "+count+" null-safe diagnostic regression tests");
  }
}
''')
assert 'lastClass.isEmpty()' not in (java/'NotificationMonitor.java').read_text()
assert 'versionCode 42' in (base/'app/build.gradle').read_text()
print('PASS: test page crash fixed; automatic diagnostic version; Whatsapp fallback')
