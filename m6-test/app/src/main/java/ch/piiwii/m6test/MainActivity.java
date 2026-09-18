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
    private static final String WINDSCRIBE_PACKAGE = "com.windscribe.vpn";
    private static final String WINDSCRIBE_PLAY = "https://play.google.com/store/apps/details?id=com.windscribe.vpn";
    private static final String M6_PACKAGE = "fr.m6.m6replay";
    private static final String M6_PLAY = "https://play.google.com/store/apps/details?id=fr.m6.m6replay";
    private static final String CHROME_PACKAGE = "com.android.chrome";
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
        title.setText("PiiWii M6 Test 0.1.4 — Windscribe");
        title.setTextSize(24);
        title.setTextColor(Color.rgb(20, 31, 48));
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setPadding(0, 0, 0, dp(8));
        root.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView help = new TextView(this);
        help.setText("hide.me est écarté pour M6+.\n1. Installe/ouvre Windscribe\n2. Choisis France et connecte\n3. Vérifie l’IP dans cette appli ET dans Chrome\n4. Teste le pays M6 puis M6+");
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

        Button vpnButton = makeButton("1. Installer / ouvrir Windscribe");
        vpnButton.setOnClickListener(v -> openPackageOrStore(WINDSCRIBE_PACKAGE, WINDSCRIBE_PLAY, "Windscribe"));
        root.addView(vpnButton);

        Button checkButton = makeButton("2. Vérifier l’IP dans PiiWii M6 Test");
        checkButton.setOnClickListener(v -> checkIpCountry());
        root.addView(checkButton);

        Button chromeIpButton = makeButton("3. Vérifier l’IP DANS CHROME");
        chromeIpButton.setOnClickListener(v -> openInChrome(IP_CHECK_URL));
        root.addView(chromeIpButton);

        Button m6GeoButton = makeButton("4. Pays détecté par M6 dans Chrome");
        m6GeoButton.setOnClickListener(v -> openInChrome(M6_GEO_URL));
        root.addView(m6GeoButton);

        Button diagButton = makeButton("Diagnostic réseau M6");
        diagButton.setOnClickListener(v -> runDiagnostics());
        root.addView(diagButton);

        Button officialM6Button = makeButton("5. Ouvrir l’application officielle M6+");
        officialM6Button.setOnClickListener(v -> openPackageOrStore(M6_PACKAGE, M6_PLAY, "M6+"));
        root.addView(officialM6Button);

        Button chromeButton = makeButton("M6+ dans Chrome — connexion Google");
        chromeButton.setOnClickListener(v -> openInChrome(M6_URL));
        root.addView(chromeButton);

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
                        status.setText("✅ FRANCE détectée\nIP : " + ip + "\nMaintenant vérifie aussi l’IP dans Chrome.");
                        status.setTextColor(Color.rgb(27, 120, 55));
                    } else {
                        status.setText("❌ Pas en France : " + country + " (" + countryCode + ")\nIP : " + ip + "\nConnecte Windscribe sur France puis revérifie.");
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
                    if (body.length() > 200) body = body.substring(0, 200) + "…";
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
                if (!finalInternetOk) conclusion = "\n\n➡ Windscribe coupe ou perturbe Internet.";
                else if (!finalFranceOk) conclusion = "\n\n➡ Windscribe ne sort pas en France.";
                else if (!finalM6Ok) conclusion = "\n\n➡ France OK mais M6 refuse cette sortie.";
                else conclusion = "\n\n➡ Réseau + France + site M6 sont joignables. Vérifie maintenant le pays M6 dans Chrome.";
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
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/131 Mobile Safari/537.36 PiiWii-M6-Test/0.1.4");
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

    private void openPackageOrStore(String packageName, String playUrl, String label) {
        try {
            Intent launch = getPackageManager().getLaunchIntentForPackage(packageName);
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launch);
                return;
            }
        } catch (Exception ignored) {
        }

        Toast.makeText(this, label + " n’est pas installé. Ouverture de sa fiche.", Toast.LENGTH_LONG).show();
        try {
            Intent market = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
            market.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(market);
        } catch (Exception e) {
            openExternal(playUrl);
        }
    }

    private void openInChrome(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setPackage(CHROME_PACKAGE);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Chrome non disponible : ouverture du navigateur système.", Toast.LENGTH_SHORT).show();
            openExternal(url);
        }
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
