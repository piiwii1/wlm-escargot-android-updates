package ch.piiwii.suchard;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

final class Tools {
 static void showDialog(Activity a,Runnable refresh){
  boolean enabled=a.getSharedPreferences("suchard",0).getBoolean("on",true);
  LinearLayout content=new LinearLayout(a);content.setOrientation(1);
  content.setPadding(MainActivity.dp(a,18),MainActivity.dp(a,8),MainActivity.dp(a,18),MainActivity.dp(a,6));
  content.addView(MainActivity.txt(a,"Samedi 17 octobre · heure suisse",14,MainActivity.BROWN,true));
  MainActivity.gap(a,content,9);
  content.addView(MainActivity.txt(a,"Notifications avant les déplacements. Le bouton « Y ALLER » ouvre directement le GPS, et « VOIR L’ÉTAPE » ramène au programme.",13,MainActivity.GRAY,false));
  MainActivity.gap(a,content,13);
  TextView accurate=MainActivity.btn(a,"⏱  AUTORISER LES ALARMES PRÉCISES",0xfff6eae4,MainActivity.BROWN);
  accurate.setTextSize(12);content.addView(accurate);
  accurate.setOnClickListener(v->{
   if(Build.VERSION.SDK_INT>=31){
    try{a.startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+a.getPackageName())));}
    catch(Exception e){Toast.makeText(a,"Ouvre les autorisations d'alarme Android.",Toast.LENGTH_LONG).show();}
   }else Toast.makeText(a,"Les alarmes précises sont déjà disponibles.",Toast.LENGTH_SHORT).show();
  });
  MainActivity.gap(a,content,8);
  content.addView(MainActivity.txt(a,"Sur Samsung, autorise aussi les notifications et l’activité en arrière-plan.",12,MainActivity.GRAY,false));
  AlertDialog dialog=new AlertDialog.Builder(a)
   .setTitle("🔔  Rappels de la journée")
   .setView(content)
   .setPositiveButton("TESTER",(d,w)->{
      if(Build.VERSION.SDK_INT>=33&&a.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
       a.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},23);
      else{Notify.show(a,0,true);Toast.makeText(a,"Notification de test envoyée",Toast.LENGTH_SHORT).show();}
   })
   .setNeutralButton(enabled?"DÉSACTIVER":"ACTIVER",(d,w)->{
      a.getSharedPreferences("suchard",0).edit().putBoolean("on",!enabled).apply();
      Notify.schedule(a);refresh.run();
   })
   .setNegativeButton("FERMER",(d,w)->{})
   .create();
  dialog.show();
 }
}
