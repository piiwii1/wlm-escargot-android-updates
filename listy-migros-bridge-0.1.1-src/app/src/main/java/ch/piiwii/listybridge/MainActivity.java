package ch.piiwii.listybridge;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends Activity {
    private TextView status,last,diag;
    private android.widget.Button mainAction;
    private boolean openingMigros=false;

    @Override public void onCreate(Bundle b){super.onCreate(b);build();handleIntent(getIntent());refresh();}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);handleIntent(i);}
    @Override protected void onResume(){super.onResume();openingMigros=false;refresh();}

    private void build(){
        getWindow().setStatusBarColor(Ui.NAVY);
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.BG);
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(this,20),Ui.dp(this,28),Ui.dp(this,20),Ui.dp(this,32));

        box.addView(Ui.text(this,"ListY",30,Ui.NAVY,true));
        box.addView(Ui.text(this,"Connexion Migros · 0.1.5",17,Color.DKGRAY,true));
        addSpace(box,18);

        status=cardText("",17,Ui.NAVY,true);
        box.addView(status);

        addSpace(box,16);
        TextView help=cardText("Plus de code à copier. Ouvre ListY sur ce téléphone, touche « Migros / Cumulus » puis « Connecter Migros maintenant ». Une fois chez Migros, la synchronisation part automatiquement après la connexion.",14,Color.DKGRAY,false);
        box.addView(help);

        addSpace(box,16);
        mainAction=Ui.button(this,"Ouvrir Migros",true);
        mainAction.setOnClickListener(v->{
            if(BridgeConfig.hasPending(this)){finishPairing(true);return;}
            if(BridgeConfig.paired(this)){openMigros();return;}
            toast("Ouvre d’abord ListY sur ce téléphone et touche « Connecter Migros maintenant ». Rien à copier.");
        });
        box.addView(mainAction);

        last=Ui.text(this,"",14,Color.DKGRAY,false);
        box.addView(spaceParam(last,14));
        diag=Ui.text(this,"",12,Color.GRAY,false);
        box.addView(spaceParam(diag,8));

        addSpace(box,20);
        android.widget.Button reset=Ui.button(this,"Réinitialiser la connexion",false);
        reset.setOnClickListener(v->{
            if(!BridgeConfig.paired(this) && !BridgeConfig.hasPending(this)){toast("Aucune connexion à réinitialiser.");return;}
            BridgeConfig.clearAll(this);
            SecureStore.putPlain(this,"pair_status","");
            SecureStore.putPlain(this,"last_sync","");
            SecureStore.putPlain(this,"last_count","");
            SecureStore.putPlain(this,"last_diag","");
            toast("Connexion locale réinitialisée. Retourne dans ListY et touche Connecter Migros.");
            refresh();
        });
        box.addView(reset);

        scroll.addView(box);
        setContentView(scroll);
    }

    private void handleIntent(Intent i){
        Uri u=i==null?null:i.getData();
        if(u==null || !"listybridge".equals(u.getScheme())) return;
        String host=u.getHost()==null?"":u.getHost();
        if("pair".equals(host)){
            if(BridgeConfig.parseAndSave(this,u.toString())){
                SecureStore.putPlain(this,"pair_status","");
                finishPairing(true);
            } else {
                SecureStore.putPlain(this,"last_diag","La liaison envoyée par ListY est illisible.");
                toast("ListY n’a pas envoyé une liaison exploitable. Retourne dans ListY et retouche Connecter Migros.");
            }
            return;
        }
        if("sync".equals(host)){
            if(BridgeConfig.paired(this)) openMigros();
            else toast("La connexion ListY n’existe plus sur ce téléphone. Retourne dans ListY et touche Connecter Migros.");
        }
    }

    private void finishPairing(boolean autoOpen){
        if(!BridgeConfig.hasPending(this)){
            if(BridgeConfig.paired(this)){ if(autoOpen) openMigros(); else refresh(); }
            return;
        }
        status.setText("Connexion à ListY…");
        mainAction.setEnabled(false);
        new Thread(()->{
            try{
                BridgeHttp.redeemPair(this);
                BridgeHttp.ping(this);
                BridgeHttp.report(this,"listy_pair","ok","Téléphone relié automatiquement à ListY.",0);
                runOnUiThread(()->{
                    refresh();
                    toast("ListY relié automatiquement");
                    if(autoOpen) openMigros();
                });
            }catch(Exception e){
                SecureStore.putPlain(this,"last_diag",e.getMessage());
                runOnUiThread(()->{refresh();toast(e.getMessage());});
            }
        }).start();
    }

    private void openMigros(){
        if(openingMigros) return;
        if(!BridgeConfig.paired(this)){toast("Connexion ListY manquante.");return;}
        openingMigros=true;
        new Thread(()->BridgeHttp.report(this,"migros_start","info","Ouverture de Migros après liaison automatique.",0)).start();
        startActivity(new Intent(this,MigrosLoginActivity.class));
    }

    private void refresh(){
        boolean pending=BridgeConfig.hasPending(this);
        boolean paired=BridgeConfig.paired(this);
        String ps=SecureStore.getPlain(this,"pair_status");
        String t=SecureStore.getPlain(this,"last_sync");
        String cnt=SecureStore.getPlain(this,"last_count");
        String d=SecureStore.getPlain(this,"last_diag");

        if(pending) status.setText("Connexion automatique en cours…");
        else if(!paired) status.setText("Pas encore connecté à ListY");
        else if(!"ok".equals(ps)) status.setText("Vérification de ListY…");
        else if(t.isEmpty()) status.setText("✓ ListY relié · Migros à synchroniser");
        else status.setText("✓ Migros synchronisé avec ListY");

        mainAction.setEnabled(!pending);
        mainAction.setText(paired?"Ouvrir Migros et synchroniser":"Connexion depuis ListY requise");

        if(!t.isEmpty()){
            try{last.setText("Dernière synchro : "+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(Long.parseLong(t)))+" · "+cnt+" coupon(s)");}
            catch(Exception e){last.setText("");}
        } else last.setText(paired?"Aucune synchronisation Cumulus réussie pour le moment.":"Aucune liaison à configurer ici manuellement.");

        diag.setText(d.isEmpty()?"":"Diagnostic : "+d);
    }

    private TextView cardText(String value,float sp,int color,boolean bold){
        TextView t=Ui.text(this,value,sp,color,bold);
        t.setPadding(Ui.dp(this,16),Ui.dp(this,15),Ui.dp(this,16),Ui.dp(this,15));
        t.setBackground(Ui.rounded(Color.WHITE,16,this));
        return t;
    }
    private View spaceParam(View v,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=Ui.dp(this,top);v.setLayoutParams(p);return v;}
    private void addSpace(LinearLayout l,int dp){View v=new View(this);v.setLayoutParams(new LinearLayout.LayoutParams(1,Ui.dp(this,dp)));l.addView(v);}
    private void toast(String s){Toast.makeText(this,s==null?"":s,Toast.LENGTH_LONG).show();}
}
