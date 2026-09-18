package ch.piiwii.m6test;

import android.app.Activity;
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
    private static final String HIDE_ME_PLAY = "https://play.google.com/store/apps/details?id=hideme.android.vpn";
    private static final String M6_URL = "https://www.m6.fr/";
    private static final String M6_GEO_URL = "https://geo.6play.fr/v1/geoInfo/?";
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
        title.setText("PiiWii M6 Test 0.1.1");
        title.setTextSize(25);
        title.setTextColor(Color.rgb(20, 31, 48));
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setPadding(0, 0, 0, dp(8));
        root.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView help = new TextView(this);
        help.setText("Le VPN n’est pas intégré à ce prototype.\n1. Installe/ouvre hide.me\n2. Dans hide.me, choisis France et connecte\n3. Reviens ici et vérifie\n4. Puis teste M6+");
        help.setTextSize(15);
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

        Button vpnButton = makeButton("Installer / ouvrir hide.me");
        vpnButton.setOnClickListener(v -> openHideMe());
        root.addView(vpnButton);

        Button vpnWebButton = makeButton("Page hide.me sur Google Play (secours)");
        vpnWebButton.setOnClickListener(v -> openExternal(HIDE_ME_PLAY));
        root.addView(vpnWebButton);

        Button checkButton = makeButton("Vérifier : suis-je en France ?");
        checkButton.setOnClickListener(v -> checkIpCountry());
        root.addView(checkButton);

        Button m6GeoButton = makeButton("Test officiel M6 : pays détecté");
        m6GeoButton.setOnClickListener(v -> openExternal(M6_GEO_URL));
        root.addView(m6GeoButton);

        LinearLayout m6Row = new LinearLayout(this);
        m6Row.setOrientation(LinearLayout.HORIZONTAL);

        Button m6Button = makeButton("M6+ dans l’app");
        m6Button.setOnClickListener(v -> webView.loadUrl(M6_URL));
        LinearLayout.LayoutParams half1 = new LinearLayout.LayoutParams(0, dp(58), 1f);
        half1.setMargins(0, dp(5), dp(5), dp(5));
        m6Row.addView(m6Button, half1);

        Button browserButton = makeButton("M6+ navigateur");
        browserButton.setOnClickListener(v -> openExternal(M6_URL));
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
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        webView.loadData("<html><body style='font-family:sans-serif;text-align:center;padding:28px;color:#666;background:#fff'>M6+ apparaîtra ici.<br><br>Si la vidéo refuse la vue intégrée, utilise <b>M6+ navigateur</b>.</body></html>", "text/html", "UTF-8");

        LinearLayout.LayoutParams webParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        webParams.setMargins(0, dp(6), 0, 0);
        root.addView(webView, webParams);
        setContentView(root);
    }

    private Button makeButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(15);
        b.setAllCaps(false);
        b.setFocusable(true);
        b.setMinHeight(dp(56));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58));
        p.setMargins(0, dp(4), 0, dp(4));
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
                connection.setRequestProperty("User-Agent", "PiiWii-M6-Test/0.1.1");
                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code);

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
                        status.setText("✅ FRANCE détectée\nIP : " + ip + "\nTeste maintenant le bouton officiel M6.");
                        status.setTextColor(Color.rgb(27, 120, 55));
                    } else {
                        status.setText("❌ Pas en France : " + country + " (" + countryCode + ")\nIP : " + ip + "\nOuvre hide.me, choisis France, connecte puis reviens.");
                        status.setTextColor(Color.rgb(180, 70, 35));
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    status.setText("⚠️ Vérification impossible\n" + safeMessage(e));
                    status.setTextColor(Color.rgb(180, 70, 35));
                });
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private void openHideMe() {
        try {
            Intent launch = getPackageManager().getLaunchIntentForPackage(HIDE_ME_PACKAGE);
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launch);
                return;
            }
        } catch (Exception ignored) {
        }
        Toast.makeText(this, "hide.me n’est pas installé. Ouverture de Google Play.", Toast.LENGTH_LONG).show();
        openExternal(HIDE_ME_PLAY);
    }

    private void openExternal(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Impossible d’ouvrir le lien", Toast.LENGTH_LONG).show();
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
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
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
