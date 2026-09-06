package ch.piiwii.gtigps;

import android.Manifest;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_LOCATION = 41;
    private LinearLayout root;
    private TextView diagnostics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDiagnostics();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF090909);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(22, 18, 22, 32);
        scroll.addView(root);

        TextView title = text("GTI GPS WIDGET", 25, Color.WHITE, true);
        root.addView(title);
        TextView subtitle = text("1.1.0 · vraie carte routière · TS18 / Topway", 14, 0xFFD71920, true);
        root.addView(subtitle);
        addSpacer(16);

        TextView architecture = text(
                "Carte : MapLibre Native 13.4.1 OpenGL\n" +
                "Données : OpenFreeMap / OpenStreetMap\n" +
                "Panneau : 380 × 350 px · x=330 · y=47\n" +
                "Navigation : non implémentée en 1.1.0 (volontaire)", 15, 0xFFE0E0E0, false);
        architecture.setLineSpacing(4f, 1f);
        root.addView(architecture);
        addSpacer(14);

        Button location = button("AUTORISER LA LOCALISATION");
        location.setOnClickListener(v -> requestLocation());
        root.addView(location);

        Button overlay = button("AUTORISER L’AFFICHAGE DU PANNEAU");
        overlay.setOnClickListener(v -> requestOverlay());
        root.addView(overlay);

        Button start = button("AFFICHER LA CARTE 380 × 350");
        start.setOnClickListener(v -> startPanel());
        root.addView(start);

        Button stop = button("MASQUER LA CARTE");
        stop.setOnClickListener(v -> sendAction(ActionReceiver.ACTION_HIDE));
        root.addView(stop);

        addSection("MODE DE CARTE");
        LinearLayout themeRow = row();
        themeRow.addView(smallButton("AUTO", v -> setTheme("auto")), weight());
        themeRow.addView(smallButton("JOUR", v -> setTheme("day")), weight());
        themeRow.addView(smallButton("NUIT", v -> setTheme("night")), weight());
        root.addView(themeRow);

        addSection("ORIENTATION");
        LinearLayout orientationRow = row();
        orientationRow.addView(smallButton("VÉHICULE", v -> setOrientation("vehicle")), weight());
        orientationRow.addView(smallButton("NORD", v -> setOrientation("north")), weight());
        root.addView(orientationRow);

        CheckBox resume = new CheckBox(this);
        resume.setText("Réafficher après redémarrage si le panneau était activé");
        resume.setTextColor(0xFFD0D0D0);
        resume.setChecked(GpsState.prefs(this).getBoolean(GpsState.KEY_AUTO_RESUME, false));
        resume.setOnCheckedChangeListener((buttonView, isChecked) ->
                GpsState.prefs(this).edit().putBoolean(GpsState.KEY_AUTO_RESUME, isChecked).apply());
        root.addView(resume);

        addSection("DIAGNOSTIC TS18");
        diagnostics = text("", 14, 0xFFEAEAEA, false);
        diagnostics.setLineSpacing(5f, 1f);
        root.addView(diagnostics);

        Button refresh = button("ACTUALISER LE DIAGNOSTIC");
        refresh.setOnClickListener(v -> refreshDiagnostics());
        root.addView(refresh);

        TextView warning = text(
                "Important : le composant AppWidget classique reste un RemoteViews. " +
                "La carte interactive est rendue par le panneau externe de cette même APK. " +
                "Aucune modification du Launcher GTI n’est faite dans cette version.",
                13, 0xFFAAAAAA, false);
        warning.setPadding(0, 18, 0, 0);
        root.addView(warning);

        setContentView(scroll);
    }

    private void requestLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
        }
    }

    private void requestOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    private void startPanel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            requestOverlay();
            return;
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocation();
            return;
        }
        sendAction(ActionReceiver.ACTION_SHOW);
    }

    private void sendAction(String action) {
        Intent i = new Intent(this, MapPanelService.class).setAction(action);
        if (ActionReceiver.ACTION_HIDE.equals(action)) {
            stopService(i);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(i);
        } else {
            startService(i);
        }
    }

    private void setTheme(String theme) {
        GpsState.prefs(this).edit().putString(GpsState.KEY_THEME, theme).apply();
        sendAction(ActionReceiver.ACTION_REFRESH);
        refreshDiagnostics();
    }

    private void setOrientation(String orientation) {
        GpsState.prefs(this).edit().putString(GpsState.KEY_ORIENTATION, orientation).apply();
        sendAction(ActionReceiver.ACTION_REFRESH);
        refreshDiagnostics();
    }

    private void refreshDiagnostics() {
        if (diagnostics == null) return;
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean permission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean gps = false;
        try { gps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER); } catch (Exception ignored) {}
        boolean overlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
        boolean internet = false;
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getActiveNetworkInfo();
            internet = info != null && info.isConnected();
        } catch (Exception ignored) {}
        boolean panel = GpsState.prefs(this).getBoolean(GpsState.KEY_PANEL_ENABLED, false);
        int widgetCount = AppWidgetManager.getInstance(this).getAppWidgetIds(
                new ComponentName(this, GTIGpsWidgetProvider.class)).length;

        long lastTime = GpsState.prefs(this).getLong(GpsState.KEY_LAST_TIME, 0L);
        String last = "aucune";
        String accuracy = "—";
        String age = "—";
        if (lastTime > 0L) {
            double lat = Double.longBitsToDouble(GpsState.prefs(this).getLong(GpsState.KEY_LAST_LAT, 0L));
            double lon = Double.longBitsToDouble(GpsState.prefs(this).getLong(GpsState.KEY_LAST_LON, 0L));
            float acc = GpsState.prefs(this).getFloat(GpsState.KEY_LAST_ACC, -1f);
            last = String.format(Locale.US, "%.6f, %.6f", lat, lon);
            accuracy = acc >= 0 ? String.format(Locale.US, "%.0f m", acc) : "—";
            age = Math.max(0L, (System.currentTimeMillis() - lastTime) / 1000L) + " s";
        }

        String size = widgetCount > 0 ? readWidgetSize() : "aucun widget placé";
        diagnostics.setText(
                "Localisation autorisée : " + yn(permission) + "\n" +
                "GPS actif : " + yn(gps) + "\n" +
                "Dernière position : " + last + "\n" +
                "Précision GPS : " + accuracy + "\n" +
                "Âge de la position : " + age + "\n" +
                "Affichage par-dessus apps : " + yn(overlay) + "\n" +
                "Connexion Internet : " + yn(internet) + "\n" +
                "Fournisseur carte : OpenFreeMap / OSM\n" +
                "Moteur carte : MapLibre Native 13.4.1 OpenGL\n" +
                "Moteur itinéraire : aucun (phase 1)\n" +
                "Navigation active : non\n" +
                "Destination actuelle : aucune\n" +
                "Panneau carte actif : " + yn(panel) + "\n" +
                "Widget actif : " + (widgetCount > 0 ? "oui (" + widgetCount + ")" : "non") + "\n" +
                "Taille attribuée : " + size + "\n" +
                "Mode carte : " + GpsState.theme(this) + "\n" +
                "Orientation : " + GpsState.orientation(this) + "\n" +
                "Version : 1.1.0 (versionCode 2)"
        );
    }

    private String readWidgetSize() {
        AppWidgetManager awm = AppWidgetManager.getInstance(this);
        int[] ids = awm.getAppWidgetIds(new ComponentName(this, GTIGpsWidgetProvider.class));
        if (ids.length == 0) return "aucun";
        Bundle options = awm.getAppWidgetOptions(ids[0]);
        int minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, -1);
        int minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, -1);
        int maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, -1);
        int maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, -1);
        return minW + "×" + minH + " à " + maxW + "×" + maxH + " dp";
    }

    private String yn(boolean value) { return value ? "oui" : "non"; }

    private TextView text(String s, float sp, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        if (bold) tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return tv;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextColor(Color.WHITE);
        b.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        b.setBackgroundColor(0xFFD71920);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 54);
        lp.bottomMargin = 9;
        b.setLayoutParams(lp);
        return b;
    }

    private Button smallButton(String s, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextColor(Color.WHITE);
        b.setTextSize(13);
        b.setBackgroundColor(0xFF232323);
        b.setOnClickListener(listener);
        return b;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        return row;
    }

    private LinearLayout.LayoutParams weight() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, 50, 1f);
        p.setMargins(3, 0, 3, 0);
        return p;
    }

    private void addSection(String title) {
        TextView t = text(title, 14, 0xFFD71920, true);
        t.setPadding(0, 18, 0, 8);
        root.addView(t);
    }

    private void addSpacer(int px) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, px));
        root.addView(v);
    }
}
