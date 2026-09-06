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
    public static final String ACTION_REQUEST_SHOW_MAP = "ch.piiwii.gtigps.REQUEST_SHOW_MAP";
    private static final int REQ_LOCATION = 41;

    private LinearLayout root;
    private TextView diagnostics;
    private TextView permissionHint;
    private boolean pendingShowMap;
    private boolean overlaySettingsOpened;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pendingShowMap = ACTION_REQUEST_SHOW_MAP.equals(getIntent() != null ? getIntent().getAction() : null);
        buildUi();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && ACTION_REQUEST_SHOW_MAP.equals(intent.getAction())) {
            pendingShowMap = true;
            overlaySettingsOpened = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDiagnostics();
        GTIGpsWidgetProvider.updateAll(this);
        if (pendingShowMap) continueShowFlow();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            refreshDiagnostics();
            if (pendingShowMap && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                continueShowFlow();
            }
        }
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF090909);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(28));
        scroll.addView(root);

        TextView title = text("GTI GPS WIDGET", 25, Color.WHITE, true);
        root.addView(title);
        TextView subtitle = text(BuildConfig.VERSION_NAME + " · vraie carte routière · TS18 / Topway", 14, 0xFFD71920, true);
        root.addView(subtitle);
        addSpacer(14);

        TextView architecture = text(
                "Carte : MapLibre Native 13.4.1 OpenGL\n" +
                "Données : OpenFreeMap / OpenStreetMap\n" +
                "Panneau cible TS18 : 380 × 350 px · x=330 · y=47\n" +
                "Navigation : non implémentée dans cette phase", 15, 0xFFE0E0E0, false);
        architecture.setLineSpacing(dp(2), 1f);
        root.addView(architecture);
        addSpacer(12);

        permissionHint = text("", 14, 0xFFFFC107, true);
        permissionHint.setPadding(dp(10), dp(8), dp(10), dp(10));
        root.addView(permissionHint);

        Button location = button("AUTORISER LA LOCALISATION");
        location.setOnClickListener(v -> requestLocation());
        root.addView(location);

        Button overlay = button("AUTORISER L’AFFICHAGE DU PANNEAU");
        overlay.setOnClickListener(v -> openOverlaySettings());
        root.addView(overlay);

        Button start = button("AFFICHER LA CARTE 380 × 350");
        start.setOnClickListener(v -> {
            pendingShowMap = true;
            overlaySettingsOpened = false;
            continueShowFlow();
        });
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
        resume.setTextSize(14);
        resume.setPadding(0, dp(4), 0, dp(4));
        resume.setChecked(GpsState.prefs(this).getBoolean(GpsState.KEY_AUTO_RESUME, false));
        resume.setOnCheckedChangeListener((buttonView, isChecked) ->
                GpsState.prefs(this).edit().putBoolean(GpsState.KEY_AUTO_RESUME, isChecked).apply());
        root.addView(resume);

        addSection("DIAGNOSTIC TS18");
        diagnostics = text("", 14, 0xFFEAEAEA, false);
        diagnostics.setLineSpacing(dp(2), 1f);
        root.addView(diagnostics);

        Button refresh = button("ACTUALISER LE DIAGNOSTIC");
        refresh.setOnClickListener(v -> refreshDiagnostics());
        root.addView(refresh);

        TextView warning = text(
                "Le widget Android classique reste un RemoteViews. Sur Android 10/API 29, la carte interactive est rendue par le panneau externe de cette même APK. " +
                "Sur le TS18, ce panneau se place aux coordonnées réservées 330/47 dans la zone 380 × 350.",
                13, 0xFFAAAAAA, false);
        warning.setPadding(0, dp(14), 0, 0);
        root.addView(warning);

        setContentView(scroll);
    }

    private void requestLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
        }
    }

    private void openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    private void continueShowFlow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            if (!overlaySettingsOpened) {
                overlaySettingsOpened = true;
                openOverlaySettings();
            }
            return;
        }
        overlaySettingsOpened = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocation();
            return;
        }

        pendingShowMap = false;
        sendAction(ActionReceiver.ACTION_SHOW);
        refreshDiagnostics();

        if (ACTION_REQUEST_SHOW_MAP.equals(getIntent() != null ? getIntent().getAction() : null)) {
            finish();
        }
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
        boolean permission = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
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

        if (permissionHint != null) {
            if (!overlay) {
                permissionHint.setTextColor(0xFFFFC107);
                permissionHint.setText("⚠ Autorisation requise : « Afficher par-dessus les autres applications ». Sans elle, la carte ne peut pas apparaître.");
                permissionHint.setVisibility(View.VISIBLE);
            } else if (!permission) {
                permissionHint.setTextColor(0xFFFFC107);
                permissionHint.setText("⚠ Autorisation de localisation requise pour afficher la position GPS.");
                permissionHint.setVisibility(View.VISIBLE);
            } else {
                permissionHint.setText("✓ Autorisations principales OK. La carte peut être démarrée.");
                permissionHint.setTextColor(0xFF8BC34A);
                permissionHint.setVisibility(View.VISIBLE);
            }
        }

        long lastTime = GpsState.prefs(this).getLong(GpsState.KEY_LAST_TIME, 0L);
        String last = panel ? "en attente" : "aucune (panneau non démarré)";
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
                "Affichage par-dessus apps : " + yn(overlay) + (overlay ? "" : "  ← requis") + "\n" +
                "Connexion Internet : " + yn(internet) + "\n" +
                "Fournisseur carte : OpenFreeMap / OSM\n" +
                "Moteur carte : MapLibre Native 13.4.1 OpenGL\n" +
                "Moteur itinéraire : aucun (phase 1)\n" +
                "Navigation active : non\n" +
                "Destination actuelle : aucune\n" +
                "Panneau carte actif : " + yn(panel) + "\n" +
                "Widget actif : " + (widgetCount > 0 ? "oui (" + widgetCount + ")" : "non") + "\n" +
                "Taille attribuée par le launcher : " + size + "\n" +
                "Taille panneau cible TS18 : 380×350 px\n" +
                "Mode carte : " + GpsState.theme(this) + "\n" +
                "Orientation : " + GpsState.orientation(this) + "\n" +
                "Version : " + BuildConfig.VERSION_NAME + " (versionCode " + BuildConfig.VERSION_CODE + ")"
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
        b.setTextSize(14);
        b.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(dp(10), 0, dp(10), 0);
        b.setBackgroundColor(0xFFD71920);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        lp.bottomMargin = dp(8);
        b.setLayoutParams(lp);
        return b;
    }

    private Button smallButton(String s, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextColor(Color.WHITE);
        b.setTextSize(13);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(dp(6), 0, dp(6), 0);
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
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(48), 1f);
        p.setMargins(dp(2), 0, dp(2), 0);
        return p;
    }

    private void addSection(String title) {
        TextView t = text(title, 14, 0xFFD71920, true);
        t.setPadding(0, dp(16), 0, dp(7));
        root.addView(t);
    }

    private void addSpacer(int valueDp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(valueDp)));
        root.addView(v);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
