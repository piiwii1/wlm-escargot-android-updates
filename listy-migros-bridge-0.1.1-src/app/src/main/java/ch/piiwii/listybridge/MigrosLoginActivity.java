package ch.piiwii.listybridge;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

public class MigrosLoginActivity extends Activity {
    private static final String REDIRECT="https://www.migros.ch/m-login-silent-login-redirect.html";
    private WebView web;
    private TextView status;
    private String pendingCode="",pendingState="";
    private boolean waitingHome=false;

    @Override public void onCreate(Bundle b){super.onCreate(b);build();}

    private void build(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        LinearLayout head=new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(Ui.dp(this,14),Ui.dp(this,12),Ui.dp(this,14),Ui.dp(this,10));
        head.setBackgroundColor(Ui.NAVY);
        head.addView(Ui.text(this,"Connexion Migros",20,Color.WHITE,true));
        status=Ui.text(this,"Connecte-toi normalement chez Migros, puis touche le bouton en bas.",12,Color.WHITE,false);
        head.addView(status);
        root.addView(head);

        web=new WebView(this);
        WebSettings s=web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web,true);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){
                String u=r.getUrl().toString();
                if(u.startsWith(REDIRECT)){captureRedirect(r.getUrl());return true;}
                return false;
            }
            @Override public void onPageFinished(WebView v,String url){
                if(waitingHome&&url.startsWith("https://www.migros.ch")){
                    waitingHome=false;
                    exchangeAndFetch();
                }
            }
        });
        root.addView(web,new LinearLayout.LayoutParams(-1,0,1));

        android.widget.Button done=Ui.button(this,"J’ai terminé → lire mes coupons",true);
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);
        bp.setMargins(Ui.dp(this,12),Ui.dp(this,8),Ui.dp(this,12),Ui.dp(this,12));
        done.setLayoutParams(bp);
        done.setOnClickListener(v->startSilentAuth());
        root.addView(done);
        setContentView(root);

        report("migros_page","info","Page officielle Migros ouverte.",0);
        web.loadUrl("https://www.migros.ch/fr/cumulus");
    }

    private void startSilentAuth(){
        if(web.getUrl()==null||!web.getUrl().startsWith("https://www.migros.ch")){
            setStatus("Reviens sur la page Migros après la connexion, puis réessaie.","migros_page","warning",0);
            web.loadUrl("https://www.migros.ch/fr/cumulus");
            return;
        }
        setStatus("Vérification de ta session Migros…","migros_auth","info",0);
        String js="(async()=>{try{const u='/authentication/public/v1/api/oauth/authorize?redirectUri='+encodeURIComponent('"+REDIRECT+"')+'&withLoginPrompt=false&claimType=LOGIN&authorizationNotRequired=true';const r=await fetch(u,{credentials:'include',headers:{Accept:'application/json, text/plain, */*'}});const txt=await r.text();let j={};try{j=JSON.parse(txt)}catch(_){return JSON.stringify({ok:false,status:r.status,error:'Réponse OAuth illisible'})}return JSON.stringify({ok:r.ok,status:r.status,url:j.url||'',error:j.message||''});}catch(e){return JSON.stringify({ok:false,status:0,error:String(e)})}})()";
        web.evaluateJavascript(js,val->{
            try{
                JSONObject j=new JSONObject(decodeJs(val));
                String url=j.optString("url","");
                int http=j.optInt("status",0);
                if(url.isEmpty()){
                    String msg=j.optString("error","");
                    if(msg.isEmpty()) msg="Session Migros introuvable. Vérifie que tu es bien connecté.";
                    setStatus(msg,"migros_auth","error",http);
                    return;
                }
                report("migros_auth","ok","Session Migros reconnue, récupération du jeton Cumulus.",http);
                web.loadUrl(url);
            }catch(Exception e){
                setStatus("Impossible de démarrer la liaison Migros : "+e.getMessage(),"migros_auth","error",0);
            }
        });
    }

    private void captureRedirect(Uri u){
        pendingCode=u.getQueryParameter("code");
        pendingState=u.getQueryParameter("state");
        if(pendingCode==null||pendingState==null){
            setStatus("Migros n’a pas renvoyé le code de session.","migros_auth","error",0);
            return;
        }
        report("migros_auth","ok","Code OAuth Migros reçu.",0);
        waitingHome=true;
        web.loadUrl("https://www.migros.ch/fr/cumulus");
    }

    private void exchangeAndFetch(){
        setStatus("Lecture de tes coupons Cumulus…","cumulus_read","info",0);
        String code=JSONObject.quote(pendingCode),state=JSONObject.quote(pendingState),redir=JSONObject.quote(REDIRECT);
        String js="(async()=>{try{const success='/authentication/public/v1/api/oauth/login-success?code='+encodeURIComponent("+code+")+'&state='+encodeURIComponent("+state+")+'&redirectUri='+encodeURIComponent("+redir+")+'&authorizationNotRequired=true';const sr=await fetch(success,{credentials:'include',headers:{Accept:'application/json'}});const st=await sr.text();let sj={};try{sj=JSON.parse(st)}catch(_){return JSON.stringify({ok:false,stage:'token',status:sr.status,error:'Réponse login-success illisible'})}let token=sj.accessToken||sj.access_token||sj.token||'';token=String(token).replace(/^Bearer\\s+/i,'');if(!token)return JSON.stringify({ok:false,stage:'token',status:sr.status,error:'Jeton Cumulus absent'});const cr=await fetch('/retentionapi/public/web/v1/cumulus-coupons',{credentials:'include',headers:{Authorization:'Bearer '+token,'migros-language':'fr','accept-language':'fr','peer-id':'website-js-1143.0.0',Accept:'application/json, text/plain, */*'}});const body=await cr.text();return JSON.stringify({ok:cr.ok,stage:'coupons',status:cr.status,body,error:cr.ok?'':body.slice(0,180)});}catch(e){return JSON.stringify({ok:false,stage:'network',status:0,error:String(e)})}})()";
        web.evaluateJavascript(js,val->{
            try{
                JSONObject r=new JSONObject(decodeJs(val));
                int http=r.optInt("status",0);
                if(!r.optBoolean("ok",false)){
                    String msg=r.optString("error","");
                    if(msg.length()>180)msg=msg.substring(0,180);
                    if(msg.isEmpty())msg="Migros refuse la lecture des coupons.";
                    setStatus("Migros refuse la lecture : "+msg,"cumulus_read","error",http);
                    return;
                }
                JSONArray normalized=CouponNormalizer.normalize(r.optString("body","[]"));
                report("cumulus_read","ok","Lecture Migros réussie : "+normalized.length()+" coupon(s) actif(s) renvoyé(s).",http);
                status.setText("Envoi de "+normalized.length()+" coupon(s) à ListY…");
                new Thread(()->{
                    try{
                        int n=BridgeHttp.pushCoupons(this,normalized);
                        BridgeHttp.report(this,"listy_sync","ok","Synchronisation terminée : "+n+" coupon(s) envoyé(s) à ListY.",200);
                        runOnUiThread(()->{
                            Toast.makeText(this,n+" coupon(s) synchronisé(s) dans ListY",Toast.LENGTH_LONG).show();
                            finish();
                        });
                    }catch(Exception e){
                        BridgeHttp.report(this,"listy_sync","error","Erreur d’envoi vers ListY : "+e.getMessage(),0);
                        runOnUiThread(()->status.setText("Erreur ListY : "+e.getMessage()));
                    }
                }).start();
            }catch(Exception e){
                setStatus("Réponse Migros illisible : "+e.getMessage(),"cumulus_read","error",0);
            }
        });
    }

    private void setStatus(String text,String stage,String level,int http){
        status.setText(text);
        report(stage,level,text,http);
    }

    private void report(String stage,String level,String message,int http){
        new Thread(()->BridgeHttp.report(this,stage,level,message,http)).start();
    }

    private static String decodeJs(String v)throws Exception{
        if(v==null||"null".equals(v))return"";
        return new JSONArray("["+v+"]").getString(0);
    }

    @Override public void onBackPressed(){
        if(web!=null&&web.canGoBack())web.goBack();
        else super.onBackPressed();
    }
}
