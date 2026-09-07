package ch.piiwii.gtialtimeter;

import android.Manifest;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_LOCATION = 100;
    private LinearLayout diagnostics;
    private ImageView preview;
    private TextView liveAltitude;
    private TextView liveStatus;
    private Button skinPremium;
    private Button skinSilhouette;
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        @Override public void run() { renderLive(); handler.postDelayed(this, 1000L); }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        if (!hasFineLocation()) requestLocation(); else AltitudeService.startIfPermitted(this);
    }

    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(refresh);
        handler.post(refresh);
        if (hasFineLocation()) AltitudeService.startIfPermitted(this);
    }

    @Override protected void onPause() {
        handler.removeCallbacks(refresh);
        super.onPause();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(8,9,11));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(16), dp(18), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("GTI ALTIMETER", 25, Color.WHITE, true);
        top.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView version = pill("v" + BuildConfig.VERSION_NAME, Color.rgb(225,20,22));
        top.addView(version);
        root.addView(top);

        TextView subtitle = text("Cervin · boussole · GPS · précision", 13, Color.rgb(165,168,175), false);
        subtitle.setPadding(0, dp(2), 0, dp(12));
        root.addView(subtitle);

        preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.setAdjustViewBounds(true);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(-1, dp(185));
        previewParams.setMargins(0, 0, 0, dp(12));
        root.addView(preview, previewParams);

        TextView skinTitle = text("SKIN DU WIDGET", 14, Color.rgb(235,35,38), true);
        skinTitle.setPadding(0, 0, 0, dp(6));
        root.addView(skinTitle);

        LinearLayout skins = new LinearLayout(this);
        skins.setOrientation(LinearLayout.HORIZONTAL);
        skins.setPadding(0, 0, 0, dp(12));
        skinPremium = button("Skin 1 · Premium");
        skinPremium.setOnClickListener(v -> selectSkin(SkinPrefs.PREMIUM));
        skins.addView(skinPremium, new LinearLayout.LayoutParams(0, dp(46), 1f));
        skinSilhouette = button("Skin 2 · Silhouette");
        skinSilhouette.setOnClickListener(v -> selectSkin(SkinPrefs.SILHOUETTE));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        sp.setMargins(dp(8),0,0,0);
        skins.addView(skinSilhouette, sp);
        root.addView(skins);
        updateSkinButtons();

        LinearLayout hero = card();
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        liveAltitude = text("Recherche…", 28, Color.WHITE, true);
        hero.addView(liveAltitude, new LinearLayout.LayoutParams(0, dp(62), .52f));
        liveStatus = text("Recherche GPS…", 13, Color.rgb(185,188,195), false);
        liveStatus.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        hero.addView(liveStatus, new LinearLayout.LayoutParams(0, dp(62), .48f));
        root.addView(hero);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(12), 0, dp(8));
        Button permission = button("Autoriser GPS");
        permission.setOnClickListener(v -> requestLocation());
        actions.addView(permission, new LinearLayout.LayoutParams(0, dp(46), 1f));
        Button relaunch = button("Relancer service");
        relaunch.setOnClickListener(v -> AltitudeService.startIfPermitted(this));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, dp(46), 1f); bp.setMargins(dp(8),0,0,0);
        actions.addView(relaunch, bp);
        root.addView(actions);

        Button gpsSettings = button("Paramètres de localisation Android");
        gpsSettings.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)));
        root.addView(gpsSettings, new LinearLayout.LayoutParams(-1, dp(46)));

        TextView h = text("DIAGNOSTIC EN DIRECT", 15, Color.rgb(235,35,38), true);
        h.setPadding(0, dp(18), 0, dp(8));
        root.addView(h);
        diagnostics = card();
        diagnostics.setOrientation(LinearLayout.VERTICAL);
        diagnostics.setPadding(dp(12), dp(4), dp(12), dp(4));
        root.addView(diagnostics);

        TextView note = text("Altitude : GPS WGS84 filtré en priorité. La v1.2.2 maintient le service par watchdog et utilise le baromètre comme secours relatif après calibration GPS. La boussole matérielle reste prioritaire à l’arrêt ; en mouvement, le cap GPS est prioritaire.", 12, Color.rgb(165,168,175), false);
        note.setPadding(0, dp(14), 0, 0);
        root.addView(note);
        setContentView(scroll);
    }

    private void selectSkin(int skin) {
        SkinPrefs.set(this, skin);
        updateSkinButtons();
        GtiWidgetUpdater.updateAll(this);
        renderLive();
    }

    private void updateSkinButtons() {
        if (skinPremium == null || skinSilhouette == null) return;
        boolean silhouette = SkinPrefs.get(this) == SkinPrefs.SILHOUETTE;
        skinPremium.setText(silhouette ? "Skin 1 · Premium" : "✓ Skin 1 · Premium");
        skinSilhouette.setText(silhouette ? "✓ Skin 2 · Silhouette" : "Skin 2 · Silhouette");
    }

    private void renderLive() {
        updateSkinButtons();
        if (preview != null) preview.setImageBitmap(WidgetRenderer.render(this, 328, 184));
        double altitude = AltitudeState.displayAltitude(this);
        boolean valid = hasFineLocation() && !Double.isNaN(altitude);
        if (liveAltitude != null) liveAltitude.setText(valid ? Math.round(altitude) + " m" : "Recherche…");
        if (liveStatus != null) {
            String precision;
            if (AltitudeState.isGpsFresh(this)) {
                float acc = !Float.isNaN(AltitudeState.acceptedVAcc(this)) ? AltitudeState.acceptedVAcc(this) : AltitudeState.acceptedHAcc(this);
                precision = Float.isNaN(acc) ? "Préc. —" : "±" + Math.max(1, Math.round(acc)) + " m";
            } else if (AltitudeState.isBaroFresh(this)) {
                precision = "BARO";
            } else {
                precision = "Préc. —";
            }
            String cap = Float.isNaN(AltitudeState.heading(this)) ? "cap —" : Math.round(AltitudeState.heading(this)) + "° " + cardinal(AltitudeState.heading(this));
            liveStatus.setText(AltitudeState.displaySource(this) + " · " + (AltitudeState.serviceAlive(this) ? "service OK" : "service à relancer") + "\n" + precision + " · " + cap);
        }
        renderDiagnostics();
    }

    private void renderDiagnostics() {
        if (diagnostics == null) return;
        diagnostics.removeAllViews();
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean gps = false; try { gps = lm != null && lm.isProviderEnabled(LocationManager.GPS_PROVIDER); } catch (Exception ignored) { }
        int[] widgets = AppWidgetManager.getInstance(this).getAppWidgetIds(new ComponentName(this, GTIAltimeterWidgetProvider.class));

        addRow("Permission localisation", hasFineLocation() ? "OK" : "KO", hasFineLocation());
        addRow("GPS activé", gps ? "OUI" : "NON", gps);
        addRow("Position reçue", AltitudeState.positionReceived(this) ? "OUI" : "NON", AltitudeState.positionReceived(this));
        addRow("Latitude / longitude", coord(AltitudeState.lat(this)) + " / " + coord(AltitudeState.lon(this)), null);
        addRow("Précision horizontale", meters(AltitudeState.hAcc(this)), null);
        addRow("Précision verticale", meters(AltitudeState.vAcc(this)), null);
        addRow("Précision fix accepté", meters(!Float.isNaN(AltitudeState.acceptedVAcc(this)) ? AltitudeState.acceptedVAcc(this) : AltitudeState.acceptedHAcc(this)), null);
        addRow("Altitude GPS brute", meters(AltitudeState.raw(this)), null);
        addRow("Altitude GPS filtrée", meters(AltitudeState.filtered(this)), null);
        addRow("Altitude barométrique", meters(AltitudeState.baroAltitude(this)), null);
        addRow("Altitude affichée / finale", meters(AltitudeState.displayAltitude(this)), null);
        addRow("Tendance 30 s", signedMeters(AltitudeState.trend(this)), null);
        addRow("Âge dernière trame GPS", age(AltitudeState.rawFixTime(this)), null);
        addRow("Âge dernier fix accepté", age(AltitudeState.fixTime(this)), null);
        addRow("Service altitude", AltitudeState.serviceAlive(this) ? "ACTIF" : "À RELANCER", AltitudeState.serviceAlive(this));
        addRow("Échantillon rejeté", AltitudeState.lastRejected(this) ? "OUI · " + AltitudeState.rejectReason(this) : "NON", !AltitudeState.lastRejected(this));
        addRow("Boussole matérielle", AltitudeState.compassSensors(this) ? "OUI" : "NON", null);
        addRow("Cap", Float.isNaN(AltitudeState.heading(this)) ? "—" : Math.round(AltitudeState.heading(this)) + "° " + cardinal(AltitudeState.heading(this)), null);
        addRow("Source du cap", AltitudeState.headingSource(this), null);
        addRow("Baromètre présent", AltitudeState.barometerPresent(this) ? "OUI" : "NON", null);
        addRow("Pression actuelle", Float.isNaN(AltitudeState.pressure(this)) ? "—" : String.format(Locale.US, "%.1f hPa", AltitudeState.pressure(this)), null);
        addRow("Baromètre calibré", AltitudeState.baroCalibrated(this) ? "OUI" : "NON", AltitudeState.baroCalibrated(this));
        addRow("Source altitude", AltitudeState.displaySource(this), null);
        addRow("Skin widget", SkinPrefs.label(this), null);
        addRow("Widget actif", widgets.length > 0 ? "OUI · " + widgets.length : "NON", widgets.length > 0);
        addRow("Taille réelle widget", AltitudeState.widgetWidth(this) + " × " + AltitudeState.widgetHeight(this) + " dp", null);
        addRow("Dernière mise à jour", date(AltitudeState.lastUpdate(this)), null);
        addRow("Version", BuildConfig.VERSION_NAME + " · code " + BuildConfig.VERSION_CODE, null);
        addRow("Package", getPackageName(), null);
        addRow("Provider", GTIAltimeterWidgetProvider.class.getName(), null);
    }

    private void addRow(String key, String value, Boolean good) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));
        TextView k = text(key, 13, Color.rgb(160,163,170), false);
        TextView v = text(value, 13, good == null ? Color.WHITE : (good ? Color.rgb(125,220,145) : Color.rgb(255,112,105)), true);
        v.setGravity(Gravity.END);
        row.addView(k, new LinearLayout.LayoutParams(0, -2, 1.05f));
        row.addView(v, new LinearLayout.LayoutParams(0, -2, 0.95f));
        diagnostics.addView(row);
        View line = new View(this); line.setBackgroundColor(Color.rgb(42,44,49));
        diagnostics.addView(line, new LinearLayout.LayoutParams(-1, 1));
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.rgb(22,23,27), Color.rgb(11,12,15)});
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), Color.rgb(58,60,66));
        l.setBackground(bg);
        l.setPadding(dp(12), dp(6), dp(12), dp(6));
        return l;
    }

    private TextView pill(String s, int color) {
        TextView t = text(s, 12, Color.WHITE, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(9), dp(4), dp(9), dp(4));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color); bg.setCornerRadius(dp(12));
        t.setBackground(bg);
        return t;
    }

    private boolean hasFineLocation() {
        return Build.VERSION.SDK_INT < 23 || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    private void requestLocation() {
        if (Build.VERSION.SDK_INT >= 23 && !hasFineLocation()) requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
        else AltitudeService.startIfPermitted(this);
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION && hasFineLocation()) AltitudeService.startIfPermitted(this);
        GtiWidgetUpdater.updateAll(this);
        renderLive();
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        return t;
    }
    private Button button(String s) {
        Button b = new Button(this); b.setText(s); b.setTextColor(Color.WHITE); b.setTextSize(13); b.setAllCaps(false);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(35,36,41)); bg.setCornerRadius(dp(9)); bg.setStroke(dp(1), Color.rgb(65,67,73));
        b.setBackground(bg); return b;
    }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private static String coord(double v) { return Double.isNaN(v) ? "—" : String.format(Locale.US, "%.6f", v); }
    private static String meters(double v) { return Double.isNaN(v) ? "—" : String.format(Locale.US, "%.1f m", v); }
    private static String meters(float v) { return Float.isNaN(v) ? "—" : String.format(Locale.US, "%.1f m", v); }
    private static String signedMeters(double v) { return Double.isNaN(v) ? "—" : String.format(Locale.US, "%+.1f m", v); }
    private static String age(long t) { if (t <= 0) return "—"; long s = Math.max(0, (System.currentTimeMillis()-t)/1000); return s + " s"; }
    private static String date(long t) { return t <= 0 ? "—" : new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(new Date(t)); }
    private static String cardinal(float d) {
        if (Float.isNaN(d)) return "—";
        String[] dirs = {"N","NE","E","SE","S","SO","O","NO"};
        return dirs[Math.round(d / 45f) & 7];
    }
}
