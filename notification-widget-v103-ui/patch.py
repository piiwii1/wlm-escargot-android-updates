from pathlib import Path
p=Path("piiwii-notification-widget-v103/app/src/main/java/ch/piiwii/notificationwidget/MainActivity.java")
s=p.read_text(encoding="utf-8")
s=s.replace("import android.view.Gravity;", "import android.view.Gravity;\nimport android.view.WindowInsets;\nimport android.graphics.Insets;")
s=s.replace("private void renderScreen(){", "private void renderScreen(){\n    powerText=null;preview=null;",1)
old="""    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(bg);
    screen=column();screen.setPadding(d(16),d(24),d(16),d(28));scroll.addView(screen);setContentView(scroll);"""
new="""    LinearLayout root=column();root.setBackgroundColor(bg);
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(bg);
    screen=column();screen.setPadding(d(16),d(16),d(16),d(28));scroll.addView(screen);
    root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));"""
assert old in s,"Original render layout differs"
s=s.replace(old,new,1)
s=s.replace('info.addView(label("WIDGET  ·  1.0.2",11,muted,true));','info.addView(label("WIDGET  ·  1.0.3",11,muted,true));')
oldnav="""    LinearLayout navbar=row();navbar.setBackground(shape(panel,15));navbar.setPadding(d(4),d(4),d(4),d(4));screen.addView(navbar,lp(20));"""
newnav="""    LinearLayout navbar=row();navbar.setBackground(shape(panel,15));navbar.setPadding(d(6),d(6),d(6),d(6));
    LinearLayout.LayoutParams navParams=new LinearLayout.LayoutParams(-1,d(72));
    navParams.setMargins(d(9),0,d(9),d(7));root.addView(navbar,navParams);"""
assert oldnav in s,"Old navbar missing"
s=s.replace(oldnav,newnav)
s=s.replace('TextView item=label(tabs[i],12,i==tab?white:muted,i==tab);','TextView item=label(new String[]{"⌂","☷","◈","✓"}[i]+"\\n"+tabs[i],11,i==tab?white:muted,i==tab);')
s=s.replace("navbar.addView(item,new LinearLayout.LayoutParams(0,d(42),1));","navbar.addView(item,new LinearLayout.LayoutParams(0,d(58),1));")
anchor='    addText(screen,"Sans publicité • Pas de connexion Internet • Réglages conservés après mise à jour",11,muted,0);'
safe="""    // Place all interactive content outside the Android status and navigation bars.
    getWindow().setStatusBarColor(Color.TRANSPARENT);
    getWindow().setNavigationBarColor(Color.TRANSPARENT);
    if(Build.VERSION.SDK_INT>=29)getWindow().setNavigationBarContrastEnforced(false);
    getWindow().getDecorView().setSystemUiVisibility(
      View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
      View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    root.setOnApplyWindowInsetsListener((v,insets)->{
      if(Build.VERSION.SDK_INT>=30){
        Insets bar=insets.getInsets(WindowInsets.Type.statusBars() |
          WindowInsets.Type.navigationBars() | WindowInsets.Type.displayCutout());
        root.setPadding(bar.left,bar.top,bar.right,bar.bottom);
      }else{
        root.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),
          insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
      }
      return insets;
    });
    setContentView(root);
    root.requestApplyInsets();"""
assert anchor in s
s=s.replace(anchor,anchor+"\n"+safe,1)
assert 'screen.addView(navbar' not in s and 'root.addView(navbar' in s
p.write_text(s,encoding="utf-8")
g=Path("piiwii-notification-widget-v103/app/build.gradle")
gradle=g.read_text()
assert "versionCode 3; versionName '1.0.2'" in gradle
g.write_text(gradle.replace("versionCode 3; versionName '1.0.2'","versionCode 4; versionName '1.0.3'"))
readme=Path("piiwii-notification-widget-v103/README.md")
readme.write_text("# 1.0.3: system safe areas and fixed bottom navigation.\n"+readme.read_text())
print("UI bottom navigation and Android system insets updated")
