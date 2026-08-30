from pathlib import Path
import re

p=Path('mode-convoi-build/android/app/src/main/java/ch/piiwii/modeconvoi/MainActivity.java')
s=p.read_text()

if 'private boolean mapPageReady' not in s:
    s=s.replace('    private WebView mapView;','    private WebView mapView;\n    private boolean mapPageReady=false;',1)

pat=re.compile(r'''        mapView=new WebView\(this\); mapView\.getSettings\(\)\.setJavaScriptEnabled\(true\); mapView\.getSettings\(\)\.setDomStorageEnabled\(true\); mapView\.setBackgroundColor\(bg\); mapView\.loadUrl\("file:///android_asset/convoy_map\.html"\);\n        mapView\.setWebViewClient\(new android\.webkit\.WebViewClient\(\)\{@Override public void onPageFinished\(WebView v,String url\)\{pushMap\(\);\}\}\);\n        LinearLayout mapShell=cardBox\(\); mapShell\.setPadding\(0,0,0,0\); mapShell\.setClipToOutline\(true\); mapShell\.addView\(mapView,new LinearLayout\.LayoutParams\(-1,dp\(500\)\)\); content\.addView\(mapShell\); pushMap\(\);''')
rep='''        mapPageReady=false;\n        mapView=new WebView(this);\n        mapView.getSettings().setJavaScriptEnabled(true);\n        mapView.getSettings().setDomStorageEnabled(true);\n        mapView.getSettings().setLoadsImagesAutomatically(true);\n        mapView.getSettings().setBlockNetworkImage(false);\n        mapView.getSettings().setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);\n        mapView.setBackgroundColor(bg);\n        mapView.setWebViewClient(new android.webkit.WebViewClient(){\n            @Override public void onPageFinished(WebView v,String url){mapPageReady=true;pushMap();}\n            @Override public void onReceivedError(WebView view,android.webkit.WebResourceRequest req,android.webkit.WebResourceError err){\n                if(req!=null && req.isForMainFrame()) toast("Impossible d’ouvrir la carte");\n            }\n        });\n        mapView.loadUrl("file:///android_asset/convoy_map.html");\n        LinearLayout mapShell=cardBox(); mapShell.setPadding(0,0,0,0); mapShell.setClipToOutline(true); mapShell.addView(mapView,new LinearLayout.LayoutParams(-1,dp(500))); content.addView(mapShell);'''
s,n=pat.subn(rep,s,count=1)
if n!=1: raise SystemExit('map WebView block not found')

s=s.replace('private void pushMap(){ if(mapView==null||snapshot==null)return; String raw=JSONObject.quote(snapshot.toString());', 'private void pushMap(){ if(mapView==null||!mapPageReady)return; String raw=JSONObject.quote(snapshot==null?"{}":snapshot.toString());',1)
s=s.replace('cardTitle(content,"Mode Convoi 0.3.6",','cardTitle(content,"Mode Convoi 0.3.7",',1)

p.write_text(s)
print('0.3.7 map patch applied')
