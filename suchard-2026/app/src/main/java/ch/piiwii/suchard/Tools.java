package ch.piiwii.suchard;
import android.app.*;import android.Manifest;import android.content.*;import android.content.pm.PackageManager;import android.graphics.Color;import android.net.Uri;import android.os.Build;import android.provider.Settings;import android.widget.*;
final class Tools {
 static void draw(Activity a,LinearLayout body,Runnable refresh){
   LinearLayout pane=new LinearLayout(a);pane.setOrientation(1);pane.setPadding(MainActivity.dp(a,15),MainActivity.dp(a,15),MainActivity.dp(a,15),MainActivity.dp(a,15));pane.setBackground(MainActivity.bg(a,Color.WHITE,16));body.addView(pane);
   pane.addView(MainActivity.txt(a,"🔔  RAPPELS DE DÉPART",15,MainActivity.BROWN,true));MainActivity.gap(a,pane,7);
   pane.addView(MainActivity.txt(a,"Alertes avec boutons « Y ALLER » et « VOIR L'ÉTAPE ». Rappels programmés pour le 17 octobre, heure suisse.",13,MainActivity.GRAY,false));MainActivity.gap(a,pane,12);
   boolean enabled=a.getSharedPreferences("suchard",0).getBoolean("on",true);
   TextView toggle=MainActivity.btn(a,enabled?"✓ NOTIFICATIONS ACTIVÉES":"ACTIVER LES NOTIFICATIONS",enabled?MainActivity.BROWN:MainActivity.PINK,Color.WHITE);
   pane.addView(toggle);toggle.setOnClickListener(v->{a.getSharedPreferences("suchard",0).edit().putBoolean("on",!enabled).apply();Notify.schedule(a);refresh.run();});
   MainActivity.gap(a,pane,8);LinearLayout row=new LinearLayout(a);pane.addView(row);
   TextView test=MainActivity.btn(a,"🔔 TESTER",0xfff4e9e4,MainActivity.BROWN);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,MainActivity.dp(a,42),1);p.rightMargin=MainActivity.dp(a,5);row.addView(test,p);
   test.setOnClickListener(v->{if(Build.VERSION.SDK_INT>=33&&a.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)a.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},23);else{Notify.show(a,0,true);Toast.makeText(a,"Notification de test envoyée",Toast.LENGTH_SHORT).show();}});
   TextView exact=MainActivity.btn(a,"⏱ PRÉCISION",0xfff4e9e4,MainActivity.BROWN);LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(0,MainActivity.dp(a,42),1);ep.leftMargin=MainActivity.dp(a,5);row.addView(exact,ep);
   exact.setOnClickListener(v->{if(Build.VERSION.SDK_INT>=31){try{a.startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+a.getPackageName())));}catch(Exception e){Toast.makeText(a,"Active les alarmes dans les paramètres Android.",Toast.LENGTH_LONG).show();}}else Toast.makeText(a,"Alarmes précises prises en charge.",Toast.LENGTH_SHORT).show();});
   MainActivity.gap(a,pane,9);
   pane.addView(MainActivity.txt(a,"Astuce : sur Samsung, autorise aussi l'activité en arrière-plan pour recevoir les rappels à l'heure.",12,MainActivity.GRAY,false));
 }
}
