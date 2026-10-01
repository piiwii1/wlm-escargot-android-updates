package ch.piiwii.mapsnavprobe;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class WidgetSettingsActivity extends Activity {
    private static final String PREF_IDLE_ART_ENABLED = "idle_art_enabled";
    private static final int BG = Color.rgb(4, 8, 13);
    private static final int TEXT = Color.rgb(248, 251, 255);
    private static final int MUTED = Color.rgb(128, 149, 168);
    private static final int ACCENT = Color.rgb(119, 238, 255);

    private SharedPreferences prefs;
    private TextView stateView;
    private TextView descriptionView;
    private Button toggleButton;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        prefs = getSharedPreferences(MapsNotificationListener.PREFS, MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(22), dp(20), dp(20));
        root.setBackgroundColor(BG);

        TextView title = text("Paramètres du widget", 27, true, TEXT);
        root.addView(title);

        TextView subtitle = text("PiiWii Nav · Launcher GTI", 12, false, MUTED);
        subtitle.setPadding(0, dp(4), 0, dp(18));
        root.addView(subtitle);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(15), dp(14), dp(15), dp(14));
        card.setBackground(rounded(Color.rgb(7, 16, 24), 18, Color.rgb(31, 66, 87)));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView settingTitle = text("Image d’attente", 15, true, TEXT);
        header.addView(settingTitle, new LinearLayout.LayoutParams(0, -2, 1f));

        stateView = text("", 12, true, ACCENT);
        header.addView(stateView);
        card.addView(header);

        descriptionView = text("", 12, false, MUTED);
        descriptionView.setPadding(0, dp(8), 0, 0);
        descriptionView.setLineSpacing(dp(2), 1f);
        card.addView(descriptionView);

        toggleButton = button("");
        toggleButton.setOnClickListener(v -> {
            boolean enabled = isIdleArtworkEnabled();
            prefs.edit().putBoolean(PREF_IDLE_ART_ENABLED, !enabled).apply();
            MapsNavWidget.updateAll(this);
            refreshSetting();
        });
        LinearLayout.LayoutParams toggleLp = new LinearLayout.LayoutParams(-1, dp(48));
        toggleLp.setMargins(0, dp(15), 0, 0);
        card.addView(toggleButton, toggleLp);

        root.addView(card, new LinearLayout.LayoutParams(-1, -2));

        TextView note = text("Pendant un trajet Google Maps, ce réglage n’a aucun effet : les consignes de navigation restent affichées normalement.", 11, false, MUTED);
        note.setPadding(dp(2), dp(14), dp(2), 0);
        root.addView(note);

        Button close = secondaryButton("Fermer");
        close.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(-1, dp(46));
        closeLp.setMargins(0, dp(18), 0, 0);
        root.addView(close, closeLp);

        setContentView(root);
        refreshSetting();
    }

    private boolean isIdleArtworkEnabled() {
        return prefs.getBoolean(PREF_IDLE_ART_ENABLED, true);
    }

    private void refreshSetting() {
        boolean enabled = isIdleArtworkEnabled();
        stateView.setText(enabled ? "ACTIVÉE" : "DÉSACTIVÉE");
        stateView.setTextColor(enabled ? ACCENT : Color.rgb(255, 194, 101));
        descriptionView.setText(enabled
                ? "Sans trajet actif, le widget affiche l’image prédéfinie."
                : "Sans trajet actif, le widget affiche la flèche bleue avec « En attente d’un trajet ».");
        toggleButton.setText(enabled ? "Désactiver l’image" : "Activer l’image");
    }

    private TextView text(String value, float size, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setAllCaps(false);
        b.setTextColor(Color.rgb(3, 17, 25));
        b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(132, 243, 255), Color.rgb(18, 126, 226)});
        g.setCornerRadius(dp(15));
        g.setStroke(dp(1), Color.rgb(168, 247, 255));
        b.setBackground(g);
        return b;
    }

    private Button secondaryButton(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setAllCaps(false);
        b.setTextColor(Color.rgb(207, 233, 248));
        b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(13, 29, 42), Color.rgb(7, 16, 24)});
        g.setCornerRadius(dp(15));
        g.setStroke(dp(1), Color.rgb(35, 72, 96));
        b.setBackground(g);
        return b;
    }

    private GradientDrawable rounded(int fill, int radiusDp, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        g.setStroke(dp(1), stroke);
        return g;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
