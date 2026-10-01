package ch.piiwii.mapsnavprobe;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(4, 8, 13);
    private static final int TEXT = Color.rgb(248, 251, 255);
    private static final int MUTED = Color.rgb(128, 149, 168);
    private static final int ACCENT = Color.rgb(119, 238, 255);
    private static final int ACCENT_DARK = Color.rgb(18, 126, 226);

    private static final String PREF_IDLE_ART_ENABLED = "idle_art_enabled";

    private TextView accessStatus, distanceView, instruction, detail, tripSummaryView, meta, liveStatus, raw;
    private TextView idleArtValue, idleArtDescription;
    private ImageView arrowView;
    private Button diagnosticButton, clearButton, idleArtToggleButton;
    private int testIndex = 0;
    private boolean diagnosticVisible = false;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { refresh(); }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        configureSystemBars();

        ScrollView screen = new ScrollView(this);
        screen.setFillViewport(true);
        screen.setBackgroundColor(BG);
        screen.setClipToPadding(false);
        screen.setVerticalScrollBarEnabled(true);
        applySystemBarInsets(screen);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, 0, 0, dp(10));
        screen.addView(content, new ScrollView.LayoutParams(-1, -2));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(14));

        LinearLayout headerText = new LinearLayout(this);
        headerText.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("PiiWii Nav", 29, true, TEXT);
        TextView intro = text("Google Maps → Launcher GTI", 12, false, MUTED);
        intro.setPadding(0, dp(3), 0, 0);
        headerText.addView(title);
        headerText.addView(intro);
        header.addView(headerText, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView version = text("v1.15", 11, true, ACCENT);
        version.setGravity(Gravity.CENTER);
        version.setPadding(dp(11), dp(6), dp(11), dp(6));
        version.setBackground(rounded(Color.rgb(8, 28, 42), 14, Color.rgb(35, 93, 121)));
        header.addView(version);
        content.addView(header);

        accessStatus = text("", 13, true, MUTED);
        accessStatus.setPadding(dp(14), dp(11), dp(14), dp(11));
        accessStatus.setBackground(rounded(Color.rgb(8, 20, 29), 17, Color.rgb(30, 60, 79)));
        content.addView(accessStatus, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(10), 0, dp(13));
        Button permission = button("Notifications");
        permission.setOnClickListener(v -> openNotificationAccess());
        Button maps = button("Ouvrir Google Maps");
        maps.setOnClickListener(v -> openMaps());
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, dp(46), 1f);
        a.setMarginEnd(dp(5));
        LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(0, dp(46), 1f);
        b.setMarginStart(dp(5));
        actions.addView(permission, a);
        actions.addView(maps, b);
        content.addView(actions);

        TextView navLabel = sectionLabel("NAVIGATION EN DIRECT");
        navLabel.setPadding(dp(2), 0, 0, dp(7));
        content.addView(navLabel);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(15), dp(13), dp(15), dp(14));
        card.setBackground(gradientCard());

        LinearLayout metaRow = new LinearLayout(this);
        metaRow.setOrientation(LinearLayout.HORIZONTAL);
        metaRow.setGravity(Gravity.CENTER_VERTICAL);
        meta = text("GOOGLE MAPS", 9, true, Color.rgb(144, 168, 187));
        meta.setLetterSpacing(.08f);
        metaRow.addView(meta, new LinearLayout.LayoutParams(0, -2, 1f));

        liveStatus = text("PRÊT", 8, true, Color.rgb(171, 247, 255));
        liveStatus.setGravity(Gravity.CENTER);
        liveStatus.setPadding(dp(9), dp(4), dp(9), dp(4));
        liveStatus.setBackground(rounded(Color.rgb(10, 27, 39), 12, Color.rgb(36, 85, 104)));
        metaRow.addView(liveStatus);
        card.addView(metaRow);

        LinearLayout navRow = new LinearLayout(this);
        navRow.setOrientation(LinearLayout.HORIZONTAL);
        navRow.setGravity(Gravity.CENTER_VERTICAL);
        navRow.setPadding(0, dp(11), 0, 0);

        arrowView = new ImageView(this);
        arrowView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        arrowView.setPadding(dp(6), dp(6), dp(6), dp(6));
        arrowView.setBackground(iconPlate());
        arrowView.setImageBitmap(NavIconSelector.render("↑", "Continuez tout droit", dp(200)));
        navRow.addView(arrowView, new LinearLayout.LayoutParams(dp(88), dp(88)));

        LinearLayout navText = new LinearLayout(this);
        navText.setOrientation(LinearLayout.VERTICAL);
        navText.setGravity(Gravity.CENTER_VERTICAL);
        navText.setPadding(dp(14), 0, 0, 0);
        distanceView = text("", 30, true, ACCENT);
        instruction = text("En attente d’un trajet", 19, true, TEXT);
        instruction.setMaxLines(2);
        detail = text("Démarre une navigation dans Google Maps", 11, false, MUTED);
        detail.setPadding(0, dp(5), 0, 0);
        detail.setMaxLines(2);
        navText.addView(distanceView);
        navText.addView(instruction);
        navText.addView(detail);
        navRow.addView(navText, new LinearLayout.LayoutParams(0, -2, 1f));
        card.addView(navRow);

        tripSummaryView = text("", 16, true, Color.rgb(220, 249, 255));
        tripSummaryView.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        tripSummaryView.setLineSpacing(dp(3), 1f);
        tripSummaryView.setPadding(dp(14), dp(10), dp(14), dp(10));
        tripSummaryView.setBackground(rounded(Color.rgb(9, 25, 36), 14, Color.rgb(31, 72, 91)));
        tripSummaryView.setVisibility(View.GONE);
        tripSummaryView.setMaxLines(3);
        LinearLayout.LayoutParams tripLp = new LinearLayout.LayoutParams(-1, -2);
        tripLp.setMargins(0, dp(10), 0, 0);
        card.addView(tripSummaryView, tripLp);

        content.addView(card);

        LinearLayout widgetInfo = new LinearLayout(this);
        widgetInfo.setOrientation(LinearLayout.VERTICAL);
        widgetInfo.setPadding(dp(14), dp(11), dp(14), dp(11));
        widgetInfo.setBackground(rounded(Color.rgb(7, 16, 24), 17, Color.rgb(26, 50, 66)));
        TextView wiTitle = text("2 widgets disponibles", 13, true, TEXT);
        TextView wiText = text("Horizontal + carré 4×4 · appui sur le widget = Google Maps · pastille LIVE = réglages", 11, false, MUTED);
        wiText.setPadding(0, dp(4), 0, 0);
        widgetInfo.addView(wiTitle);
        widgetInfo.addView(wiText);
        LinearLayout.LayoutParams wiLp = new LinearLayout.LayoutParams(-1, -2);
        wiLp.setMargins(0, dp(10), 0, 0);
        content.addView(widgetInfo, wiLp);

        TextView settingsLabel = sectionLabel("PARAMÈTRES DU WIDGET");
        settingsLabel.setPadding(dp(2), dp(12), 0, dp(7));
        content.addView(settingsLabel);

        LinearLayout idleArtCard = new LinearLayout(this);
        idleArtCard.setOrientation(LinearLayout.VERTICAL);
        idleArtCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        idleArtCard.setBackground(rounded(Color.rgb(7, 16, 24), 17, Color.rgb(26, 50, 66)));

        LinearLayout idleArtHeader = new LinearLayout(this);
        idleArtHeader.setOrientation(LinearLayout.HORIZONTAL);
        idleArtHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView idleArtTitle = text("Image d’attente", 13, true, TEXT);
        idleArtHeader.addView(idleArtTitle, new LinearLayout.LayoutParams(0, -2, 1f));
        idleArtValue = text("", 12, true, ACCENT);
        idleArtHeader.addView(idleArtValue);
        idleArtCard.addView(idleArtHeader);

        idleArtDescription = text("", 11, false, MUTED);
        idleArtDescription.setPadding(0, dp(5), 0, 0);
        idleArtCard.addView(idleArtDescription);

        idleArtToggleButton = secondaryButton("");
        idleArtToggleButton.setOnClickListener(v -> {
            boolean enabledPref = isIdleArtworkEnabled();
            getSharedPreferences(MapsNotificationListener.PREFS, MODE_PRIVATE)
                    .edit()
                    .putBoolean(PREF_IDLE_ART_ENABLED, !enabledPref)
                    .apply();
            MapsNavWidget.updateAll(this);
            refresh();
        });
        LinearLayout.LayoutParams idleToggleLp = new LinearLayout.LayoutParams(-1, dp(44));
        idleToggleLp.setMargins(0, dp(10), 0, 0);
        idleArtCard.addView(idleArtToggleButton, idleToggleLp);

        LinearLayout.LayoutParams idleCardLp = new LinearLayout.LayoutParams(-1, -2);
        idleCardLp.setMargins(0, 0, 0, 0);
        content.addView(idleArtCard, idleCardLp);

        Button simulate = secondaryButton("Tester la manœuvre suivante");
        simulate.setOnClickListener(v -> simulateNext());
        LinearLayout.LayoutParams simLp = new LinearLayout.LayoutParams(-1, dp(46));
        simLp.setMargins(0, dp(10), 0, 0);
        content.addView(simulate, simLp);

        diagnosticButton = secondaryButton("Afficher le diagnostic");
        diagnosticButton.setOnClickListener(v -> toggleDiagnostic());
        LinearLayout.LayoutParams diagBtnLp = new LinearLayout.LayoutParams(-1, dp(44));
        diagBtnLp.setMargins(0, dp(8), 0, 0);
        content.addView(diagnosticButton, diagBtnLp);

        raw = text("Aucune donnée reçue.", 12, false, Color.rgb(192, 206, 219));
        raw.setTextIsSelectable(true);
        raw.setPadding(dp(12), dp(11), dp(12), dp(11));
        raw.setMinHeight(dp(110));
        raw.setBackground(rounded(Color.rgb(7, 14, 20), 15, Color.rgb(28, 52, 68)));
        raw.setVisibility(View.GONE);
        LinearLayout.LayoutParams rawLp = new LinearLayout.LayoutParams(-1, -2);
        rawLp.setMargins(0, dp(8), 0, 0);
        content.addView(raw, rawLp);

        clearButton = secondaryButton("Effacer les données de test");
        clearButton.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences(MapsNotificationListener.PREFS, MODE_PRIVATE);
            boolean keepIdleArtwork = prefs.getBoolean(PREF_IDLE_ART_ENABLED, true);
            prefs.edit().clear().putBoolean(PREF_IDLE_ART_ENABLED, keepIdleArtwork).apply();
            MapsNavWidget.updateAll(this);
            refresh();
        });
        clearButton.setVisibility(View.GONE);
        LinearLayout.LayoutParams clearLp = new LinearLayout.LayoutParams(-1, dp(44));
        clearLp.setMargins(0, dp(8), 0, dp(4));
        content.addView(clearButton, clearLp);

        setContentView(screen);
    }

    private void toggleDiagnostic() {
        diagnosticVisible = !diagnosticVisible;
        raw.setVisibility(diagnosticVisible ? View.VISIBLE : View.GONE);
        clearButton.setVisibility(diagnosticVisible ? View.VISIBLE : View.GONE);
        diagnosticButton.setText(diagnosticVisible ? "Masquer le diagnostic" : "Afficher le diagnostic");
    }

    private void configureSystemBars() {
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        int flags = getWindow().getDecorView().getSystemUiVisibility();
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= 26) flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        getWindow().getDecorView().setSystemUiVisibility(flags);
        if (Build.VERSION.SDK_INT >= 28) getWindow().setNavigationBarDividerColor(BG);
    }

    private void applySystemBarInsets(final View root) {
        final int baseLeft = dp(18), baseTop = dp(14), baseRight = dp(18), baseBottom = dp(14);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int l, t, r, b;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                l = bars.left; t = bars.top; r = bars.right; b = bars.bottom;
            } else {
                l = insets.getSystemWindowInsetLeft(); t = insets.getSystemWindowInsetTop();
                r = insets.getSystemWindowInsetRight(); b = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(baseLeft + l, baseTop + t, baseRight + r, baseBottom + b);
            return insets;
        });
        root.requestApplyInsets();
    }

    @Override protected void onResume() {
        super.onResume();
        IntentFilter f = new IntentFilter(MapsNotificationListener.ACTION_UPDATE);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, f, RECEIVER_NOT_EXPORTED); else registerReceiver(receiver, f);
        refresh();
    }

    @Override protected void onPause() {
        super.onPause();
        try { unregisterReceiver(receiver); } catch (Throwable ignored) {}
    }

    private void refresh() {
        boolean enabled = listenerEnabled();
        accessStatus.setText(enabled ? "● Prêt · Google Maps peut envoyer ses consignes" : "● Autorise l’accès aux notifications pour activer le widget");
        accessStatus.setTextColor(enabled ? ACCENT : Color.rgb(255, 190, 96));

        SharedPreferences p = getSharedPreferences(MapsNotificationListener.PREFS, MODE_PRIVATE);
        String arrow = p.getString("arrow", "");
        String distance = p.getString("distance", "");
        String primary = p.getString("primary", "");
        String secondary = p.getString("secondary", "");
        String eta = p.getString("eta", "");
        String tripDistance = p.getString("trip_distance", "");
        String tripDuration = p.getString("trip_duration", "");
        String rawText = p.getString("raw", "");
        boolean simulated = p.getBoolean("simulated", false);
        String tripSummary = TripSummaryFormatter.build(eta, tripDistance, tripDuration);

        if (TextUtils.isEmpty(primary)) {
            arrowView.setImageBitmap(NavIconSelector.render("↑", "Continuez tout droit", dp(200)));
            distanceView.setText("");
            instruction.setText("En attente d’un trajet");
            detail.setText("Démarre une navigation dans Google Maps");
            meta.setText("GOOGLE MAPS");
            liveStatus.setText(enabled ? "PRÊT" : "OFF");
            tripSummaryView.setVisibility(View.GONE);
        } else {
            arrowView.setImageBitmap(NavIconSelector.render(arrow, primary, dp(200)));
            distanceView.setText(distance);
            instruction.setText(primary);
            detail.setText(TextUtils.isEmpty(secondary) ? "Google Maps" : secondary);
            meta.setText(simulated ? "MODE TEST" : "GOOGLE MAPS");
            liveStatus.setText(simulated ? "TEST" : "LIVE");
            if (TextUtils.isEmpty(tripSummary)) {
                tripSummaryView.setVisibility(View.GONE);
            } else {
                tripSummaryView.setText(tripSummary);
                tripSummaryView.setVisibility(View.VISIBLE);
            }
        }
        refreshIdleArtPreference();
        raw.setText(TextUtils.isEmpty(rawText) ? "Aucune donnée reçue de Google Maps." : rawText);
    }

    private boolean isIdleArtworkEnabled() {
        return getSharedPreferences(MapsNotificationListener.PREFS, MODE_PRIVATE)
                .getBoolean(PREF_IDLE_ART_ENABLED, true);
    }

    private void refreshIdleArtPreference() {
        boolean enabled = isIdleArtworkEnabled();
        if (idleArtValue != null) {
            idleArtValue.setText(enabled ? "Activée" : "Désactivée");
            idleArtValue.setTextColor(enabled ? ACCENT : Color.rgb(255, 198, 112));
        }
        if (idleArtDescription != null) {
            idleArtDescription.setText(enabled
                    ? "Sans trajet actif, le widget affiche l’image prédéfinie."
                    : "Sans trajet actif, le widget affiche la flèche bleue et “En attente d’un trajet”.");
        }
        if (idleArtToggleButton != null) {
            idleArtToggleButton.setText(enabled ? "Désactiver l’image d’attente" : "Activer l’image d’attente");
        }
    }

    private boolean listenerEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (enabled == null) return false;
        ComponentName me = new ComponentName(this, MapsNotificationListener.class);
        return enabled.contains(me.flattenToString()) || enabled.contains(getPackageName());
    }

    private void openNotificationAccess() {
        try { startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")); }
        catch (Throwable t) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
    }

    private void openMaps() {
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage(MapsNotificationListener.MAPS_PACKAGE);
            if (i != null) startActivity(i);
            else startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com")));
        } catch (Throwable t) {
            Toast.makeText(this, "Google Maps introuvable", Toast.LENGTH_LONG).show();
        }
    }

    private void simulateNext() {
        String[][] tests = {
                {"↑", "600 m", "Continuez tout droit", "Route de Lausanne"},
                {"←", "100 m", "Tournez à gauche", "Rue du Rhône"},
                {"→", "200 m", "Tournez à droite", "Avenue de France"},
                {"↖", "350 m", "Légèrement à gauche", "Route cantonale"},
                {"↗", "450 m", "Légèrement à droite", "Direction Sion"},
                {"↙", "250 m", "Fortement à gauche", "Route secondaire"},
                {"↘", "250 m", "Fortement à droite", "Route secondaire"},
                {"↶", "80 m", "Faites demi-tour à gauche", "Route principale"},
                {"↷", "80 m", "Faites demi-tour à droite", "Route principale"},
                {"↖", "300 m", "Restez à gauche", "Deux voies"},
                {"↗", "300 m", "Restez à droite", "Deux voies"},
                {"↑", "500 m", "Restez au centre", "Trois voies"},
                {"↑", "400 m", "Continuez tout droit sur la voie", "Voie centrale"},
                {"↖", "300 m", "Rejoignez la voie de gauche", "Deux voies"},
                {"↗", "300 m", "Rejoignez la voie de droite", "Deux voies"},
                {"↑", "280 m", "Fusion par la gauche", "Insertion"},
                {"↑", "280 m", "Fusion par la droite", "Insertion"},
                {"↖", "220 m", "Bifurcation à gauche", "Échangeur"},
                {"↗", "220 m", "Bifurcation à droite", "Échangeur"},
                {"←", "500 m", "Prenez la sortie à gauche", "Autoroute"},
                {"→", "500 m", "Prenez la sortie à droite", "Autoroute"},
                {"↑", "1,2 km", "Rejoignez l'autoroute", "A9"},
                {"→", "600 m", "Quitter l'autoroute", "Sortie 27"},
                {"⟳", "120 m", "Prenez la 1re sortie", "Rond-point"},
                {"⟳", "120 m", "Prenez la 2e sortie", "Rond-point"},
                {"⟳", "120 m", "Prenez la 3e sortie", "Rond-point"},
                {"⚠", "", "Travaux", "Route principale"},
                {"⚑", "", "Vous êtes arrivé", "Destination"}
        };
        String[] t = tests[testIndex % tests.length];
        testIndex++;
        getSharedPreferences(MapsNotificationListener.PREFS, MODE_PRIVATE).edit()
                .putString("arrow", t[0]).putString("distance", t[1]).putString("primary", t[2]).putString("secondary", t[3])
                .putString("eta", "15:42").putString("trip_distance", "18,6 km").putString("trip_duration", "24 min")
                .putString("timestamp", MapsNotificationListener.now())
                .putString("raw", "SIMULATION v1.15.0\narrow=" + t[0] + "\ndistance=" + t[1] + "\ninstruction=" + t[2] + "\nroad=" + t[3] + "\neta=15:42\ntripDistance=18,6 km\ntripDuration=24 min")
                .putBoolean("simulated", true).apply();
        MapsNavWidget.updateAll(this);
        refresh();
    }

    private TextView sectionLabel(String value) {
        TextView t = text(value, 10, true, Color.rgb(102, 130, 153));
        t.setLetterSpacing(.10f);
        return t;
    }

    private TextView text(String value, float size, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setIncludeFontPadding(false);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value); b.setAllCaps(false); b.setTextColor(Color.rgb(3, 17, 25)); b.setTextSize(13); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.rgb(132, 243, 255), ACCENT_DARK});
        g.setCornerRadius(dp(15)); g.setStroke(dp(1), Color.rgb(168, 247, 255)); b.setBackground(g);
        return b;
    }

    private Button secondaryButton(String value) {
        Button b = new Button(this);
        b.setText(value); b.setAllCaps(false); b.setTextColor(Color.rgb(207, 233, 248)); b.setTextSize(13); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.rgb(13, 29, 42), Color.rgb(7, 16, 24)});
        g.setCornerRadius(dp(15)); g.setStroke(dp(1), Color.rgb(35, 72, 96)); b.setBackground(g);
        return b;
    }

    private GradientDrawable iconPlate() {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.rgb(12, 31, 45), Color.rgb(5, 12, 19)});
        g.setCornerRadius(dp(21)); g.setStroke(dp(1), Color.rgb(28, 58, 76));
        return g;
    }

    private GradientDrawable gradientCard() {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.rgb(11, 20, 29), Color.rgb(3, 7, 11)});
        g.setCornerRadius(dp(27)); g.setStroke(dp(1), Color.rgb(28, 53, 67));
        return g;
    }

    private GradientDrawable rounded(int fill, int radiusDp, int stroke) {
        GradientDrawable g = new GradientDrawable(); g.setColor(fill); g.setCornerRadius(dp(radiusDp)); g.setStroke(dp(1), stroke); return g;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
