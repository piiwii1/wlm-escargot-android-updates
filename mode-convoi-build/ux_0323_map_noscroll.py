from pathlib import Path
import re

p=Path('mode-convoi-build/android/app/src/main/java/ch/piiwii/modeconvoi/MainActivity.java')
s=p.read_text()

if 'private ScrollView mainScroll;' not in s:
    s=s.replace('    private LinearLayout root, content, bottomNav;\n','    private LinearLayout root, content, bottomNav;\n    private ScrollView mainScroll;\n',1)

if 'mainScroll = scroll;' not in s:
    s=s.replace('        ScrollView scroll = new ScrollView(this);\n        scroll.setFillViewport(true);\n','        ScrollView scroll = new ScrollView(this);\n        mainScroll = scroll;\n        scroll.setFillViewport(true);\n',1)

new_method=r'''    private void renderMapPage() {
        currentPage = "map";
        refreshBottomNav();
        content.removeAllViews();
        if(root!=null) root.setPadding(dp(10),dp(4),dp(10),dp(4));
        if(mainScroll!=null){mainScroll.scrollTo(0,0);mainScroll.setVerticalScrollBarEnabled(false);mainScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);}

        LinearLayout toolbar=cardBox();
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(10),dp(4),dp(8),dp(4));
        LinearLayout.LayoutParams toolbarLp=new LinearLayout.LayoutParams(-1,dp(50));
        toolbarLp.setMargins(0,dp(3),0,dp(5));
        toolbar.setLayoutParams(toolbarLp);

        TextView title=text("CARTE DU CONVOI",14,true,fg);
        toolbar.addView(title,new LinearLayout.LayoutParams(0,dp(42),1));

        TextView gpsChip=text("● GPS",11,true,gpsStatusGood()?Color.rgb(90,200,120):danger);
        gpsChip.setGravity(Gravity.CENTER);
        gpsChip.setPadding(dp(9),0,dp(9),0);
        gpsChip.setBackground(roundBg(control,gpsStatusGood()?Color.rgb(76,130,82):danger,12,1));
        gpsChip.setOnClickListener(v->{
            if(!hasLocationPermission()) ensurePermissionsAndService();
            else{
                startShareService();
                toast("GPS relancé");
                ui.postDelayed(()->{gpsChip.setTextColor(gpsStatusGood()?Color.rgb(90,200,120):danger);pushMap();},1000);
            }
        });
        toolbar.addView(gpsChip,new LinearLayout.LayoutParams(dp(68),dp(34)));

        Button full=smallButton("⛶",Color.TRANSPARENT,accent);
        full.setTextSize(20);
        full.setBackground(roundBg(Color.TRANSPARENT,accent,10,1));
        full.setOnClickListener(v->showFullScreenMap());
        LinearLayout.LayoutParams fullLp=new LinearLayout.LayoutParams(dp(42),dp(34));
        fullLp.setMargins(dp(7),0,0,0);
        toolbar.addView(full,fullLp);
        content.addView(toolbar);

        FrameLayout mapStage=new FrameLayout(this);
        mapStage.setBackground(roundBg(card,border,16,1));
        mapStage.setClipToOutline(true);

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
        mapStage.addView(mapView,new FrameLayout.LayoutParams(-1,-1));

        if(snapshot!=null){
            JSONObject rally=snapshot.optJSONObject("rally");
            if(rally!=null){
                LinearLayout overlay=new LinearLayout(this);
                overlay.setOrientation(LinearLayout.HORIZONTAL);
                overlay.setGravity(Gravity.CENTER_VERTICAL);
                overlay.setPadding(dp(12),dp(7),dp(8),dp(7));
                overlay.setBackground(roundBg(darkTheme?Color.rgb(22,25,28):Color.WHITE,accent,14,1));

                LinearLayout labels=new LinearLayout(this);
                labels.setOrientation(LinearLayout.VERTICAL);
                labels.addView(text("📍  "+rally.optString("name","Point de regroupement"),13,true,fg));
                StringBuilder sub=new StringBuilder();
                String desired=rally.optString("desiredTime","");
                JSONObject me=findMe();
                JSONObject ml=me==null?null:me.optJSONObject("location");
                if(ml!=null){
                    double d=GeoUtils.distanceMeters(ml.optDouble("lat"),ml.optDouble("lon"),rally.optDouble("lat"),rally.optDouble("lon"));
                    sub.append(GeoUtils.humanDistance(d));
                }
                if(!desired.isEmpty()){
                    if(sub.length()>0)sub.append("  ·  ");
                    sub.append(desired);
                }
                if(sub.length()==0)sub.append("Point de regroupement actif");
                labels.addView(text(sub.toString(),11,false,muted));
                overlay.addView(labels,new LinearLayout.LayoutParams(0,dp(48),1));

                Button open=smallButton("GPS ➤",accent,Color.rgb(17,18,19));
                open.setOnClickListener(v->openGps(rally));
                overlay.addView(open,new LinearLayout.LayoutParams(dp(76),dp(38)));

                FrameLayout.LayoutParams op=new FrameLayout.LayoutParams(-1,dp(64),Gravity.BOTTOM);
                op.setMargins(dp(8),0,dp(8),dp(8));
                mapStage.addView(overlay,op);
            }
        }

        LinearLayout.LayoutParams mapLp=new LinearLayout.LayoutParams(-1,0,1);
        content.addView(mapStage,mapLp);
        ui.post(this::fitMapPageNoScroll);
    }

    private void fitMapPageNoScroll(){
        if(!"map".equals(currentPage)||mainScroll==null||root==null||content==null)return;
        int fixed=root.getPaddingTop()+root.getPaddingBottom();
        for(int i=0;i<root.getChildCount();i++){
            View child=root.getChildAt(i);
            if(child!=content && child.getVisibility()!=View.GONE) fixed+=child.getMeasuredHeight();
        }
        int available=Math.max(dp(320),mainScroll.getMeasuredHeight()-fixed);
        ViewGroup.LayoutParams lp=content.getLayoutParams();
        lp.height=available;
        content.setLayoutParams(lp);
        mainScroll.scrollTo(0,0);
    }

    private void restoreScrollableContent(){
        if(root!=null)root.setPadding(dp(16),dp(8),dp(16),dp(28));
        if(mainScroll!=null){mainScroll.setVerticalScrollBarEnabled(true);mainScroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);mainScroll.scrollTo(0,0);}
        if(content!=null){ViewGroup.LayoutParams lp=content.getLayoutParams();lp.height=ViewGroup.LayoutParams.WRAP_CONTENT;content.setLayoutParams(lp);}
    }
'''
pat=r'    private void renderMapPage\(\) \{.*?(?=    private boolean gpsStatusGood\(\))'
s,n=re.subn(pat,new_method,s,flags=re.S)
if n!=1:
    raise SystemExit(f'renderMapPage replacement count={n}')

if 'if(!"map".equals(page)) restoreScrollableContent();' not in s:
    s=s.replace('    private void navigateBottom(String page){\n        if("home".equals(page)){\n','    private void navigateBottom(String page){\n        if(!"map".equals(page)) restoreScrollableContent();\n        if("home".equals(page)){\n',1)

p.write_text(s)
print('Mode Convoi 0.3.23 map no-scroll patch applied')
