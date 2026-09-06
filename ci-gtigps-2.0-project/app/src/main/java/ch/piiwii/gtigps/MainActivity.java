package ch.piiwii.gtigps;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
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
        GTIGpsWidgetProvider.updateAll(this);
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF090909);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(28));
        scroll.addView(root);

        root.addView(text("GTI GPS · GOOGLE MAPS WINDOW", 24, Color.WHITE, true));
        root.addView(text("2.0.0 · vraie application Google Maps · TS18 / Topway", 14, 0xFFD71920, true));
        addSpacer(14);

        TextView explanation = text(
                "Cette version ne fabrique plus son propre GPS. Elle lance Google Maps lui-même dans une fenêtre redimensionnée placée exactement sur la zone du widget.\n\n" +
                "La recherche, les favoris, le trafic, les itinéraires et le guidage restent 100 % Google Maps.",
                16, 0xFFE8E8E8, false);
        explanation.setLineSpacing(dp(3), 1f);
        root.addView(explanation);
        addSpacer(14);

        Button calibrate = button("1 · CALIBRER LA ZONE DU WIDGET");
        calibrate.setOnClickListener(v -> startActivity(new Intent(this, CalibrationActivity.class)));
        root.addView(calibrate);

        Button openWindow = button("2 · OUVRIR GOOGLE MAPS DANS LA ZONE");
        openWindow.setOnClickListener(v -> showResult(GoogleMapsWindow.launchWindow(this)));
        root.addView(openWindow);

        Button full = darkButton("OUVRIR GOOGLE MAPS EN PLEIN ÉCRAN");
        full.setOnClickListener(v -> showResult(GoogleMapsWindow.launchFullScreen(this)));
        root.addView(full);

        CheckBox boot = new CheckBox(this);
        boot.setText("Réouvrir Google Maps dans la zone après le démarrage de l’autoradio");
        boot.setTextColor(0xFFE0E0E0);
        boot.setTextSize(15);
        boot.setChecked(WindowPrefs.autoBoot(this));
        boot.setOnCheckedChangeListener((b, checked) -> WindowPrefs.setAutoBoot(this, checked));
        root.addView(boot);

        addSection("DIAGNOSTIC DE FENÊTRE");
        diagnostics = text("", 14, 0xFFE6E6E6, false);
        diagnostics.setLineSpacing(dp(2), 1f);
        root.addView(diagnostics);

        Button refresh = darkButton("ACTUALISER");
        refresh.setOnClickListener(v -> refreshDiagnostics());
        root.addView(refresh);

        TextView note = text(
                "Important : Android ne permet pas d’intégrer l’interface d’une autre application dans un AppWidget classique. " +
                "GTI GPS utilise donc le mode fenêtre/freeform du système pour que Google Maps soit réellement exécuté au-dessus de la zone du widget. " +
                "Sur un firmware qui bloque le freeform, l’app retombe automatiquement sur Google Maps plein écran.",
                13, 0xFFAAAAAA, false);
        note.setPadding(0, dp(12), 0, 0);
        root.addView(note);

        setContentView(scroll);
    }

    private void showResult(GoogleMapsWindow.LaunchResult r) {
        Toast.makeText(this, r.message, Toast.LENGTH_LONG).show();
        refreshDiagnostics();
    }

    private void refreshDiagnostics() {
        if (diagnostics == null) return;
        DisplayMetrics dm = getResources().getDisplayMetrics();
        Rect r = WindowPrefs.getBounds(this);
        diagnostics.setText(
                "Google Maps installé : " + yn(GoogleMapsWindow.isInstalled(this)) + "\n" +
                "Écran : " + dm.widthPixels + " × " + dm.heightPixels + " px\n" +
                "Densité : " + dm.density + "\n" +
                "Zone calibrée : x=" + r.left + " · y=" + r.top + " · " + r.width() + " × " + r.height() + " px\n" +
                "Calibration faite : " + yn(WindowPrefs.isConfigured(this)) + "\n" +
                "Dernier lancement : " + WindowPrefs.lastMode(this) + "\n" +
                "Version : 2.0.0 (versionCode 5)\n" +
                "Package : ch.piiwii.gtigps"
        );
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
        b.setPadding(dp(8), 0, dp(8), 0);
        b.setBackgroundColor(0xFFD71920);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        lp.bottomMargin = dp(8);
        b.setLayoutParams(lp);
        return b;
    }

    private Button darkButton(String s) {
        Button b = button(s);
        b.setBackgroundColor(0xFF242424);
        return b;
    }

    private void addSection(String title) {
        TextView t = text(title, 14, 0xFFD71920, true);
        t.setPadding(0, dp(14), 0, dp(7));
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
