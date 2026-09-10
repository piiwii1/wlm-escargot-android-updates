package ch.piiwii.listy;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public final class MainActivity extends Activity {
    public static final String HOME = "https://piiwii.ch/listy/";
    public static final String EXTRA_ACTION = "listy_action";
    public static final String ACTION_ADD_MEMO = "add_memo";
    public static final String ACTION_OPEN_MEMO = "open_memo";
    private static final int FILE_PICKER = 4101;
    private static final int LOCATION = 4102;

    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private GeolocationPermissions.Callback geoCallback;
    private String geoOrigin;
    private String pendingNativeAction;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        pendingNativeAction = readAction(getIntent());

        webView = new WebView(this);
        setContentView(webView);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setUserAgentString(settings.getUserAgentString() + " ListYAndroid/1.1.0");

        webView.addJavascriptInterface(new NativeBridge(this), "ListYAndroid");
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return route(request.getUrl());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return route(Uri.parse(url));
            }
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                dispatchPendingAction();
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), FILE_PICKER);
                    return true;
                } catch (ActivityNotFoundException e) {
                    fileCallback = null;
                    Toast.makeText(MainActivity.this, "Aucun sélecteur de fichier disponible.", Toast.LENGTH_LONG).show();
                    return false;
                }
            }
            @Override public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    callback.invoke(origin, true, false);
                } else {
                    geoOrigin = origin;
                    geoCallback = callback;
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION);
                }
            }
        });

        if (state != null) {
            webView.restoreState(state);
            dispatchPendingAction();
        } else {
            String startAction = pendingNativeAction;
            webView.loadUrl(startUrlFor(startAction));
            if (!startAction.isEmpty()) pendingNativeAction = "";
        }
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        pendingNativeAction = readAction(intent);
        dispatchPendingAction();
    }

    private String readAction(Intent intent) {
        if (intent == null) return "";
        String value = intent.getStringExtra(EXTRA_ACTION);
        return value == null ? "" : value.trim();
    }

    private String startUrlFor(String action) {
        if (ACTION_ADD_MEMO.equals(action) || ACTION_OPEN_MEMO.equals(action)) {
            return HOME + "?listy_mobile_action=" + Uri.encode(action);
        }
        return HOME;
    }

    private void dispatchPendingAction() {
        if (webView == null || pendingNativeAction == null || pendingNativeAction.isEmpty()) return;
        final String action = pendingNativeAction;
        webView.postDelayed(new Runnable() {
            int attempts = 0;
            @Override public void run() {
                if (webView == null || isFinishing()) return;
                String escaped = action.replace("\\", "\\\\").replace("'", "\\'");
                webView.evaluateJavascript("(function(){if(window.ListYNative&&window.ListYNative.handleAndroidAction){window.ListYNative.handleAndroidAction('" + escaped + "');return 'ok';}return 'wait';})()", value -> {
                    if (value != null && value.contains("ok")) {
                        pendingNativeAction = "";
                    } else if (++attempts < 12) {
                        webView.postDelayed(this, 350);
                    }
                });
            }
        }, 250);
    }

    private boolean route(Uri uri) {
        if (uri == null || uri.getScheme() == null) return false;
        String scheme = uri.getScheme().toLowerCase();
        if (scheme.equals("https") || scheme.equals("http")) {
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
            String path = uri.getPath() == null ? "" : uri.getPath();
            if ((host.equals("piiwii.ch") || host.equals("www.piiwii.ch")) && path.startsWith("/listy")) return false;
        }
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
        catch (ActivityNotFoundException e) { Toast.makeText(this, "Aucune application ne peut ouvrir ce lien.", Toast.LENGTH_LONG).show(); }
        return true;
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_PICKER && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            fileCallback = null;
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION && geoCallback != null) {
            boolean granted = false;
            for (int result : grantResults) if (result == PackageManager.PERMISSION_GRANTED) granted = true;
            geoCallback.invoke(geoOrigin, granted, false);
            geoCallback = null;
            geoOrigin = null;
        }
    }

    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }
}
