from pathlib import Path

p=Path('mode-convoi-build/android/app/src/main/java/ch/piiwii/modeconvoi/MainActivity.java')
s=p.read_text()

if 'private boolean mapPageReady' not in s:
    s=s.replace('    private WebView mapView;','    private WebView mapView;\n    private boolean mapPageReady=false;',1)

start=s.find('private void renderMapPage')
end=s.find('private void renderParticipantsPage', start)
if start<0 or end<0: raise SystemExit('map function markers not found')
indent=s.rfind('\n',0,start)+1
prefix=s[:indent]
suffix=s[end:]
new='''    private void renderMapPage() {
        currentPage = "map";
        refreshBottomNav();
        content.removeAllViews();
        LinearLayout top=pageHeader("‹","CARTE DU CONVOI");
        top.getChildAt(0).setOnClickListener(v->{render();startPolling();});
        content.addView(top);
        mapPageReady=false;
        mapView=new WebView(this);
        mapView.getSettings().setJavaScriptEnabled(true);
        mapView.getSettings().setDomStorageEnabled(true);
        mapView.getSettings().setLoadsImagesAutomatically(true);
        mapView.getSettings().setBlockNetworkImage(false);
        mapView.getSettings().setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);
        mapView.setBackgroundColor(bg);
        mapView.setWebViewClient(new android.webkit.WebViewClient(){
            @Override public void onPageFinished(WebView v,String url){mapPageReady=true;pushMap();}
            @Override public void onReceivedError(WebView view,android.webkit.WebResourceRequest req,android.webkit.WebResourceError err){
                if(req!=null && req.isForMainFrame()) toast("Impossible d’ouvrir la carte");
            }
        });
        mapView.loadUrl("file:///android_asset/convoy_map.html");
        LinearLayout mapShell=cardBox();
        mapShell.setPadding(0,0,0,0);
        mapShell.setClipToOutline(true);
        mapShell.addView(mapView,new LinearLayout.LayoutParams(-1,dp(500)));
        content.addView(mapShell);
        if(snapshot!=null){
            JSONObject rally=snapshot.optJSONObject("rally");
            if(rally!=null){
                LinearLayout box=cardBox();
                box.addView(text("📍  Point de regroupement",12,true,accent));
                box.addView(text(rally.optString("name","Point de regroupement"),20,true,fg));
                String desired=rally.optString("desiredTime","");
                if(!desired.isEmpty()) box.addView(text("Heure souhaitée : "+desired,12,true,accent));
                JSONObject me=findMe();JSONObject ml=me==null?null:me.optJSONObject("location");
                if(ml!=null){double d=GeoUtils.distanceMeters(ml.optDouble("lat"),ml.optDouble("lon"),rally.optDouble("lat"),rally.optDouble("lon"));box.addView(text(GeoUtils.humanDistance(d),14,false,muted));}
                Button open=button("➤   OUVRIR DANS LE GPS",accent,Color.rgb(17,18,19));open.setOnClickListener(v->openGps(rally));box.addView(open);content.addView(box);
            }
        }
    }
    private void pushMap(){
        if(mapView==null||!mapPageReady)return;
        String raw=JSONObject.quote(snapshot==null?"{}":snapshot.toString());
        String id=JSONObject.quote(prefs.get("participantId",""));
        mapView.evaluateJavascript("window.updateConvoy("+raw+","+id+")",null);
    }

    '''
s=prefix+new+suffix
s=s.replace('cardTitle(content,"Mode Convoi 0.3.6",','cardTitle(content,"Mode Convoi 0.3.7",',1)
p.write_text(s)
print('0.3.7 map patch applied')
