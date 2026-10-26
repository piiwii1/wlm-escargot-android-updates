from pathlib import Path
import re
root=Path('piiwii-notification-widget-v106')
main=root/'app/src/main/java/ch/piiwii/notificationwidget/MainActivity.java'
s=main.read_text(encoding='utf-8')
a=s.index('  private void renderScreen(){')
b=s.index('  void showHome(){',a)
part=s[a:b]
assert 'root.addView(navSurface,navPosition)' in part
assert 'setContentView(root);' in part
assert not re.search(r'if\s*\(\s*tab\s*==\s*0\s*\)\s*showHome\s*\(',part)
anchor='    // Place all interactive content outside the Android status and navigation bars.'
assert part.count(anchor)==1
dispatch='''    // Restore the page renderer accidentally removed in version 1.0.5.
    if(tab==0)showHome();
    else if(tab==1)showCategories();
    else if(tab==2)showAppearance();
    else showTests();
    addText(screen,"Sans publicité • Pas de connexion Internet • Réglages conservés après mise à jour",11,muted,0);

'''
s=s[:a]+part.replace(anchor,dispatch+anchor,1)+s[b:]
s=s.replace('WIDGET  ·  1.0.5','WIDGET  ·  1.0.6')
main.write_text(s,encoding='utf-8')
gradle=root/'app/build.gradle'
g=gradle.read_text(encoding='utf-8')
assert "versionCode 6; versionName '1.0.5'" in g
gradle.write_text(g.replace("versionCode 6; versionName '1.0.5'","versionCode 7; versionName '1.0.6'"),encoding='utf-8')
readme=root/'README.md'
readme.write_text('# 1.0.6: restore all four screen pages, preserve bottom bar and app settings.\n'+readme.read_text(encoding='utf-8'),encoding='utf-8')
content=s[s.index('  private void renderScreen(){'):s.index('  void showHome(){')]
for expected in ('if(tab==0)showHome();','else if(tab==1)showCategories();','else if(tab==2)showAppearance();','else showTests();','root.addView(scroll','root.addView(navSurface,navPosition)','root.setOnApplyWindowInsetsListener','setContentView(root);'):
    assert expected in content, 'UI regression: '+expected
print('PASS 1.0.6: all pages restored, beautiful 1.0.5 bottom bar retained')
