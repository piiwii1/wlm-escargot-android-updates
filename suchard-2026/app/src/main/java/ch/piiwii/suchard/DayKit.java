package ch.piiwii.suchard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.widget.Toast;

final class DayKit {
 private static final String[] ITEMS={
  "Sandwichs et bouteille d'eau",
  "Veste de pluie et parapluie",
  "Téléphone chargé",
  "Chargeur pour la voiture",
  "Clés, permis et moyen de paiement",
  "Plein d'essence vérifié"
 };
 static int checkedCount(Activity a){
  SharedPreferences p=a.getSharedPreferences("suchard",0);
  int n=0;for(int i=0;i<ITEMS.length;i++)if(p.getBoolean("kit_"+i,false))n++;
  return n;
 }
 static void checklist(Activity a){
  SharedPreferences p=a.getSharedPreferences("suchard",0);
  boolean[] checked=new boolean[ITEMS.length];
  for(int i=0;i<ITEMS.length;i++)checked[i]=p.getBoolean("kit_"+i,false);
  new AlertDialog.Builder(a)
   .setTitle("🧳 Avant de partir · "+checkedCount(a)+"/"+ITEMS.length)
   .setMultiChoiceItems(ITEMS,checked,(dialog,which,isChecked)->{
     p.edit().putBoolean("kit_"+which,isChecked).apply();
   })
   .setPositiveButton("PRÊT !",(d,w)->Toast.makeText(a,
     "Checklist : "+checkedCount(a)+"/"+ITEMS.length+" éléments",Toast.LENGTH_SHORT).show())
   .setNegativeButton("FERMER",(d,w)->{})
   .show();
 }
 static void menu(Activity a){
  String[] items={
   "🧳 Checklist avant le départ ("+checkedCount(a)+"/"+ITEMS.length+")",
   "🖼 Affiche officielle du jubilé",
   "🚶 Retour à pied au parking du Seyon",
   "🚗 Itinéraire voiture vers le parking",
   "📤 Partager le programme de la journée",
   "🌐 Site officiel Suchard 2026"
  };
  new AlertDialog.Builder(a)
   .setTitle("Suchard · Infos pratiques")
   .setItems(items,(d,which)->{
    switch(which){
     case 0:checklist(a);break;
     case 1:a.startActivity(new Intent(a,PosterActivity.class));break;
     case 2:Gps.walkingToParking(a);break;
     case 3:Gps.start(a,0);break;
     case 4:shareProgram(a);break;
     case 5:
      try{a.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.suchard2026.ch/")));}
      catch(Exception ex){Toast.makeText(a,"Site non disponible",Toast.LENGTH_SHORT).show();}
      break;
    }
   })
   .setNegativeButton("FERMER",(d,w)->{})
   .show();
 }
 static void shareProgram(Activity a){
  StringBuilder b=new StringBuilder();
  b.append("🍫 SUCHARD 200 ANS · SAMEDI 17 OCTOBRE 2026\n");
  b.append("Départ de Sion · 12 étapes · retour en voiture\n\n");
  for(Route.Stop step:Route.ALL){
   b.append(step.time).append("  ").append(step.emoji).append(" ").append(step.title);
   b.append("\n").append(step.place);
   if(!step.destination.isEmpty())b.append("\nGPS : ").append(step.destination);
   b.append("\n\n");
  }
  b.append("https://www.suchard2026.ch/");
  Intent send=new Intent(Intent.ACTION_SEND);
  send.setType("text/plain");
  send.putExtra(Intent.EXTRA_SUBJECT,"Sortie Suchard 200 ans · Neuchâtel");
  send.putExtra(Intent.EXTRA_TEXT,b.toString());
  try{a.startActivity(Intent.createChooser(send,"Partager le programme"));}
  catch(Exception ex){Toast.makeText(a,"Partage indisponible",Toast.LENGTH_LONG).show();}
 }
 private DayKit(){}
}
