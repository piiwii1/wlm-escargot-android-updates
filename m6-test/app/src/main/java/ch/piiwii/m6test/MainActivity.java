package ch.piiwii.m6test;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final String HIDE_ME_PACKAGE = "hideme.android.vpn";
    private static final String M6_URL = "https://www.m6.fr/";
    private static final String IP_CHECK_URL = "https://ipwho.is/";

    private TextView status;
    private ProgressBar progress;
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        checkIpCountry();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(12));
        root.setBackgroundColor(Color.rgb(245, 247, 250));

        TextView title = new TextView(this);
        title.setText("PiiWii M6 Test");
        title.setTextSize(26);
        title.setTextColor(Color.rgb(20, 31, 48));
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setPadding(0, 0, 0, dp(8));
        root.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView help = new TextView(this);
        help.setText("1. Ouvre le VPN et choisis France\n2. Reviens ici et vérifie la sortie réseau\n3. Si le statut est FRANCE, ouvre M6+");
        help.setTextSize(16);
        help.setTextColor(Color.DKGRAY);
        help.setGravity(Gravity.CENTER_HORIZONTAL);
        help.setPadding(dp(8), 0, dp(8), dp(10));
        root.addView(help, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        status = new TextView(this);
        status.setText("Vérification de la connexion…");
        status.setTextSize(18);
        status.setTextColor(Color.rgb(55, 71, 79));
        status.setGravity(Gravity.CENTER);
        status.setPadding(dp(12), dp(10), dp(12), dp(10));
        root.addView(status, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        progress = new ProgressBar(this);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(dp(30), dp(30));
        progressParams.gravity = Gravity.CENTER_HORIZONTAL;
        progressParams.setMargins(0, dp(4), 0, dp(8));
        root.addView(progress, progressParams);

        Button vpnButton = makeButton("Ouvrir hide.me VPN");
        vpnButton.setOnClickListener(v -> openHideMe());
        root.addView(vpnButton);

        Button checkButton = makeButton("Vérifier : suis-je en France ?");
        checkButton.setOnClickListener(v -> checkIpCountry());
        root.addView(checkButton);

        LinearLayout m6Row = new LinearLayout(this);
        m6Row.setOrientation(LinearLayout.HORIZONTAL);

        Button m6Button = makeButton("M6+ dans l’app");
        m6Button.setOnClickListener(v -> webView.loadUrl(M6_URL));
        LinearLayout.LayoutParams half1 = new LinearLayout.LayoutParams(0, dp(58), 1f);
        half1.setMargins(0, dp(5), dp(5), dp(5));
        m6Row.addView(m6Button, half1);

        Button browserButton = makeButton("M6+ navigateur");
        browserButton.setOnClickListener(v -> openBrowser(M6_URL));
        LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(0, dp(58), 1f);
        half2.setMargins(dp(5), dp(5), 0, dp(5));
        m6Row.addView(browserButton, half2);
        root.addView(m6Row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        webView = new WebView(this);
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setBuiltInZoomControls(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());

        TextView startPage = new TextView(this);
        startPage.setText("La page M6+ apparaîtra ici.\nLe bouton navigateur est prévu si le lecteur protégé refuse de fonctionner dans WebView.");
        startPage.setTextSize(16);
        startPage.setTextColor(Color.GRAY);
        startPage.setGravity(Gravity.CENTER);
        webView.addView(startPage);

        LinearLayout.LayoutParams webParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        webParams.setMargins(0, dp(6), 0, 0);
        root.addView(webView, webParams);

        setContentView(root);
    }

    private Button makeButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setFocusable(true);
        b.setMinHeight(dp(56));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58));
        p.setMargins(0, dp(5), 0, dp(5));
        b.setLayoutParams(p);
        return b;
    }

    private void checkIpCountry() {
        progress.setVisibility(View.VISIBLE);
        status.setText("Vérification de la sortie Internet…");
        status.setTextColor(Color.rgb(55, 71, 79));

        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(IP_CHECK_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("User-Agent", "PiiWii-M6-Test/0.1.0");

                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) {
                    throw new IllegalStateException("HTTP " + code);
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
                reader.close();

                JSONObject json = new JSONObject(body.toString());
                String countryCode = json.optString("country_code", "?");
                String country = json.optString("country", "Inconnu");
                String ip = json.optString("ip", "?");
                boolean france = "FR".equalsIgnoreCase(countryCode);

                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    if (france) {
                        status.setText("✅ FRANCE détectée\nIP : " + ip + "\nTu peux tester M6+.");
                        status.setTextColor(Color.rgb(27, 120, 55));
                    } else {
                        status.setText("❌ Pas en France : " + country + " (" + countryCode + ")\nIP : " + ip + "\nConnecte le VPN sur France puis revérifie.");
                        status.setTextColor(Color.rgb(180, 70, 35));
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    status.setText("⚠️ Impossible de vérifier le pays\n" + e.getClass().getSimpleName() + ": " + safeMessage(e));
                    status.setTextColor(Color.rgb(180, 70, 35));
                });
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private void openHideMe() {
        Intent launch = getPackageManager().getLaunchIntentForPackage(HIDE_ME_PACKAGE);
        if (launch != null) {
            startActivity(launch);
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + HIDE_ME_PACKAGE)));
        } catch (ActivityNotFoundException e) {
            openBrowser("https://play.google.com/store/apps/details?id=" + HIDE_ME_PACKAGE);
        }
    }

    private void openBrowser(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "Aucun navigateur disponible", Toast.LENGTH_LONG).show();
        }
    }

    private String safeMessage(Exception e) {
        String m = e.getMessage();
        return m == null || m.trim().isEmpty() ? "erreur réseau" : m;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}
