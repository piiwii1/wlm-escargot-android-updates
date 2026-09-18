package ch.piiwii.m6test;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
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
    private static final String CONNECTIVITY_URL = "https://www.google.com/generate_204";

    private TextView status;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        checkIpCountry();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(24));
        root.setBackgroundColor(Color.rgb(245, 247, 250));
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("PiiWii M6 Test 0.1.2");
        title.setTextSize(25);
        title.setTextColor(Color.rgb(20, 31, 48));
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setPadding(0, 0, 0, dp(8));
        root.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView help = new TextView(this);
        help.setText("1. Connecte hide.me sur France\n2. Vérifie l’IP\n3. Lance le diagnostic M6\n4. Ouvre M6+ en plein écran");
        help.setTextSize(15);
        help.setTextColor(Color.DKGRAY);
        help.setGravity(Gravity.CENTER_HORIZONTAL);
        help.setPadding(dp(8), 0, dp(8), dp(10));
        root.addView(help, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        status = new TextView(this);
        status.setText("Vérification de la connexion…");
        status.setTextSize(17);
        status.setTextColor(Color.rgb(55, 71, 79));
        status.setGravity(Gravity.CENTER);
        status.setPadding(dp(12), dp(12), dp(12), dp(12));
        root.addView(status, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        progress = new ProgressBar(this);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(dp(30), dp(30));
        progressParams.gravity = Gravity.CENTER_HORIZONTAL;
        progressParams.setMargins(0, dp(4), 0, dp(8));
        root.addView(progress, progressParams);

        Button vpnButton = makeButton("Installer / ouvrir hide.me");
        vpnButton.setOnClickListener(v -> openHideMe());
        root.addView(vpnButton);

        Button checkButton = makeButton("Vérifier : suis-je en France ?");
        checkButton.setOnClickListener(v -> checkIpCountry());
        root.addView(checkButton);

        Button diagButton = makeButton("Diagnostic réseau M6");
        diagButton.setOnClickListener(v -> runDiagnostics());
        root.addView(diagButton);

        Button m6FullButton = makeButton("M6+ PLEIN ÉCRAN");
        m6FullButton.setOnClickListener(v -> startActivity(new Intent(this, M6Activity.class)));
        root.addView(m6FullButton);

        Button browserButton = makeButton("M6+ dans le navigateur externe");
        browserButton.setOnClickListener(v -> openExternal(M6_URL));
        root.addView(browserButton);

        Button m6GeoButton = makeButton("Test officiel M6 : pays détecté");
        m6GeoButton.setOnClickListener(v -> openExternal(M6_GEO_URL));
        root.addView(m6GeoButton);

        Button vpnWebButton = makeButton("Page hide.me Google Play (secours)");
        vpnWebButton.setOnClickListener(v -> openExternal(HIDE_ME_PLAY));
        root.addView(vpnWebButton);

        setContentView(scroll);
    }

    private Button makeButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setFocusable(true);
        b.setMinHeight(dp(58));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(60));
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
                connection = open(IP_CHECK_URL, "application/json");
                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code);

                String body = readBody(connection);
                JSONObject json = new JSONObject(body);
                String countryCode = json.optString("country_code", "?");
                String country = json.optString("country", "Inconnu");
                String ip = json.optString("ip", "?");
                boolean france = "FR".equalsIgnoreCase(countryCode);

                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    if (france) {
                        status.setText("✅ FRANCE détectée\nIP : " + ip + "\nLance maintenant ‘Diagnostic réseau M6’. ");
                        status.setTextColor(Color.rgb(27, 120, 55));
                    } else {
                        status.setText("❌ Pas en France : " + country + " (" + countryCode + ")\nIP : " + ip + "\nConnecte hide.me sur France puis revérifie.");
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

    private void runDiagnostics() {
        progress.setVisibility(View.VISIBLE);
        status.setText("Diagnostic en cours…");
        status.setTextColor(Color.rgb(55, 71, 79));

        new Thread(() -> {
            StringBuilder report = new StringBuilder();
            boolean internetOk = false;
            boolean franceOk = false;
            boolean m6Ok = false;

            HttpURLConnection c = null;
            try {
                c = open(CONNECTIVITY_URL, "*/*");
                int code = c.getResponseCode();
                internetOk = code == 204 || (code >= 200 && code < 400);
                report.append(internetOk ? "✅ Internet : OK" : "❌ Internet : HTTP " + code).append('\n');
            } catch (Exception e) {
                report.append("❌ Internet : ").append(safeMessage(e)).append('\n');
            } finally {
                if (c != null) c.disconnect();
            }

            c = null;
            try {
                c = open(IP_CHECK_URL, "application/json");
                int code = c.getResponseCode();
                if (code >= 200 && code < 300) {
                    JSONObject json = new JSONObject(readBody(c));
                    String cc = json.optString("country_code", "?");
                    String ip = json.optString("ip", "?");
                    franceOk = "FR".equalsIgnoreCase(cc);
                    report.append(franceOk ? "✅ IP France : " : "❌ IP non-France (" + cc + ") : ").append(ip).append('\n');
                } else {
                    report.append("❌ IP : HTTP ").append(code).append('\n');
                }
            } catch (Exception e) {
                report.append("❌ IP : ").append(safeMessage(e)).append('\n');
            } finally {
                if (c != null) c.disconnect();
            }

            c = null;
            try {
                c = open(M6_URL, "text/html,*/*");
                int code = c.getResponseCode();
                m6Ok = code >= 200 && code < 400;
                report.append(m6Ok ? "✅ www.m6.fr : HTTP " : "❌ www.m6.fr : HTTP ").append(code).append('\n');
            } catch (Exception e) {
                report.append("❌ www.m6.fr : ").append(safeMessage(e)).append('\n');
            } finally {
                if (c != null) c.disconnect();
            }

            c = null;
            try {
                c = open(M6_GEO_URL, "application/json,text/plain,*/*");
                int code = c.getResponseCode();
                report.append(code >= 200 && code < 400 ? "✅ Geo M6 : HTTP " : "❌ Geo M6 : HTTP ").append(code);
                if (code >= 200 && code < 300) {
                    String body = readBody(c).replace('\n', ' ').trim();
                    if (body.length() > 180) body = body.substring(0, 180) + "…";
                    if (!body.isEmpty()) report.append("\n↳ ").append(body);
                }
                report.append('\n');
            } catch (Exception e) {
                report.append("❌ Geo M6 : ").append(safeMessage(e)).append('\n');
            } finally {
                if (c != null) c.disconnect();
            }

            boolean finalInternetOk = internetOk;
            boolean finalFranceOk = franceOk;
            boolean finalM6Ok = m6Ok;
            String finalReport = report.toString().trim();
            runOnUiThread(() -> {
                progress.setVisibility(View.GONE);
                String conclusion;
                if (!finalInternetOk) conclusion = "\n\n➡ Le VPN coupe ou perturbe Internet.";
                else if (!finalFranceOk) conclusion = "\n\n➡ Le VPN ne sort pas réellement en France.";
                else if (!finalM6Ok) conclusion = "\n\n➡ Internet + France sont OK, mais M6 refuse ou n’est pas joignable via cette sortie.";
                else conclusion = "\n\n➡ Réseau + France + site M6 sont joignables. Si M6+ affiche encore ‘pas de connexion’, le blocage est probablement au niveau du site/lecteur ou de la détection VPN.";
                status.setText(finalReport + conclusion);
                status.setTextColor((finalInternetOk && finalFranceOk) ? Color.rgb(27, 100, 55) : Color.rgb(180, 70, 35));
            });
        }).start();
    }

    private HttpURLConnection open(String url, String accept) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Accept", accept);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/131 Mobile Safari/537.36 PiiWii-M6-Test/0.1.2");
        return connection;
    }

    private String readBody(HttpURLConnection connection) throws Exception {
        InputStream stream = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) body.append(line).append('\n');
        reader.close();
        return body.toString();
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
        return m == null || m.trim().isEmpty() ? e.getClass().getSimpleName() : m;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
