package ch.piiwii.mapsnavprobe;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private TextView accessStatus, instruction, detail, raw;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { refresh(); }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(16), dp(18), dp(16));
        root.setBackgroundColor(Color.rgb(244,246,248));
        root.addView(text("Google Maps · Widget Probe",25,true));
        TextView intro=text("Test 1.1.0 — consignes simplifiées, grosses flèches et priorité à la distance + manœuvre. Google Maps reste le GPS.",15,false);
        intro.setPadding(0,dp(6),0,dp(10)); root.addView(intro);
        accessStatus=text("",16,true); root.addView(accessStatus);
        Button permission=button("1 · Autoriser l’accès aux notifications"); permission.setOnClickListener(v->openNotificationAccess()); root.addView(permission);
        Button maps=button("2 · Ouvrir Google Maps"); maps.setOnClickListener(v->openMaps()); root.addView(maps);
        Button simulate=button("Tester : 100 m · Tournez à gauche"); simulate.setOnClickListener(v->simulate()); root.addView(simulate);
        TextView help=text("Ajoute le widget « Maps Nav Probe » sur l’écran d’accueil. Lance un vrai trajet Google Maps puis reviens à l’accueil.",14,false);
        help.setPadding(0,dp(6),0,dp(10)); root.addView(help);

        LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(14),dp(12),dp(14),dp(12)); card.setBackgroundColor(Color.WHITE);
        TextView cap=text("DERNIÈRE CONSIGNE SIMPLIFIÉE",13,true); cap.setTextColor(Color.rgb(40,120,65)); card.addView(cap);
        instruction=text("Aucune indication détectée",28,true); instruction.setPadding(0,dp(6),0,dp(3)); card.addView(instruction);
        detail=text("",16,false); card.addView(detail); root.addView(card);

        TextView rawTitle=text("Données brutes reçues de Google Maps",16,true); rawTitle.setPadding(0,dp(12),0,dp(4)); root.addView(rawTitle);
        ScrollView scroll=new ScrollView(this); raw=text("Aucune donnée.",13,false); raw.setTextIsSelectable(true); raw.setPadding(dp(10),dp(8),dp(10),dp(8)); raw.setBackgroundColor(Color.WHITE); scroll.addView(raw); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        Button clear=button("Effacer les données du test"); clear.setOnClickListener(v->{ getSharedPreferences(MapsNotificationListener.PREFS,MODE_PRIVATE).edit().clear().apply(); MapsNavWidget.updateAll(this); refresh(); }); root.addView(clear);
        setContentView(root);
    }

    @Override protected void onResume(){
        super.onResume();
        IntentFilter f=new IntentFilter(MapsNotificationListener.ACTION_UPDATE);
        if(Build.VERSION.SDK_INT>=33) registerReceiver(receiver,f,RECEIVER_NOT_EXPORTED); else registerReceiver(receiver,f);
        refresh();
    }

    @Override protected void onPause(){ super.onPause(); try{unregisterReceiver(receiver);}catch(Throwable ignored){} }

    private void refresh(){
        boolean enabled=listenerEnabled();
        accessStatus.setText(enabled?"✓ Accès aux notifications activé":"⚠ Accès aux notifications à activer");
        accessStatus.setTextColor(enabled?Color.rgb(25,135,65):Color.rgb(190,80,25));

        SharedPreferences p=getSharedPreferences(MapsNotificationListener.PREFS,MODE_PRIVATE);
        String arrow=p.getString("arrow","");
        String distance=p.getString("distance","");
        String primary=p.getString("primary","");
        String secondary=p.getString("secondary","");
        String stamp=p.getString("timestamp","");
        String rawText=p.getString("raw","");
        boolean simulated=p.getBoolean("simulated",false);

        if(TextUtils.isEmpty(primary)){
            instruction.setText("Aucune indication détectée");
            detail.setText("Lance un trajet Google Maps après avoir activé l’autorisation.");
        } else {
            StringBuilder main=new StringBuilder();
            main.append(TextUtils.isEmpty(arrow)?"↑":arrow).append("  ");
            if(!TextUtils.isEmpty(distance)) main.append(distance).append(" · ");
            main.append(primary);
            instruction.setText(main.toString());
            String line=(simulated?"[SIMULATION] ":"")+(TextUtils.isEmpty(secondary)?"Google Maps":secondary);
            if(!TextUtils.isEmpty(stamp)) line+="\nMise à jour : "+stamp;
            detail.setText(line);
        }
        raw.setText(TextUtils.isEmpty(rawText)?"Aucune donnée reçue de Google Maps.":rawText);
    }

    private boolean listenerEnabled(){
        String enabled=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");
        if(enabled==null)return false;
        ComponentName me=new ComponentName(this,MapsNotificationListener.class);
        return enabled.contains(me.flattenToString())||enabled.contains(getPackageName());
    }

    private void openNotificationAccess(){
        try{startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));}
        catch(Throwable t){startActivity(new Intent(Settings.ACTION_SETTINGS));}
    }

    private void openMaps(){
        try{
            Intent i=getPackageManager().getLaunchIntentForPackage(MapsNotificationListener.MAPS_PACKAGE);
            if(i!=null)startActivity(i); else startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com")));
        }catch(Throwable t){Toast.makeText(this,"Google Maps introuvable",Toast.LENGTH_LONG).show();}
    }

    private void simulate(){
        getSharedPreferences(MapsNotificationListener.PREFS,MODE_PRIVATE).edit()
                .putString("arrow","←")
                .putString("distance","100 m")
                .putString("primary","Tournez à gauche")
                .putString("secondary","Rue de Lausanne")
                .putString("timestamp",MapsNotificationListener.now())
                .putString("raw","SIMULATION\ntext=À 100 m, tournez à gauche sur Rue de Lausanne\n\nPARSED\narrow=←\ndistance=100 m\ninstruction=Tournez à gauche\nroad=Rue de Lausanne")
                .putBoolean("simulated",true).apply();
        MapsNavWidget.updateAll(this); refresh();
        Toast.makeText(this,"Simulation envoyée au widget",Toast.LENGTH_SHORT).show();
    }

    private TextView text(String value,float size,boolean bold){
        TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(Color.rgb(25,30,35));
        if(bold)t.setTypeface(t.getTypeface(),android.graphics.Typeface.BOLD); return t;
    }
    private Button button(String value){ Button b=new Button(this); b.setText(value); b.setAllCaps(false); return b; }
    private int dp(int value){ return Math.round(value*getResources().getDisplayMetrics().density); }
}
