package ch.piiwii.notificationwidget;
import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.widget.*;
public class MainActivity extends Activity {
 private final int bg=Color.rgb(15,22,35),panel=Color.rgb(27,37,56),fg=Color.WHITE,muted=Color.rgb(174,192,219),accent=Color.rgb(108,177,255);
 LinearLayout body;
 TextView status;
 TextView chargeStatus;
 boolean powerReceiverRegistered=false;
 Handler handler=new Handler(Looper.getMainLooper());
 private final BroadcastReceiver powerUiReceiver=new BroadcastReceiver(){
   @Override public void onReceive(Context context,Intent intent){updateChargeStatus();}
 };
 @Override protected void onStart(){
   super.onStart();
   IntentFilter f=new IntentFilter();
   f.addAction(Intent.ACTION_POWER_CONNECTED);
   f.addAction(Intent.ACTION_POWER_DISCONNECTED);
   if(Build.VERSION.SDK_INT>=33)registerReceiver(powerUiReceiver,f,Context.RECEIVER_NOT_EXPORTED);
   else registerReceiver(powerUiReceiver,f);
   powerReceiverRegistered=true;
   updateChargeStatus();
 }
 @Override protected void onStop(){
   if(powerReceiverRegistered){unregisterReceiver(powerUiReceiver);powerReceiverRegistered=false;}
   super.onStop();
 }

 int dp(int x){return(int)(x*getResources().getDisplayMetrics().density+0.5f);}
 GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
 LinearLayout.LayoutParams lp(int bottom){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(bottom);return p;}
 TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
 Button button(String label){Button b=new Button(this);b.setAllCaps(false);b.setText(label);b.setTextColor(fg);b.setBackground(shape(Color.rgb(42,65,96),12));return b;}
 @Override public void onCreate(Bundle b){super.onCreate(b);show();}
 @Override protected void onResume(){super.onResume();if(body!=null){updateStatus();updateChargeStatus();WidgetProvider.refresh(this);}}
 void show(){
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(bg);
  body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(18),dp(26),dp(18),dp(28));scroll.addView(body);setContentView(scroll);
  body.addView(text("◉  PiiWii Notification Widget",24,fg,true),lp(4));
  body.addView(text("1×1 · transparent · 27 catégories · priorité intelligente",13,muted,false),lp(14));
  LinearLayout chargePanel=new LinearLayout(this);
  chargePanel.setOrientation(LinearLayout.VERTICAL);
  chargePanel.setPadding(dp(14),dp(12),dp(14),dp(12));
  chargePanel.setBackground(shape(panel,16));
  body.addView(chargePanel,lp(16));
  chargePanel.addView(text("⚡  RECHARGE PRIORITAIRE",16,accent,true),lp(5));
  chargeStatus=text("Vérification de la batterie…",14,fg,true);
  chargePanel.addView(chargeStatus,lp(7));
  chargePanel.addView(text("Pendant que le câble ou le chargeur sans fil alimente le téléphone, 🔌 s'affiche avant les messages, appels et alertes. Au débranchement, les notifications reprennent immédiatement leur priorité normale.",13,muted,false),lp(0));
  updateChargeStatus();
  body.addView(text("L’icône change quand une notification choisie arrive. Touchez le widget pour ouvrir la notification.",15,fg,false),lp(12));
  status=text("",14,accent,true);body.addView(status,lp(10));updateStatus();
  Button permission=button("Autoriser l’accès aux notifications");body.addView(permission,lp(10));
  permission.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
  Button pin=button("Ajouter le widget à l’écran d’accueil");body.addView(pin,lp(18));
  pin.setOnClickListener(v->{
   if(Build.VERSION.SDK_INT>=26){
    AppWidgetManager m=AppWidgetManager.getInstance(this);
    ComponentName component=new ComponentName(this,WidgetProvider.class);
    if(m.isRequestPinAppWidgetSupported()){m.requestPinAppWidget(component,null,null);return;}
   }
   new AlertDialog.Builder(this).setMessage("Appui long sur l’écran d’accueil, puis Widgets → PiiWii Notification Widget.").setPositiveButton("OK",null).show();
  });
  body.addView(text("MES 27 PICTOGRAMMES",17,fg,true),lp(8));
  body.addView(text("Touchez une icône pour la modifier. Décochez une catégorie pour l’ignorer.",13,muted,false),lp(12));
  for(String[] row:Categories.ALL)addRow(row);
  body.addView(text("TEST RAPIDE",17,fg,true),lp(8));
  body.addView(text("Le test WhatsApp reste secondaire si le téléphone est physiquement en charge.",12,muted,false),lp(8));
  Button test=button("Simuler WhatsApp pendant 10 secondes");body.addView(test,lp(10));
  test.setOnClickListener(v->{
   Categories.prefs(this).edit().putString("preview_id","whatsapp").putLong("preview_until",System.currentTimeMillis()+10000).apply();
   WidgetProvider.refresh(this);handler.postDelayed(()->WidgetProvider.refresh(this),10300);
  });
  body.addView(text("Les textes des notifications ne sont ni enregistrés ni envoyés. Les détails des notifications bancaires peuvent être masqués.",12,muted,false),lp(6));
 }
 void updateChargeStatus(){
   if(chargeStatus==null)return;
   boolean charging=WidgetProvider.isPhysicallyPluggedIn(this);
   chargeStatus.setText(charging
    ? "🔌 En charge : icône prioritaire active"
    : "⚪ Débranché : notifications normales");
 }
 void updateStatus(){
  if(status==null)return;
  String s=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");
  status.setText(s!=null&&s.contains(getPackageName())?"✓ Accès aux notifications autorisé":"⚠ Autorisation de notifications nécessaire");
 }
 void addRow(String[] row){
  String id=row[0];
  LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);r.setGravity(Gravity.CENTER_VERTICAL);
  r.setPadding(dp(9),dp(6),dp(8),dp(6));r.setBackground(shape(panel,12));body.addView(r,lp(6));
  Button icon=button(Categories.glyph(this,id));icon.setTextSize(22);
  LinearLayout.LayoutParams bi=new LinearLayout.LayoutParams(dp(60),dp(58));bi.rightMargin=dp(8);r.addView(icon,bi);
  icon.setOnClickListener(v->edit(id,icon));
  TextView label=text(row[1],14,fg,false);r.addView(label,new LinearLayout.LayoutParams(0,-2,1));
  CheckBox cb=new CheckBox(this);cb.setButtonTintList(ColorStateList.valueOf(accent));cb.setChecked(Categories.enabled(this,id));
  r.addView(cb,new LinearLayout.LayoutParams(dp(45),dp(54)));
  cb.setOnCheckedChangeListener((v,checked)->{Categories.prefs(this).edit().putBoolean("enabled_"+id,checked).apply();WidgetProvider.refresh(this);});
 }
 void edit(String id,Button target){
  EditText input=new EditText(this);input.setSingleLine(true);input.setText(Categories.glyph(this,id));input.setTextSize(28);input.setInputType(InputType.TYPE_CLASS_TEXT);
  LinearLayout shell=new LinearLayout(this);shell.setPadding(dp(20),dp(8),dp(20),0);shell.addView(input);
  new AlertDialog.Builder(this).setTitle("Pictogramme : "+Categories.label(id)).setMessage("Choisis un emoji ou un symbole Unicode.")
   .setView(shell).setPositiveButton("Enregistrer",(d,w)->{
    String icon=input.getText().toString().trim();
    if(!icon.isEmpty())Categories.prefs(this).edit().putString("icon_"+id,icon).apply();
    target.setText(Categories.glyph(this,id));WidgetProvider.refresh(this);
   }).setNeutralButton("Réinitialiser",(d,w)->{
    Categories.prefs(this).edit().remove("icon_"+id).apply();target.setText(Categories.glyph(this,id));WidgetProvider.refresh(this);
   }).setNegativeButton("Annuler",null).show();
 }
}