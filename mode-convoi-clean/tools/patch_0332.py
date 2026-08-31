from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
main_path = root / "android/app/src/main/java/ch/piiwii/modeconvoi/MainActivity.java"
build_path = root / "android/app/build.gradle"
api_path = root / "android/app/src/main/java/ch/piiwii/modeconvoi/ConvoyApi.java"
controller_path = root / "android/app/src/main/java/ch/piiwii/modeconvoi/ConvoyMapController.java"

main = main_path.read_text(encoding="utf-8")

if not controller_path.exists():
    raise SystemExit("0.3.32: ConvoyMapController.java missing")
controller = controller_path.read_text(encoding="utf-8")
for token in (
    "public final class ConvoyMapController",
    "public void attachPage(",
    "public void detachPage()",
    "public void attachFullScreen(",
    "public void detachFullScreen()",
    "public void pushAll()",
    "public JSONObject snapshotForMap()",
    "schedulePageRefresh",
    "scheduleFullScreenRefresh",
    "pageGeneration",
    "fullScreenGeneration",
):
    if token not in controller:
        raise SystemExit(f"0.3.32: map controller token missing: {token}")

if "private ConvoyMapController mapController;" not in main:
    field_anchor = "    private ConvoySessionManager sessionManager;\n"
    if main.count(field_anchor) != 1:
        raise SystemExit("0.3.32: session manager field anchor mismatch")
    main = main.replace(field_anchor, field_anchor + "    private ConvoyMapController mapController;\n", 1)

    main = main.replace("    private boolean mapPageReady=false;\n", "", 1)
    main = main.replace("    private WebView fullScreenMapView;\n", "", 1)
    main = main.replace("    private boolean fullScreenMapReady=false;\n", "", 1)

    init_anchor = "        sessionManager = new ConvoySessionManager(prefs,DEFAULT_SERVER);\n        pollingController = new ConvoyPollingController"
    if main.count(init_anchor) != 1:
        raise SystemExit("0.3.32: map controller initialization anchor mismatch")
    main = main.replace(
        init_anchor,
        "        sessionManager = new ConvoySessionManager(prefs,DEFAULT_SERVER);\n        mapController = new ConvoyMapController(prefs,()->snapshot);\n        pollingController = new ConvoyPollingController",
        1,
    )

    snapshot_anchor = "                else if(mapView!=null)pushMap();"
    if main.count(snapshot_anchor) != 1:
        raise SystemExit("0.3.32: polling map push anchor mismatch")
    main = main.replace(snapshot_anchor, "                else if(mapView!=null)mapController.pushAll();", 1)

    destroy_anchor = "    @Override protected void onDestroy() { if(pollingController!=null)pollingController.close(); if(liveTalkie!=null)liveTalkie.close(); io.shutdownNow(); super.onDestroy(); }"
    if main.count(destroy_anchor) != 1:
        raise SystemExit("0.3.32: onDestroy anchor mismatch")
    main = main.replace(
        destroy_anchor,
        "    @Override protected void onDestroy() { if(mapController!=null)mapController.close(); if(pollingController!=null)pollingController.close(); if(liveTalkie!=null)liveTalkie.close(); io.shutdownNow(); super.onDestroy(); }",
        1,
    )

    # Any page that explicitly clears mapView must also terminate its refresh generation.
    main = main.replace("mapView=null;", "if(mapController!=null)mapController.detachPage();mapView=null;")

    map_setup_pattern = re.compile(
        r"        mapPageReady=false;\n"
        r"        mapView=new WebView\(this\);\n"
        r"        mapView\.getSettings\(\)\.setJavaScriptEnabled\(true\);\n"
        r"        mapView\.getSettings\(\)\.setDomStorageEnabled\(true\);\n"
        r"        mapView\.getSettings\(\)\.setLoadsImagesAutomatically\(true\);\n"
        r"        mapView\.getSettings\(\)\.setBlockNetworkImage\(false\);\n"
        r"        mapView\.getSettings\(\)\.setCacheMode\(android\.webkit\.WebSettings\.LOAD_DEFAULT\);\n"
        r"        mapView\.setBackgroundColor\(bg\);\n"
        r"        mapView\.setWebViewClient\(new android\.webkit\.WebViewClient\(\)\{\n"
        r"            @Override public void onPageFinished\(WebView v,String url\)\{mapPageReady=true;pushMap\(\);scheduleMapLocalRefresh\(\);\}\n"
        r"            @Override public void onReceivedError\(WebView view,android\.webkit\.WebResourceRequest req,android\.webkit\.WebResourceError err\)\{\n"
        r"                if\(req!=null && req\.isForMainFrame\(\)\) toast\(\"Impossible d’ouvrir la carte\"\);\n"
        r"            \}\n"
        r"        \}\);\n"
        r"        mapView\.loadUrl\(\"file:///android_asset/convoy_map\.html\"\);"
    )
    map_setup_replacement = """        mapView=new WebView(this);
        mapController.attachPage(mapView,bg,()->toast("Impossible d’ouvrir la carte"));"""
    main, count = map_setup_pattern.subn(map_setup_replacement, main, count=1)
    if count != 1:
        raise SystemExit("0.3.32: map WebView setup block not found exactly once")

    map_transport_pattern = re.compile(
        r"    private JSONObject snapshotForMap\(\)\{.*?\n"
        r"    private void pushMap\(\)\{.*?\n\n"
        r"    private void renderParticipantsPage\(\)",
        re.S,
    )
    map_transport_replacement = """    private void pushMap(){if(mapController!=null)mapController.pushPage();}

    private void renderParticipantsPage()"""
    main, count = map_transport_pattern.subn(map_transport_replacement, main, count=1)
    if count != 1:
        raise SystemExit("0.3.32: map snapshot/refresh block not found exactly once")

    full_pattern = re.compile(
        r"    private void showFullScreenMap\(\)\{.*?\n"
        r"    private void scheduleFullScreenMapRefresh\(Dialog d\)\{.*?\n\n"
        r"    private void renderMorePage\(\)",
        re.S,
    )
    full_replacement = """    private void showFullScreenMap(){
        if(fullScreenMapDialog!=null&&fullScreenMapDialog.isShowing())return;
        final Dialog d=new Dialog(this,android.R.style.Theme_Black_NoTitleBar_Fullscreen);fullScreenMapDialog=d;
        FrameLayout frame=new FrameLayout(this);frame.setBackgroundColor(Color.rgb(10,12,14));
        WebView w=new WebView(this);mapController.attachFullScreen(w,bg,()->toast("Impossible d’ouvrir la carte"));frame.addView(w,new FrameLayout.LayoutParams(-1,-1));
        TextView close=text("✕",27,true,Color.WHITE);close.setGravity(Gravity.CENTER);close.setBackground(roundBg(Color.rgb(20,23,26),Color.rgb(90,94,98),22,1));close.setOnClickListener(v->d.dismiss());FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(dp(46),dp(46),Gravity.LEFT|Gravity.TOP);cp.setMargins(dp(12),dp(14),0,0);frame.addView(close,cp);
        TextView title=text("CARTE DU CONVOI",12,true,accent);title.setGravity(Gravity.CENTER);title.setBackground(roundBg(Color.rgb(20,23,26),Color.rgb(90,94,98),18,1));FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(dp(160),dp(40),Gravity.TOP|Gravity.CENTER_HORIZONTAL);tp.setMargins(0,dp(17),0,0);frame.addView(title,tp);
        d.setContentView(frame);d.setOnDismissListener(x->{if(mapController!=null)mapController.detachFullScreen();fullScreenMapDialog=null;});d.show();if(d.getWindow()!=null)d.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void renderMorePage()"""
    main, count = full_pattern.subn(full_replacement, main, count=1)
    if count != 1:
        raise SystemExit("0.3.32: full-screen map block not found exactly once")

    main = main.replace('cardTitle(content,"Mode Convoi 0.3.31",', 'cardTitle(content,"Mode Convoi 0.3.32",', 1)

required = (
    "private ConvoyMapController mapController;",
    "mapController = new ConvoyMapController(prefs,()->snapshot);",
    "mapController.attachPage(mapView,bg",
    "mapController.attachFullScreen(w,bg",
    "mapController.detachFullScreen()",
    "mapController.pushAll()",
    "mapController.pushPage()",
    "mapController.close()",
    "Mode Convoi 0.3.32",
)
for token in required:
    if token not in main:
        raise SystemExit(f"0.3.32: required MainActivity token missing: {token}")

for forbidden in (
    "mapPageReady",
    "fullScreenMapView",
    "fullScreenMapReady",
    "private JSONObject snapshotForMap(",
    "scheduleMapLocalRefresh",
    "pushFullScreenMap",
    "scheduleFullScreenMapRefresh",
    'mapView.loadUrl("file:///android_asset/convoy_map.html")',
):
    if forbidden in main:
        raise SystemExit(f"0.3.32: stale map lifecycle remains in MainActivity: {forbidden}")

# Every explicit mapView clear must invalidate the controller refresh generation first.
for match in re.finditer(r"mapView=null;", main):
    prefix = main[max(0, match.start()-70):match.start()]
    if "mapController.detachPage()" not in prefix:
        raise SystemExit("0.3.32: mapView cleared without controller detach")

main_path.write_text(main, encoding="utf-8")

build = build_path.read_text(encoding="utf-8")
build = build.replace("versionCode 34", "versionCode 35")
build = build.replace("versionName '0.3.31'", "versionName '0.3.32'")
if "versionCode 35" not in build or "versionName '0.3.32'" not in build:
    raise SystemExit("0.3.32: build version migration failed")
build_path.write_text(build, encoding="utf-8")

api = api_path.read_text(encoding="utf-8")
api = api.replace("ModeConvoi-Android/0.3.31", "ModeConvoi-Android/0.3.32")
if "ModeConvoi-Android/0.3.32" not in api:
    raise SystemExit("0.3.32: API user-agent migration failed")
api_path.write_text(api, encoding="utf-8")

print("Mode Convoi 0.3.32 map controller migration validated")
