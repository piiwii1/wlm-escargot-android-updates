from pathlib import Path
root=Path('piiwii-notification-widget-v105')
f=root/'app/src/main/java/ch/piiwii/notificationwidget/MainActivity.java'
s=f.read_text(encoding='utf-8')
old_start='    LinearLayout navbar=row();'
old_end='    // Android 15/16 enforce edge-to-edge with targetSdk 35.'
assert s.count(old_start)==1 and s.count(old_end)==1
start=s.index(old_start);end=s.index(old_end,start)
new_nav='''    // Modern, elevated bottom bar with accessible vector icons and active capsule.
    LinearLayout navSurface=row();
    navSurface.setPadding(d(7),d(7),d(7),d(7));
    GradientDrawable navBg=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
      new int[]{Color.rgb(28,39,60),Color.rgb(16,26,43)});
    navBg.setCornerRadius(d(25));
    navBg.setStroke(d(1),Color.rgb(57,79,112));
    navSurface.setBackground(navBg);
    navSurface.setElevation(d(12));
    LinearLayout.LayoutParams navPosition=new LinearLayout.LayoutParams(-1,d(78));
    navPosition.setMargins(d(12),d(4),d(12),d(10));
    root.addView(navSurface,navPosition);
    final int[] navImages={R.drawable.nav_home,R.drawable.nav_bell,R.drawable.nav_palette,R.drawable.nav_test};
    final String[] compactTitles={"Accueil","Notifs","Style","Tests"};
    for(int i=0;i<tabs.length;i++){
      final int destination=i;
      boolean selected=i==tab;
      LinearLayout item=column();
      item.setGravity(Gravity.CENTER);
      item.setFocusable(true);
      item.setClickable(true);
      item.setSelected(selected);
      item.setContentDescription(tabs[i]+(selected?", onglet actif":""));
      item.setMinimumHeight(d(60));
      if(selected){
        GradientDrawable active=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
          new int[]{Color.rgb(52,128,226),Color.rgb(28,85,176)});
        active.setCornerRadius(d(19));
        active.setStroke(d(1),Color.rgb(93,165,243));
        item.setBackground(active);
        item.setElevation(d(3));
      }
      ImageView itemIcon=new ImageView(this);
      itemIcon.setImageResource(navImages[i]);
      itemIcon.setImageTintList(ColorStateList.valueOf(selected?Color.WHITE:Color.rgb(161,177,200)));
      itemIcon.setContentDescription(null);
      LinearLayout.LayoutParams iconPosition=new LinearLayout.LayoutParams(d(24),d(24));
      iconPosition.gravity=Gravity.CENTER_HORIZONTAL;
      iconPosition.bottomMargin=d(4);
      item.addView(itemIcon,iconPosition);
      TextView title=label(compactTitles[i],11,selected?Color.WHITE:Color.rgb(166,181,202),selected);
      title.setGravity(Gravity.CENTER);
      title.setSingleLine(true);
      item.addView(title,new LinearLayout.LayoutParams(-1,d(18)));
      LinearLayout.LayoutParams width=new LinearLayout.LayoutParams(0,-1,1);
      width.setMargins(d(2),0,d(2),0);
      navSurface.addView(item,width);
      item.setOnClickListener(v->{if(tab!=destination){tab=destination;renderScreen();}});
    }

'''
s=s[:start]+new_nav+s[end:]
assert 'root.addView(navSurface,navPosition)' in s and 'root.addView(navbar' not in s
s=s.replace('WIDGET  ·  1.0.4','WIDGET  ·  1.0.5')
s=s.replace('  private final String[] tabIcons={"⌂","☷","◈","✓"};\n','')
f.write_text(s,encoding='utf-8')
icons={
'nav_home':('M3,10 L12,3 21,10 21,20 14,20 14,14 10,14 10,20 3,20 Z',),
'nav_bell':('M18,8 C18,4.7 15.3,2 12,2 C8.7,2 6,4.7 6,8 L6,12 C6,14.7 4.7,16 3,17 L21,17 C19.3,15 18,13.8 18,12 Z','M10,21 Q12,23 14,21'),
'nav_palette':('M12,3 C6.5,3 2,7.5 2,13 C2,18 6,22 11,22 C13.2,22 15,20.2 15,18 C15,17.2 14.7,16.4 14.1,15.9 C13.5,15.3 13.5,14.4 14.1,13.8 C14.5,13.4 15,13.3 15.5,13.3 L18,13.3 C20.2,13.3 22,11.5 22,9.3 C22,5.8 17.5,3 12,3 Z','M7.5,9.5 L7.51,9.5','M12,7 L12.01,7','M17,9 L17.01,9','M7.5,15 L7.51,15'),
'nav_test':('M9,3 L15,3','M10,3 L10,9 4.1,18.3 Q2.5,21 6,21 L18,21 Q21.5,21 19.9,18.3 L14,9 14,3','M7.5,16 L16.5,16')}
res=root/'app/src/main/res/drawable'
res.mkdir(parents=True,exist_ok=True)
for name,paths in icons.items():
  content='\n'.join(f'  <path android:pathData="{p}" android:fillColor="@android:color/transparent" android:strokeColor="#FFFFFFFF" android:strokeWidth="1.85" android:strokeLineCap="round" android:strokeLineJoin="round" />' for p in paths)
  xml=f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
 android:width="24dp" android:height="24dp"
 android:viewportWidth="24" android:viewportHeight="24">
{content}
</vector>
'''
  (res/(name+'.xml')).write_text(xml,encoding='utf-8')
gradle=root/'app/build.gradle'
s=gradle.read_text(encoding='utf-8')
assert "versionCode 5; versionName '1.0.4'" in s
gradle.write_text(s.replace("versionCode 5; versionName '1.0.4'","versionCode 6; versionName '1.0.5'"),encoding='utf-8')
readme=root/'README.md'
readme.write_text('# PiiWii Notification Widget 1.0.5\n\nModern floating bottom bar, native vector icons, blue gradient active state, larger targets, system-bar insets, previous features preserved.\n\n'+readme.read_text(encoding='utf-8'),encoding='utf-8')
print('Updated 1.0.5 navigation, icons and Gradle')
