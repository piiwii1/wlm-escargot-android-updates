package ch.piiwii.notificationwidget;
import android.content.Context;
import android.content.SharedPreferences;
import android.app.Notification;
import android.service.notification.StatusBarNotification;
import java.util.Locale;
final class Categories {
 static final String[][] ALL={
 {"sms","SMS / RCS","💬"},{"whatsapp","WhatsApp","💚"},{"messenger","Messenger","🗨️"},{"call","Appel entrant","📞"},
 {"bank","Applications bancaires","🏦"},{"twint","TWINT","🪙"},{"paypal","PayPal","💰"},{"card","Paiement par carte","💳"},{"transfer","Virement / argent reçu","💸"},
 {"instagram","Instagram","📸"},{"facebook","Facebook","👍"},{"tiktok","TikTok","🎵"},
 {"gmail","Gmail","✉️"},{"outlook","Outlook","📨"},{"mail","Autres e-mails","📧"},
 {"calendar","Événements du calendrier","📅"},{"reminder","Rappels / tâches","📌"},{"alarm","Réveils","⏰"},{"timer","Minuteurs","⏱️"},{"birthday","Anniversaires","🎂"},
 {"parcel","Colis / La Poste","📦"},{"maps","Google Maps / Waze","🧭"},{"parking","Stationnement / parking","🅿️"},
 {"low","Batterie faible","🪫"},{"charging","En charge","🔌"},{"full","Batterie pleine","🔋"},{"spotify","Spotify / musique","🎧"}};
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("piiwii_notification_widget",0);}
 static String glyph(Context c,String id){String d="⚪";for(String[] a:ALL)if(a[0].equals(id))d=a[2];return prefs(c).getString("icon_"+id,d);}
 static String label(String id){for(String[] a:ALL)if(a[0].equals(id))return a[1];return id;}
 static boolean enabled(Context c,String id){return prefs(c).getBoolean("enabled_"+id,true);}
 static int priority(String id){
  switch(id){
  case "call":return 100;
  case "bank":case "twint":case "paypal":case "card":case "transfer":return 90;
  case "low":return 85;
  case "sms":case "whatsapp":case "messenger":return 80;
  case "alarm":case "timer":return 78;
  case "gmail":case "outlook":case "mail":return 70;
  case "instagram":case "facebook":case "tiktok":return 65;
  case "calendar":case "reminder":case "birthday":case "parcel":return 60;
  case "maps":case "parking":return 50;
  case "spotify":return 20;
  case "charging":case "full":return 10;
  default:return 0;}
 }
 static String classify(StatusBarNotification s){
  Notification n=s.getNotification();
  if((n.flags & Notification.FLAG_GROUP_SUMMARY)!=0)return null;
  String p=s.getPackageName().toLowerCase(Locale.ROOT);
  CharSequence title=n.extras.getCharSequence(Notification.EXTRA_TITLE),desc=n.extras.getCharSequence(Notification.EXTRA_TEXT);
  String m=((title==null?"":title)+" "+(desc==null?"":desc)).toLowerCase(Locale.ROOT);
  if(Notification.CATEGORY_CALL.equals(n.category)||p.contains("dialer")||p.contains("incallui"))return "call";
  if(p.contains("twint"))return "twint";
  if(p.contains("paypal"))return "paypal";
  if(has(p,"raiffeisen","postfinance","ubs","revolut","neon","yuh","bcvs","kantonalbank","migrosbank","yapeal")){
   if(has(m,"virement reçu","virement recu","argent reçu","argent recu","gutschrift","credited","incoming transfer"))return "transfer";
   if(has(m,"paiement par carte","carte utilisée","card payment","kartenzahlung"))return "card";
   return "bank";
  }
  if(p.startsWith("com.whatsapp"))return "whatsapp";
  if(p.contains("facebook.orca"))return "messenger";
  if(p.contains("instagram"))return "instagram";
  if(p.contains("facebook.katana"))return "facebook";
  if(has(p,"musically","tiktok"))return "tiktok";
  if(p.contains("google.android.gm"))return "gmail";
  if(p.contains("outlook"))return "outlook";
  if(has(p,"email","mail","proton"))return "mail";
  if(has(p,"messaging","sms"))return "sms";
  if(p.contains("spotify"))return "spotify";
  if(has(p,"waze","google.android.apps.maps"))return "maps";
  if(has(p,"parking","easypark","paybyphone"))return "parking";
  if(has(p,"post.ch","postapp","swisspost","dhl","dpd"))return "parcel";
  if(has(p,"deskclock","clockpackage"))return has(m,"minuteur","timer","countdown","compte à rebours")?"timer":"alarm";
  if(has(p,"calendar","splanner"))return has(m,"anniversaire","birthday","geburtstag")?"birthday":"calendar";
  if(has(p,"reminder","tasks","keep"))return "reminder";
  return null;
 }
 static boolean has(String str,String...terms){for(String t:terms)if(str.contains(t))return true;return false;}
}