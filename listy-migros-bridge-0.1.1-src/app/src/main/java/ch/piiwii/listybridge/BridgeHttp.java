package ch.piiwii.listybridge;

import android.content.Context;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class BridgeHttp {
    private BridgeHttp() {}

    public static JSONObject clientMeta() throws Exception {
        JSONObject c=new JSONObject();
        c.put("app","ListY Migros Bridge");
        c.put("app_version","0.1.3");
        c.put("version_code",4);
        c.put("device",Build.MANUFACTURER+" "+Build.MODEL);
        c.put("android",Build.VERSION.RELEASE);
        return c;
    }

    public static void redeemPair(Context c) throws Exception {
        if(!BridgeConfig.hasPending(c)) return;
        JSONObject b=new JSONObject();
        b.put("pair_id",BridgeConfig.pairId(c));
        b.put("code",BridgeConfig.pairCode(c));
        b.put("client",clientMeta());
        JSONObject r=postPublic(BridgeConfig.pairEndpoint(c),b);
        String token=r.optString("token","");
        String endpoint=r.optString("sync_endpoint","");
        if(!BridgeConfig.saveFinal(c,endpoint,token)) throw new Exception("ListY n’a pas renvoyé une liaison utilisable.");
        SecureStore.putPlain(c,"pair_status","ok");
        SecureStore.putPlain(c,"last_diag","Code temporaire accepté. Liaison ListY créée.");
    }

    public static void ping(Context c) throws Exception {
        JSONObject b=new JSONObject();
        b.put("client",clientMeta());
        post(BridgeConfig.pingEndpoint(c),BridgeConfig.token(c),b);
        SecureStore.putPlain(c,"pair_status","ok");
        SecureStore.putPlain(c,"last_diag","Liaison ListY vérifiée.");
    }

    public static void report(Context c,String stage,String status,String message,int httpStatus) {
        if(!BridgeConfig.paired(c)) return;
        try {
            JSONObject r=new JSONObject();
            r.put("stage",stage==null?"bridge":stage);
            r.put("status",status==null?"info":status);
            r.put("message",message==null?"":message);
            if(httpStatus>0) r.put("http_status",httpStatus);
            JSONObject b=new JSONObject();
            b.put("client",clientMeta());
            b.put("report",r);
            post(BridgeConfig.reportEndpoint(c),BridgeConfig.token(c),b);
            SecureStore.putPlain(c,"last_diag",message==null?"":message);
        } catch(Exception ignored) {}
    }

    public static int pushCoupons(Context c, JSONArray coupons) throws Exception {
        JSONObject b=new JSONObject();
        b.put("coupons",coupons);
        b.put("client",clientMeta());
        post(BridgeConfig.endpoint(c),BridgeConfig.token(c),b);
        SecureStore.putPlain(c,"last_sync",String.valueOf(System.currentTimeMillis()));
        SecureStore.putPlain(c,"last_count",String.valueOf(coupons.length()));
        SecureStore.putPlain(c,"last_diag","Synchronisation Cumulus réussie : "+coupons.length()+" coupon(s).");
        return coupons.length();
    }

    private static JSONObject post(String endpoint,String token,JSONObject body) throws Exception {
        if(endpoint==null||!endpoint.startsWith("https://")||token==null||!token.startsWith("LYM1.")) throw new Exception("Liaison ListY invalide.");
        return request(endpoint,body,token);
    }

    private static JSONObject postPublic(String endpoint,JSONObject body) throws Exception {
        if(endpoint==null||!endpoint.startsWith("https://")) throw new Exception("Adresse de liaison ListY invalide.");
        return request(endpoint,body,null);
    }

    private static JSONObject request(String endpoint,JSONObject body,String token) throws Exception {
        HttpURLConnection h=(HttpURLConnection)new URL(endpoint).openConnection();
        h.setConnectTimeout(15000);
        h.setReadTimeout(20000);
        h.setRequestMethod("POST");
        h.setDoOutput(true);
        h.setRequestProperty("Accept","application/json");
        h.setRequestProperty("Content-Type","application/json; charset=utf-8");
        if(token!=null&&!token.isEmpty()) h.setRequestProperty("X-ListY-Bridge-Key",token);
        byte[] raw=body.toString().getBytes(StandardCharsets.UTF_8);
        h.setFixedLengthStreamingMode(raw.length);
        try(OutputStream os=h.getOutputStream()){os.write(raw);}
        int code=h.getResponseCode();
        InputStream in=code>=400?h.getErrorStream():h.getInputStream();
        String text=read(in);
        h.disconnect();
        if(code<200||code>=300){
            String msg="";
            try{msg=new JSONObject(text).optString("message","");}catch(Exception ignored){}
            if(code==410) throw new Exception(msg.isEmpty()?"Ce code de liaison a expiré ou a déjà été utilisé. Retourne dans ListY et crée un nouveau code.":msg);
            if(code==401) throw new Exception(msg.isEmpty()?"Le code de liaison ne correspond plus. Retourne dans ListY et ouvre le dernier code affiché.":msg);
            throw new Exception("ListY a refusé la liaison (HTTP "+code+")"+(msg.isEmpty()?"":" : "+msg));
        }
        return text.isEmpty()?new JSONObject():new JSONObject(text);
    }

    private static String read(InputStream in)throws Exception{
        if(in==null)return"";
        StringBuilder s=new StringBuilder();
        try(BufferedReader b=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){
            String l;while((l=b.readLine())!=null)s.append(l);
        }
        return s.toString();
    }
}
