from pathlib import Path
import re
p=Path('mode-convoi-build/android/app/src/main/java/ch/piiwii/modeconvoi/MainActivity.java')
out=p
s=p.read_text()

def must_replace(old,new,count=1,label='replace'):
    global s
    if old not in s:
        raise SystemExit(f'{label} not found')
    s=s.replace(old,new,count)

must_replace('import android.graphics.Typeface;','import android.graphics.Typeface;\nimport android.graphics.Bitmap;\nimport android.graphics.BitmapFactory;',label='graphics imports')
must_replace('import android.os.*;','import android.os.*;\nimport android.util.Base64;',label='base64 import')
must_replace('import java.net.URLEncoder;','import java.net.URLEncoder;\nimport java.io.ByteArrayOutputStream;\nimport java.io.InputStream;',label='io imports')
must_replace('private static final int REQ_LOCATION = 1001, REQ_NOTIF = 1002;','private static final int REQ_LOCATION = 1001, REQ_NOTIF = 1002, REQ_VEHICLE_IMAGE = 2001;',label='request code')
must_replace('private static final String EVENTS_CHANNEL = "convoy_events";','private static final String EVENTS_CHANNEL = "convoy_events";\n    private FrameLayout vehiclePreview;\n    private Dialog fullScreenMapDialog;\n    private WebView fullScreenMapView;\n    private boolean fullScreenMapReady=false;',label='fields')

old='EditText convoyName=profileInput(profile,"⚑","Nom du convoi","VW Suisse – Gênes"); content.addView(profile);'
new='EditText convoyName=profileInput(profile,"⚑","Nom du convoi","VW Suisse – Gênes"); Button vehicleLook=ghostButton("🚗   APPARENCE DU VÉHICULE"); vehicleLook.setOnClickListener(v->vehicleAppearanceDialog()); profile.addView(vehicleLook); content.addView(profile);'
must_replace(old,new,label='welcome vehicle button')

payload_old='.put("vehicleColor",prefs.get("profileColor",""));'
payload_new='.put("vehicleColor",prefs.get("profileColor","")).put("vehicleIcon",prefs.get("profileVehicleIcon","🚗")).put("vehicleMarkerColor",prefs.get("profileVehicleMarkerColor","#FFB514")).put("vehicleImage",prefs.get("profileVehicleImage",""));'
if s.count(payload_old)<2: raise SystemExit('payload markers missing')
s=s.replace(payload_old,payload_new,2)

old='TextView carIcon=text("🚗",25,false,stripeColor);carIcon.setGravity(Gravity.CENTER);carIcon.setBackground(roundBg(Color.rgb(31,34,37),stripeColor,22,1));info.addView(carIcon,new LinearLayout.LayoutParams(dp(48),dp(48)));'
new='View carIcon=participantAvatar(p,48,stripeColor);info.addView(carIcon,new LinearLayout.LayoutParams(dp(48),dp(48)));'
must_replace(old,new,label='position avatar')

old='TextView icon=text("🚗",18,false,state);icon.setGravity(Gravity.CENTER);icon.setBackground(roundBg(Color.rgb(32,35,38),state,19,1));row.addView(icon,new LinearLayout.LayoutParams(dp(40),dp(40)));'
new='View icon=participantAvatar(p,40,state);row.addView(icon,new LinearLayout.LayoutParams(dp(40),dp(40)));'
must_replace(old,new,label='participants avatar')

start=s.index('        LinearLayout gpsCard=cardBox();',s.index('    private void renderMapPage()'))
end=s.index('        mapPageReady=false;',start)
compact='''        LinearLayout gpsBar=cardBox(); gpsBar.setOrientation(LinearLayout.HORIZONTAL); gpsBar.setGravity(Gravity.CENTER_VERTICAL); gpsBar.setPadding(dp(10),dp(5),dp(8),dp(5));
        TextView pin=text("📍",18,false,gpsStatusGood()?Color.rgb(90,200,120):accent); pin.setGravity(Gravity.CENTER); gpsBar.addView(pin,new LinearLayout.LayoutParams(dp(34),dp(44)));
        LinearLayout gpsTexts=new LinearLayout(this); gpsTexts.setOrientation(LinearLayout.VERTICAL);
        TextView gpsState=text(gpsStatusCompactText(),12,true,gpsStatusGood()?Color.rgb(90,200,120):danger); gpsTexts.addView(gpsState);
        TextView serverState=text(gpsServerCompactText(),10,false,muted); gpsTexts.addView(serverState); gpsBar.addView(gpsTexts,new LinearLayout.LayoutParams(0,dp(44),1));
        Button gpsAction=smallButton(hasLocationPermission()?"↻":"ACTIVER",Color.rgb(31,34,37),hasLocationPermission()?muted:accent);
        gpsAction.setOnClickListener(v->{if(!hasLocationPermission())ensurePermissionsAndService();else{startShareService();toast("GPS relancé");ui.postDelayed(()->{gpsState.setText(gpsStatusCompactText());serverState.setText(gpsServerCompactText());pushMap();},1000);}});
        gpsBar.addView(gpsAction,new LinearLayout.LayoutParams(hasLocationPermission()?dp(48):dp(78),dp(36))); content.addView(gpsBar);

        LinearLayout mapActions=new LinearLayout(this); mapActions.setGravity(Gravity.CENTER_VERTICAL);
        TextView mapHint=text("Carte du convoi",12,true,muted); mapActions.addView(mapHint,new LinearLayout.LayoutParams(0,dp(38),1));
        Button full=smallButton("⛶  PLEIN ÉCRAN",Color.TRANSPARENT,accent); full.setBackground(roundBg(Color.TRANSPARENT,accent,10,1)); full.setOnClickListener(v->showFullScreenMap()); mapActions.addView(full,new LinearLayout.LayoutParams(dp(132),dp(36))); content.addView(mapActions);

'''
s=s[:start]+compact+s[end:]

anchor='''    private String gpsServerStatusText(){
        String err=prefs.get(LocationShareService.PREF_GPS_ERROR,"");
        long sent=prefs.getLong(LocationShareService.PREF_GPS_SENT_AT,0);
        if(!err.isEmpty()) return "Serveur : "+err;
        if(sent>0) return "Serveur : position envoyée "+ageText(Math.max(0,System.currentTimeMillis()-sent));
        return "Serveur : aucune position envoyée pour l’instant";
    }'''
helper=anchor+'''
    private String gpsStatusCompactText(){
        if(!hasLocationPermission()) return "Localisation désactivée";
        long at=prefs.getLong(LocationShareService.PREF_GPS_FIX_AT,0);
        if(at<=0) return "GPS en attente…";
        return "GPS actif · "+ageText(Math.max(0,System.currentTimeMillis()-at));
    }
    private String gpsServerCompactText(){
        String err=prefs.get(LocationShareService.PREF_GPS_ERROR,""); if(!err.isEmpty())return err;
        long sent=prefs.getLong(LocationShareService.PREF_GPS_SENT_AT,0);
        return sent>0?"Synchronisé · "+ageText(Math.max(0,System.currentTimeMillis()-sent)):"En attente de synchronisation";
    }'''
must_replace(anchor,helper,label='gps compact helpers')

needle='            long fixAt=prefs.getLong(LocationShareService.PREF_GPS_FIX_AT,0);'
rep='            if(me!=null){me.put("vehicleIcon",prefs.get("profileVehicleIcon","🚗"));me.put("vehicleMarkerColor",prefs.get("profileVehicleMarkerColor","#FFB514"));me.put("vehicleColor",prefs.get("profileColor",""));String img=prefs.get("profileVehicleImage","");if(!img.isEmpty())me.put("vehicleImage",img);}\n'+needle
must_replace(needle,rep,label='own map customization')

old='Button appearance=ghostButton("◐   APPARENCE"); appearance.setOnClickListener(v->themeDialog()); settings.addView(appearance);\n        Button advanced=ghostButton("⚙   PARAMÈTRES AVANCÉS");'
new='Button appearance=ghostButton("◐   APPARENCE"); appearance.setOnClickListener(v->themeDialog()); settings.addView(appearance);\n        Button myVehicle=ghostButton("🚗   MON VÉHICULE"); myVehicle.setOnClickListener(v->vehicleAppearanceDialog()); settings.addView(myVehicle);\n        Button advanced=ghostButton("⚙   PARAMÈTRES AVANCÉS");'
must_replace(old,new,label='more vehicle button')

must_replace('cardTitle(content,"Mode Convoi 0.3.8",','cardTitle(content,"Mode Convoi 0.3.9",',label='about version')

insert_at=s.index('    private void renderMorePage(){')
methods=r'''    private View participantAvatar(JSONObject p,int size,int fallbackColor){
        String image=p==null?prefs.get("profileVehicleImage",""):p.optString("vehicleImage","");
        if(image!=null&&!image.isEmpty()){
            try{byte[] raw=Base64.decode(image,Base64.DEFAULT);Bitmap b=BitmapFactory.decodeByteArray(raw,0,raw.length);if(b!=null){ImageView iv=new ImageView(this);iv.setScaleType(ImageView.ScaleType.CENTER_CROP);iv.setImageBitmap(b);iv.setBackground(roundBg(Color.rgb(31,34,37),participantMarkerColor(p,fallbackColor),size/2,1));iv.setClipToOutline(true);return iv;}}catch(Exception ignored){}
        }
        String icon=p==null?prefs.get("profileVehicleIcon","🚗"):p.optString("vehicleIcon","🚗");if(icon.isEmpty())icon="🚗";
        TextView v=text(icon,size>=46?24:19,false,participantMarkerColor(p,fallbackColor));v.setGravity(Gravity.CENTER);v.setBackground(roundBg(Color.rgb(31,34,37),participantMarkerColor(p,fallbackColor),size/2,1));return v;
    }
    private int participantMarkerColor(JSONObject p,int fallback){String c=p==null?prefs.get("profileVehicleMarkerColor",""):p.optString("vehicleMarkerColor","");try{if(c!=null&&!c.isEmpty())return Color.parseColor(c);}catch(Exception ignored){}return fallback;}
    private void vehicleAppearanceDialog(){
        ScrollView scroll=new ScrollView(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(8),dp(18),dp(14));scroll.addView(box);
        vehiclePreview=new FrameLayout(this);vehiclePreview.setPadding(dp(8),dp(8),dp(8),dp(8));box.addView(vehiclePreview,new LinearLayout.LayoutParams(-1,dp(100)));refreshVehiclePreview();
        TextView il=text("CHOISIS UNE ICÔNE",11,true,accent);il.setPadding(0,dp(8),0,dp(6));box.addView(il);
        GridLayout icons=new GridLayout(this);icons.setColumnCount(5);String[] all={"🚗","🚙","🏎️","🚐","🛻","🚕","🚓","🚘","🚖","🚚","🚌","🚜","🏁","⚡","★"};
        for(String ic:all){TextView b=text(ic,24,false,fg);b.setGravity(Gravity.CENTER);b.setBackground(roundBg(Color.rgb(31,34,37),Color.rgb(60,64,68),12,1));b.setOnClickListener(v->{prefs.put("profileVehicleIcon",ic);prefs.remove("profileVehicleImage");refreshVehiclePreview();});GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=dp(52);lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.setMargins(dp(3),dp(3),dp(3),dp(3));icons.addView(b,lp);}box.addView(icons,new LinearLayout.LayoutParams(-1,-2));
        TextView cl=text("COULEUR DU REPÈRE",11,true,accent);cl.setPadding(0,dp(12),0,dp(6));box.addView(cl);
        GridLayout colors=new GridLayout(this);colors.setColumnCount(8);String[] cs={"#FFB514","#EF4444","#3B82F6","#22C55E","#A855F7","#F97316","#E5E7EB","#111827"};
        for(String c:cs){TextView dot=text("●",25,false,Color.parseColor(c));dot.setGravity(Gravity.CENTER);dot.setOnClickListener(v->{prefs.put("profileVehicleMarkerColor",c);refreshVehiclePreview();});GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=dp(44);lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);colors.addView(dot,lp);}box.addView(colors,new LinearLayout.LayoutParams(-1,-2));
        Button photo=outlinedButton("▣   CHOISIR MA PROPRE IMAGE",accent);photo.setOnClickListener(v->pickVehicleImage());box.addView(photo);
        if(!prefs.get("profileVehicleImage","").isEmpty()){Button remove=ghostButton("RETIRER L’IMAGE PERSONNELLE");remove.setOnClickListener(v->{prefs.remove("profileVehicleImage");refreshVehiclePreview();});box.addView(remove);}
        AlertDialog dlg=new AlertDialog.Builder(this).setTitle("Mon véhicule").setView(scroll).setNegativeButton("Fermer",null).setPositiveButton("ENREGISTRER",(d,w)->{syncProfileToServer();if("participants".equals(currentPage))renderParticipantsPage();else if("map".equals(currentPage))pushMap();else if("home".equals(currentPage))refreshSnapshotArea();}).create();dlg.setOnDismissListener(d->vehiclePreview=null);dlg.show();
    }
    private void refreshVehiclePreview(){if(vehiclePreview==null)return;vehiclePreview.removeAllViews();View av=participantAvatar(null,72,accent);FrameLayout.LayoutParams ap=new FrameLayout.LayoutParams(dp(72),dp(72),Gravity.CENTER);vehiclePreview.addView(av,ap);}
    private void pickVehicleImage(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");startActivityForResult(i,REQ_VEHICLE_IMAGE);}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);if(requestCode!=REQ_VEHICLE_IMAGE||resultCode!=RESULT_OK||data==null||data.getData()==null)return;
        try(InputStream in=getContentResolver().openInputStream(data.getData())){Bitmap src=BitmapFactory.decodeStream(in);if(src==null){toast("Image illisible");return;}int w=src.getWidth(),h=src.getHeight(),side=Math.min(w,h);Bitmap crop=Bitmap.createBitmap(src,(w-side)/2,(h-side)/2,side,side);Bitmap small=Bitmap.createScaledBitmap(crop,96,96,true);ByteArrayOutputStream out=new ByteArrayOutputStream();small.compress(Bitmap.CompressFormat.JPEG,72,out);String b64=Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP);if(b64.length()>24000){toast("Image trop lourde");return;}prefs.put("profileVehicleImage",b64);refreshVehiclePreview();toast("Image du véhicule enregistrée");}catch(Exception e){toast("Impossible de lire cette image");}
    }
    private void syncProfileToServer(){if(!prefs.hasActiveConvoy())return;io.execute(()->{try{JSONObject b=authBody().put("name",prefs.get("profileName","Conducteur")).put("vehicle",prefs.get("profileVehicle","Véhicule")).put("vehicleColor",prefs.get("profileColor","")).put("vehicleIcon",prefs.get("profileVehicleIcon","🚗")).put("vehicleMarkerColor",prefs.get("profileVehicleMarkerColor","#FFB514")).put("vehicleImage",prefs.get("profileVehicleImage",""));ConvoyApi.post(prefs.get("serverUrl",""),"/api/convoys/"+prefs.get("code","")+"/profile",b,null);ui.post(()->{toast("Véhicule mis à jour");pollOnce();});}catch(Exception e){ui.post(()->toast("Profil local enregistré · serveur à mettre à jour"));}});}
    private void showFullScreenMap(){
        if(fullScreenMapDialog!=null&&fullScreenMapDialog.isShowing())return;final Dialog d=new Dialog(this,android.R.style.Theme_Black_NoTitleBar_Fullscreen);fullScreenMapDialog=d;FrameLayout frame=new FrameLayout(this);frame.setBackgroundColor(Color.rgb(10,12,14));
        WebView w=new WebView(this);fullScreenMapView=w;fullScreenMapReady=false;w.getSettings().setJavaScriptEnabled(true);w.getSettings().setDomStorageEnabled(true);w.getSettings().setLoadsImagesAutomatically(true);w.getSettings().setBlockNetworkImage(false);w.setBackgroundColor(bg);w.setWebViewClient(new android.webkit.WebViewClient(){@Override public void onPageFinished(WebView v,String url){fullScreenMapReady=true;pushFullScreenMap();scheduleFullScreenMapRefresh(d);}});frame.addView(w,new FrameLayout.LayoutParams(-1,-1));
        TextView close=text("✕",27,true,Color.WHITE);close.setGravity(Gravity.CENTER);close.setBackground(roundBg(Color.rgb(20,23,26),Color.rgb(90,94,98),22,1));close.setOnClickListener(v->d.dismiss());FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(dp(46),dp(46),Gravity.LEFT|Gravity.TOP);cp.setMargins(dp(12),dp(14),0,0);frame.addView(close,cp);
        TextView title=text("CARTE DU CONVOI",12,true,accent);title.setGravity(Gravity.CENTER);title.setBackground(roundBg(Color.rgb(20,23,26),Color.rgb(90,94,98),18,1));FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(dp(160),dp(40),Gravity.TOP|Gravity.CENTER_HORIZONTAL);tp.setMargins(0,dp(17),0,0);frame.addView(title,tp);
        d.setContentView(frame);d.setOnDismissListener(x->{fullScreenMapReady=false;fullScreenMapView=null;fullScreenMapDialog=null;});d.show();if(d.getWindow()!=null)d.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);w.loadUrl("file:///android_asset/convoy_map.html");
    }
    private void pushFullScreenMap(){if(fullScreenMapView==null||!fullScreenMapReady)return;JSONObject data=snapshotForMap();String raw=JSONObject.quote(data.toString());String id=JSONObject.quote(prefs.get("participantId",""));fullScreenMapView.evaluateJavascript("window.updateConvoy("+raw+","+id+")",null);}
    private void scheduleFullScreenMapRefresh(Dialog d){ui.postDelayed(new Runnable(){@Override public void run(){if(d==null||!d.isShowing()||fullScreenMapView==null)return;pushFullScreenMap();ui.postDelayed(this,2000);}},2000);}

'''
s=s[:insert_at]+methods+s[insert_at:]
out.write_text(s)
print('0.3.9 exact patch applied',len(s))