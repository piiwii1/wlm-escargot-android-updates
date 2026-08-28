package ch.piiwii.roadbookgeneve;
import android.content.Context;import android.content.SharedPreferences;import java.time.*;import java.time.format.DateTimeFormatter;
public final class Itinerary{
 public static final String PREFS="roadbook_prefs"; public static final LocalDate DAY=LocalDate.of(2026,8,29); public static final ZoneId ZONE=ZoneId.of("Europe/Zurich");
 public static class Stop{public final String id,defaultTime,title,note,destination;public final boolean alertAtTime,fixedEvent;public final int alertBeforeMinutes;Stop(String i,String t,String ti,String n,String d,boolean a,int b,boolean f){id=i;defaultTime=t;title=ti;note=n;destination=d;alertAtTime=a;alertBeforeMinutes=b;fixedEvent=f;}}
 public static final Stop[] STOPS={
  new Stop("depart_sion","13:30","Départ de Sion","Pars vers 13h30. La marge de circulation est déjà intégrée au programme.","Château de Rolle, Grand-Rue 39, 1180 Rolle, Suisse",true,15,false),
  new Stop("rolle","14:55","Meeting Volkswagen — Château de Rolle","Environ 1h40–1h50 pour profiter du meeting sans courir.","Château de Rolle, Grand-Rue 39, 1180 Rolle, Suisse",false,0,false),
  new Stop("leave_rolle","16:50","Quitter Rolle pour Genève","À 16h35 l'app te prévient de finir ton tour. Départ conseillé à 16h50.","P+R Bachet-Praille, Genève, Suisse",true,15,false),
  new Stop("bachet","17:40","Parking Bachet-Praille","Plan pratique pour le Village du Soir. Ensuite quelques minutes à pied.","P+R Bachet-Praille, Genève, Suisse",false,0,false),
  new Stop("geneva_rp","18:00","Geneva RP / Geneva Vice — partie journée","Démos, découverte du projet et animations avant la partie club.","Village du Soir, Route des Jeunes 24, 1212 Lancy, Suisse",false,0,true),
  new Stop("leave_village","20:45","Quitter Geneva RP pour le centre","Marge volontaire pour circulation, parking et marche avant le spectacle.","Parking Rive Centre, Genève, Suisse",true,15,false),
  new Stop("garden","21:20","Être au Jardin Anglais / se placer","Tu as de l'avance pour choisir une bonne vue. Le Classique de 21h30 est un bonus.","Jardin anglais, Quai du Général-Guisan 34, 1204 Genève, Suisse",true,10,false),
  new Stop("rock","22:00","Rêve d'Eau — ROCK & POP","Objectif principal. Séance Rock & Pop de 22h00 à environ 22h20.","Jardin anglais, Quai du Général-Guisan 34, 1204 Genève, Suisse",true,15,true),
  new Stop("choice","22:20","Choix libre pour la suite","Selon la motivation: rester pour 22h30, rentrer à Sion, ou partir ailleurs.",null,true,0,false),
  new Stop("classic","22:30","Option — spectacle Classique","Si vous avez encore envie, restez pour la séance Classique de 22h30.","Jardin anglais, Quai du Général-Guisan 34, 1204 Genève, Suisse",false,0,true)};
 public static SharedPreferences prefs(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);} public static String getTime(Context c,Stop s){return prefs(c).getString("time_"+s.id,s.defaultTime);} public static void setTime(Context c,Stop s,int h,int m){prefs(c).edit().putString("time_"+s.id,String.format(java.util.Locale.ROOT,"%02d:%02d",h,m)).apply();}
 public static long getTimeMillis(Context c,Stop s){String[]p=getTime(c,s).split(":");return DAY.atTime(Integer.parseInt(p[0]),Integer.parseInt(p[1])).atZone(ZONE).toInstant().toEpochMilli();}
 private Itinerary(){}
}
