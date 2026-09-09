package ch.piiwii.listybridge;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends Activity {
    private EditText pairing;
    private TextView status,last,diag;

    @Override public void onCreate(Bundle b){super.onCreate(b);build();handleIntent(getIntent());refresh();}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);handleIntent(i);}
    @Override protected void onResume(){super.onResume();refresh();}

    private void build(){
        getWindow().setStatusBarColor(Ui.NAVY);
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.BG);
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(this,20),Ui.dp(this,24),Ui.dp(this,20),Ui.dp(this,32));

        box.addView(Ui.text(this,"ListY",30,Ui.NAVY,true));
        box.addView(Ui.text(this,"Migros / Cumulus Bridge · 0.1.3",17,Color.DKGRAY,true));
        addSpace(box,18);

        status=cardText("",16,Ui.NAVY,true);
        box.addView(status);

        addSpace(box,16);
        box.addView(Ui.text(this,"1 · Relier à ListY",19,Ui.NAVY,true));
        box.addView(Ui.text(this,"Dans ListY → Migros / Cumulus, touche une fois « Relier mon téléphone », puis ouvre la liaison avec cette application. Le nouveau code temporaire reste valable 10 minutes et n’est plus invalidé si tu recliques par erreur.",14,Color.DKGRAY,false));
        addSpace(box,8);

        pairing=new EditText(this);
        pairing.setHint("listybridge://pair?...");
        pairing.setSingleLine(false);
        pairing.setMinLines(2);
        pairing.setTextSize(13);
        pairing.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        pairing.setPadding(Ui.dp(this,14),Ui.dp(this,12),Ui.dp(this,14),Ui.dp(this,12));
        pairing.setBackground(Ui.rounded(Color.WHITE,12,this));
        box.addView(pairing);

        android.widget.Button save=Ui.button(this,"Enregistrer / valider la liaison",false);
        save.setOnClickListener(v->{
            if(BridgeConfig.parseAndSave(this,pairing.getText().toString())){
                SecureStore.putPlain(this,"pair_status","");
                finishPairing();
            } else toast("Liaison illisible. Copie le lien complet affiché par ListY.");
        });
        box.addView(spaceParam(save,10));

        android.widget.Button test=Ui.button(this,"Tester la liaison ListY",false);
        test.setOnClickListener(v->{
            if(BridgeConfig.hasPending(this)){finishPairing();return;}
            if(!BridgeConfig.paired(this)){toast("Relie d’abord ListY Bridge.");return;}
            ping();
        });
        box.addView(spaceParam(test,8));

        addSpace(box,20);
        box.addView(Ui.text(this,"2 · Connecter Migros",19,Ui.NAVY,true));
        box.addView(Ui.text(this,"Tu te connectes dans la page officielle Migros. Le mot de passe, les cookies et le jeton Migros ne sont jamais envoyés à WordPress.",14,Color.DKGRAY,false));
        addSpace(box,10);

        android.widget.Button login=Ui.button(this,"Ouvrir Migros et synchroniser",true);
        login.setOnClickListener(v->{
            if(BridgeConfig.hasPending(this)){toast("La liaison ListY est encore en validation. Attends le message de confirmation.");finishPairing();return;}
            if(!BridgeConfig.paired(this)){toast("Relie d’abord ListY Bridge.");return;}
            new Thread(()->BridgeHttp.report(this,"migros_start","info","Ouverture de la connexion Migros sur le téléphone.",0)).start();
            startActivity(new Intent(this,MigrosLoginActivity.class));
        });
        box.addView(login);

        last=Ui.text(this,"",14,Color.DKGRAY,false);
        box.addView(spaceParam(last,12));

        diag=cardText("",13,Color.DKGRAY,false);
        box.addView(spaceParam(diag,12));

        addSpace(box,18);
        box.addView(Ui.text(this,"État : expérimental. Le pont n’est considéré prêt qu’après une première synchronisation Cumulus réussie.",12,Color.GRAY,false));

        scroll.addView(box);
        setContentView(scroll);
    }

    private TextView cardText(String value,float sp,int color,boolean bold){
        TextView t=Ui.text(this,value,sp,color,bold);
        t.setPadding(Ui.dp(this,16),Ui.dp(this,15),Ui.dp(this,16),Ui.dp(this,15));
        t.setBackground(Ui.rounded(Color.WHITE,16,this));
        return t;
    }

    private View spaceParam(View v,int top){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.topMargin=Ui.dp(this,top);
        v.setLayoutParams(p);
        return v;
    }
    private void addSpace(LinearLayout l,int dp){
        View v=new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1,Ui.dp(this,dp)));
        l.addView(v);
    }

    private void handleIntent(Intent i){
        Uri u=i==null?null:i.getData();
        if(u!=null&&"listybridge".equals(u.getScheme())){
            String s=u.toString();
            pairing.setText(s);
            if(BridgeConfig.parseAndSave(this,s)){
                SecureStore.putPlain(this,"pair_status","");
                finishPairing();
            } else toast("La liaison reçue est illisible. Retourne dans ListY et crée un nouveau code.");
        }
    }

    private void finishPairing(){
        if(BridgeConfig.hasPending(this)){
            status.setText("… Validation du code temporaire ListY");
            new Thread(()->{
                try{
                    BridgeHttp.redeemPair(this);
                    BridgeHttp.ping(this);
                    BridgeHttp.report(this,"listy_pair","ok","Téléphone relié à ListY. Il reste à synchroniser Migros/Cumulus.",0);
                    runOnUiThread(()->{toast("Liaison ListY créée et vérifiée");refresh();});
                }catch(Exception e){
                    SecureStore.putPlain(this,"last_diag",e.getMessage());
                    runOnUiThread(()->{toast(e.getMessage());refresh();});
                }
            }).start();
        } else if(BridgeConfig.paired(this)) {
            ping();
        } else {
            toast("Aucune liaison ListY exploitable.");
        }
    }

    private void ping(){
        status.setText("… Vérification de la liaison ListY");
        new Thread(()->{
            try{
                BridgeHttp.ping(this);
                BridgeHttp.report(this,"listy_pair","ok","Téléphone relié à ListY. Il reste à synchroniser Migros/Cumulus.",0);
                runOnUiThread(()->{toast("Liaison ListY reconnue");refresh();});
            }catch(Exception e){
                SecureStore.putPlain(this,"last_diag",e.getMessage());
                runOnUiThread(()->{toast(e.getMessage());refresh();});
            }
        }).start();
    }

    private void refresh(){
        boolean pending=BridgeConfig.hasPending(this);
        boolean paired=BridgeConfig.paired(this);
        String ps=SecureStore.getPlain(this,"pair_status");
        String t=SecureStore.getPlain(this,"last_sync");
        String cnt=SecureStore.getPlain(this,"last_count");
        String d=SecureStore.getPlain(this,"last_diag");

        if(pending) status.setText("◐ Étape 1/3 · Code temporaire reçu, validation en cours");
        else if(!paired) status.setText("○ Étape 1/3 · ListY n’est pas encore relié");
        else if(!"ok".equals(ps)) status.setText("○ Étape 1/3 · Liaison enregistrée, pas encore vérifiée");
        else if(t.isEmpty()) status.setText("◐ Étape 2/3 · ListY relié, Cumulus pas encore synchronisé");
        else status.setText("✓ Étape 3/3 · ListY + Cumulus synchronisés");

        if(!t.isEmpty()){
            try{
                last.setText("Dernière synchro : "+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(Long.parseLong(t)))+" · "+cnt+" coupon(s)");
            }catch(Exception e){last.setText("");}
        } else last.setText("Aucune synchronisation Cumulus réussie pour le moment.");

        diag.setText(d.isEmpty()?"Diagnostic : aucun test terminé pour le moment.":"Dernier diagnostic : "+d);
    }

    private void toast(String s){Toast.makeText(this,s==null?"":s,Toast.LENGTH_LONG).show();}
}
