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
        root.addView(text("2.1.0 · test accueil / Launcher · vraie application Google Maps", 14, 0xFFD71920, true));
        addSpacer(14);

        TextView explanation = text(
                "Le test ne passe plus par un sélecteur de zone. La zone est fixée automatiquement.\n\n" +
                "Le bouton du widget utilise maintenant une Activity transparente : Google Maps est lancé de la même manière que le test qui fonctionnait dans l'application, puis GTI GPS disparaît pour laisser l'écran d'accueil / le Launcher derrière Maps.",
                16, 0xFFE8E8E8, false);
        explanation.setLineSpacing(dp(3), 1f);
        root.addView(explanation);
        addSpacer(14);

        Button homeTest = button("TESTER GOOGLE MAPS SUR L'ACCUEIL");
        homeTest.setOnClickListener(v -> {
            startActivity(new Intent(this, WindowLaunchActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            finish();
            overridePendingTransition(0, 0);
        });
        root.addView(homeTest);

        Button full = darkButton("OUVRIR GOOGLE MAPS EN PLEIN ÉCRAN");
        full.setOnClickListener(v -> {
            GoogleMapsWindow.launchFullScreen(this);
            finish();
            overridePendingTransition(0, 0);
        });
        root.addView(full);

        CheckBox boot = new CheckBox(this);
        boot.setText("Réouvrir Google Maps après le démarrage de l'autoradio");
        boot.setTextColor(0xFFE0E0E0);
        boot.setTextSize(15);
        boot.setChecked(WindowPrefs.autoBoot(this));
        boot.setOnCheckedChangeListener((b, checked) -> WindowPrefs.setAutoBoot(this, checked));
        root.addView(boot);

        addSection("ZONE DE TEST AUTOMATIQUE");
        Rect r = WindowPrefs.getBounds(this);
        TextView zone = text(
                WindowPrefs.boundsSource(this) + "\n" +
                "x=" + r.left + " · y=" + r.top + " · " + r.width() + " × " + r.height() + " px\n" +
                "Aucun choix manuel dans cette version de test.",
                15, 0xFFE4E4E4, false);
        root.addView(zone);

        addSection("DIAGNOSTIC DE FENÊTRE");
        diagnostics = text("", 14, 0xFFE6E6E6, false);
        diagnostics.setLineSpacing(dp(2), 1f);
        root.addView(diagnostics);

        Button refresh = darkButton("ACTUALISER");
        refresh.setOnClickListener(v -> refreshDiagnostics());
        root.addView(refresh);

        TextView note = text(
                "But de ce test : vérifier que la vraie fenêtre Google Maps reste visible une fois GTI GPS fermé et que l'écran d'accueil / le Launcher est bien derrière. Ensuite, le Launcher GTI pourra fournir automatiquement les coordonnées exactes de sa zone GPS.",
                13, 0xFFAAAAAA, false);
        note.setPadding(0, dp(12), 0, 0);
        root.addView(note);

        setContentView(scroll);
    }

    private void refreshDiagnostics() {
        if (diagnostics == null) return;
        DisplayMetrics dm = getResources().getDisplayMetrics();
        Rect r = WindowPrefs.getBounds(this);
        diagnostics.setText(
                "Google Maps installé : " + yn(GoogleMapsWindow.isInstalled(this)) + "\n" +
                "Freeform déclaré par Android : " + yn(GoogleMapsWindow.hasFreeformFeature(this)) + "\n" +
                "Écran : " + dm.widthPixels + " × " + dm.heightPixels + " px\n" +
                "Densité : " + dm.density + "\n" +
                "Zone : x=" + r.left + " · y=" + r.top + " · " + r.width() + " × " + r.height() + " px\n" +
                "Source zone : " + WindowPrefs.boundsSource(this) + "\n" +
                "Dernier lancement : " + WindowPrefs.lastMode(this) + "\n" +
                "Version : 2.1.0 (versionCode 6)\n" +
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
