from pathlib import Path

root=Path('mode-convoi-build/android/app/src/main/java/ch/piiwii/modeconvoi')
svc=root/'LocationShareService.java'
s=svc.read_text()

if 'PREF_GPS_FIX_AT' not in s:
    s=s.replace('    private static final String EVENTS_CHANNEL = "convoy_events";\n', '    private static final String EVENTS_CHANNEL = "convoy_events";\n    public static final String PREF_GPS_LAT = "gpsLat";\n    public static final String PREF_GPS_LON = "gpsLon";\n    public static final String PREF_GPS_ACC = "gpsAccuracy";\n    public static final String PREF_GPS_FIX_AT = "gpsLastFixAt";\n    public static final String PREF_GPS_SENT_AT = "gpsLastSentAt";\n    public static final String PREF_GPS_ERROR = "gpsLastError";\n',1)

start=s.index('    private void startLocation() {')
end=s.index('\n    @Override public void onLocationChanged', start)
s=s[:start]+'''    private void startLocation() {
        if (!hasLocationPermission()) {
            prefs.put(PREF_GPS_ERROR, "Permission de localisation absente");
            return;
        }
        boolean anyProvider=false;
        try { if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){ lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 3, this); anyProvider=true; } } catch (Exception ignored) {}
        try { if(lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){ lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 5, this); anyProvider=true; } } catch (Exception ignored) {}
        if(!anyProvider) prefs.put(PREF_GPS_ERROR, "Localisation du téléphone désactivée");
        pushBestLastKnownLocation();
    }

    private boolean hasLocationPermission(){
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void pushBestLastKnownLocation(){
        if(!hasLocationPermission()) return;
        try{
            Location best=null;
            for(String provider:new String[]{LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER}){
                try{
                    Location x=lm.getLastKnownLocation(provider);
                    if(x!=null && (best==null || x.getTime()>best.getTime())) best=x;
                }catch(Exception ignored){}
            }
            if(best!=null && System.currentTimeMillis()-best.getTime()<10*60_000L) onLocationChanged(best);
        }catch(Exception ignored){}
    }
''' + s[end:]

start=s.index('    @Override public void onLocationChanged(Location loc) {')
end=s.index('\n    private void send(Location l)', start)
s=s[:start]+'''    @Override public void onLocationChanged(Location loc) {
        long t = System.currentTimeMillis();
        prefs.put(PREF_GPS_LAT, String.valueOf(loc.getLatitude()));
        prefs.put(PREF_GPS_LON, String.valueOf(loc.getLongitude()));
        prefs.put(PREF_GPS_ACC, String.valueOf(loc.hasAccuracy()?loc.getAccuracy():-1));
        prefs.putLong(PREF_GPS_FIX_AT, t);
        prefs.remove(PREF_GPS_ERROR);
        float speed = loc.hasSpeed() ? loc.getSpeed() : 0f;
        long interval = speed > 1.5f ? 5000 : 18000;
        if (lastLocation != null && loc.hasAccuracy() && lastLocation.hasAccuracy() && loc.getAccuracy() > 80 && lastLocation.getAccuracy() < loc.getAccuracy()) return;
        lastLocation = loc;
        if (t - lastSent < interval) return;
        lastSent = t;
        send(loc);
    }
''' + s[end:]

old='''                ConvoyApi.post(base, "/api/convoys/"+code+"/location", b, null);
            } catch (Exception ignored) {}
'''
new='''                ConvoyApi.post(base, "/api/convoys/"+code+"/location", b, null);
                prefs.putLong(PREF_GPS_SENT_AT, System.currentTimeMillis());
                prefs.remove(PREF_GPS_ERROR);
            } catch (Exception e) {
                String m=e.getMessage();
                prefs.put(PREF_GPS_ERROR, (m==null||m.trim().isEmpty())?"Envoi de position impossible":m.trim());
            }
'''
if old not in s: raise SystemExit('location send block not found')
s=s.replace(old,new,1)
svc.write_text(s)

main=root/'MainActivity.java'
m=main.read_text()
m=m.replace('@Override protected void onResume() { super.onResume(); if (prefs.hasActiveConvoy()) startPolling(); }', '@Override protected void onResume() { super.onResume(); if (prefs.hasActiveConvoy()) { startPolling(); startShareServiceIfPermitted(); } }',1)

start=m.index('    private void ensurePermissionsAndService(){')
end=m.index('\n    private void startPolling()', start)
m=m[:start]+'''    private boolean hasLocationPermission(){
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;
    }
    private void ensurePermissionsAndService(){
        if(!hasLocationPermission()){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},REQ_LOCATION);
            return;
        }
        startShareService();
        requestNotificationPermissionIfNeeded();
    }
    private void requestNotificationPermissionIfNeeded(){
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQ_NOTIF);
    }
    private void startShareServiceIfPermitted(){ if(hasLocationPermission()) startShareService(); }
    @Override public void onRequestPermissionsResult(int req,String[] perms,int[] grants){
        super.onRequestPermissionsResult(req,perms,grants);
        if(req==REQ_LOCATION){
            if(hasLocationPermission()){ startShareService(); requestNotificationPermissionIfNeeded(); if("map".equals(currentPage)) pushMap(); }
            else toast("La localisation est nécessaire pour apparaître dans le convoi");
        }
    }
    private void startShareService(){
        if(!prefs.hasActiveConvoy() || !hasLocationPermission()) return;
        Intent s=new Intent(this,LocationShareService.class);
        try{ if(Build.VERSION.SDK_INT>=26)startForegroundService(s);else startService(s); }
        catch(Exception e){ toast("Impossible de démarrer le partage GPS"); }
    }
''' + m[end:]

start=m.index('    private void renderMapPage() {')
end=m.index('    private void renderParticipantsPage(){', start)
m=m[:start]+'''    private void renderMapPage() {
        currentPage = "map";
        refreshBottomNav();
        content.removeAllViews();
        LinearLayout top=pageHeader("‹","CARTE DU CONVOI");
        top.getChildAt(0).setOnClickListener(v->{render();startPolling();});
        content.addView(top);

        LinearLayout gpsCard=cardBox();
        gpsCard.addView(text("LOCALISATION",12,true,accent));
        TextView gpsState=text(gpsStatusText(),13,true,gpsStatusGood()?Color.rgb(90,200,120):danger);
        gpsState.setPadding(0,dp(5),0,dp(2)); gpsCard.addView(gpsState);
        TextView serverState=text(gpsServerStatusText(),12,false,muted); gpsCard.addView(serverState);
        if(!hasLocationPermission()){
            Button allow=outlinedButton("◎   AUTORISER LA LOCALISATION",accent);
            allow.setOnClickListener(v->ensurePermissionsAndService()); gpsCard.addView(allow);
        }else{
            Button refresh=outlinedButton("◎   RELANCER LE GPS",Color.rgb(94,99,104));
            refresh.setOnClickListener(v->{startShareService();toast("Partage GPS relancé");ui.postDelayed(()->{gpsState.setText(gpsStatusText());serverState.setText(gpsServerStatusText());pushMap();},1200);}); gpsCard.addView(refresh);
        }
        content.addView(gpsCard);

        mapPageReady=false;
        mapView=new WebView(this);
        mapView.getSettings().setJavaScriptEnabled(true);
        mapView.getSettings().setDomStorageEnabled(true);
        mapView.getSettings().setLoadsImagesAutomatically(true);
        mapView.getSettings().setBlockNetworkImage(false);
        mapView.getSettings().setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);
        mapView.setBackgroundColor(bg);
        mapView.setWebViewClient(new android.webkit.WebViewClient(){
            @Override public void onPageFinished(WebView v,String url){mapPageReady=true;pushMap();scheduleMapLocalRefresh();}
            @Override public void onReceivedError(WebView view,android.webkit.WebResourceRequest req,android.webkit.WebResourceError err){
                if(req!=null && req.isForMainFrame()) toast("Impossible d’ouvrir la carte");
            }
        });
        mapView.loadUrl("file:///android_asset/convoy_map.html");
        LinearLayout mapShell=cardBox(); mapShell.setPadding(0,0,0,0); mapShell.setClipToOutline(true); mapShell.addView(mapView,new LinearLayout.LayoutParams(-1,dp(500))); content.addView(mapShell);
        if(snapshot!=null){JSONObject rally=snapshot.optJSONObject("rally"); if(rally!=null){LinearLayout box=cardBox(); box.addView(text("📍  Point de regroupement",12,true,accent));box.addView(text(rally.optString("name","Point de regroupement"),20,true,fg)); String desired=rally.optString("desiredTime",""); if(!desired.isEmpty()) box.addView(text("Heure souhaitée : "+desired,12,true,accent));JSONObject me=findMe();JSONObject ml=me==null?null:me.optJSONObject("location");if(ml!=null){double d=GeoUtils.distanceMeters(ml.optDouble("lat"),ml.optDouble("lon"),rally.optDouble("lat"),rally.optDouble("lon"));box.addView(text(GeoUtils.humanDistance(d),14,false,muted));}Button open=button("➤   OUVRIR DANS LE GPS",accent,Color.rgb(17,18,19));open.setOnClickListener(v->openGps(rally));box.addView(open);content.addView(box);}}
    }
    private boolean gpsStatusGood(){ return hasLocationPermission() && prefs.getLong(LocationShareService.PREF_GPS_FIX_AT,0)>0; }
    private String gpsStatusText(){
        if(!hasLocationPermission()) return "● GPS : autorisation manquante";
        long at=prefs.getLong(LocationShareService.PREF_GPS_FIX_AT,0);
        if(at<=0) return "● GPS : en attente d’une position…";
        return "● GPS téléphone : reçu "+ageText(Math.max(0,System.currentTimeMillis()-at));
    }
    private String gpsServerStatusText(){
        String err=prefs.get(LocationShareService.PREF_GPS_ERROR,"");
        long sent=prefs.getLong(LocationShareService.PREF_GPS_SENT_AT,0);
        if(!err.isEmpty()) return "Serveur : "+err;
        if(sent>0) return "Serveur : position envoyée "+ageText(Math.max(0,System.currentTimeMillis()-sent));
        return "Serveur : aucune position envoyée pour l’instant";
    }
    private JSONObject snapshotForMap(){
        try{
            JSONObject out=snapshot==null?new JSONObject():new JSONObject(snapshot.toString());
            JSONArray ps=out.optJSONArray("participants"); if(ps==null){ps=new JSONArray();out.put("participants",ps);}
            String meId=prefs.get("participantId",""); JSONObject me=null;
            for(int i=0;i<ps.length();i++){JSONObject p=ps.optJSONObject(i);if(p!=null&&meId.equals(p.optString("id"))){me=p;break;}}
            if(me==null && !meId.isEmpty()){
                me=new JSONObject().put("id",meId).put("name",prefs.get("profileName","Moi")).put("vehicle",prefs.get("profileVehicle","Véhicule")); ps.put(me);
            }
            long fixAt=prefs.getLong(LocationShareService.PREF_GPS_FIX_AT,0);
            if(me!=null && fixAt>0){
                double lat=Double.parseDouble(prefs.get(LocationShareService.PREF_GPS_LAT,"0"));
                double lon=Double.parseDouble(prefs.get(LocationShareService.PREF_GPS_LON,"0"));
                if(Math.abs(lat)<=90 && Math.abs(lon)<=180 && (lat!=0 || lon!=0)){
                    JSONObject loc=new JSONObject().put("lat",lat).put("lon",lon).put("receivedAt",fixAt).put("deviceTime",fixAt);
                    try{double acc=Double.parseDouble(prefs.get(LocationShareService.PREF_GPS_ACC,"-1"));if(acc>=0)loc.put("accuracy",acc);}catch(Exception ignored){}
                    me.put("location",loc);
                    out.put("serverTime",System.currentTimeMillis());
                }
            }
            return out;
        }catch(Exception e){ return snapshot==null?new JSONObject():snapshot; }
    }
    private void scheduleMapLocalRefresh(){
        ui.postDelayed(new Runnable(){@Override public void run(){
            if(!"map".equals(currentPage)||mapView==null||!mapPageReady)return;
            pushMap(); ui.postDelayed(this,2000);
        }},2000);
    }
    private void pushMap(){ if(mapView==null||!mapPageReady)return; JSONObject data=snapshotForMap(); String raw=JSONObject.quote(data.toString()); String id=JSONObject.quote(prefs.get("participantId","")); mapView.evaluateJavascript("window.updateConvoy("+raw+","+id+")",null); }

''' + m[end:]
m=m.replace('cardTitle(content,"Mode Convoi 0.3.7",','cardTitle(content,"Mode Convoi 0.3.8",',1)
main.write_text(m)
print('0.3.8 GPS/local-own-position patch applied')
