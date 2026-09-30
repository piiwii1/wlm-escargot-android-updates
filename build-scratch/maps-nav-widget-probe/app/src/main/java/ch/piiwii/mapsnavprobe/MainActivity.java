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
    private static final int BG = Color.rgb(7,11,17);
    private static final int CARD = Color.rgb(14,24,35);
    private static final int TEXT = Color.WHITE;
    private static final int MUTED = Color.rgb(158,176,193);
    private static final int ACCENT = Color.rgb(111,229,255);
    private static final int ACCENT_DARK = Color.rgb(28,151,235);

    private TextView accessStatus, distanceView, instruction, detail, meta, raw;
    private ImageView arrowView;
    private int testIndex = 0;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { refresh(); }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);

        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        int flags = getWindow().getDecorView().getSystemUiVisibility();
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= 26) flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        getWindow().getDecorView().setSystemUiVisibility(flags);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(18), dp(18), dp(18), dp(16));
        applySystemBarInsets(root);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(14));

        LinearLayout headerText = new LinearLayout(this);
        headerText.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("Maps Nav Probe", 28, true, TEXT);
        TextView intro = text("v1.4.0 · navigation visuelle Google Maps", 13, false, MUTED);
        intro.setPadding(0, dp(2), 0, 0);
        headerText.addView(title);
        headerText.addView(intro);
        header.addView(headerText, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView version = text("1.4", 12, true, ACCENT);
        version.setGravity(Gravity.CENTER);
        version.setBackground(rounded(Color.rgb(11,30,44), 12, Color.rgb(41,119,161)));
        version.setPadding(dp(10), dp(6), dp(10), dp(6));
        header.addView(version);
        root.addView(header);

        accessStatus = text("", 14, true, MUTED);
        accessStatus.setPadding(dp(13), dp(11), dp(13), dp(11));
        accessStatus.setBackground(rounded(Color.rgb(12,25,36), 16, Color.rgb(43,78,104)));
        root.addView(accessStatus, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(10), 0, dp(10));
        Button permission = button("Notifications");
        permission.setOnClickListener(v -> openNotificationAccess());
        Button maps = button("Ouvrir Maps");
        maps.setOnClickListener(v -> openMaps());
        LinearLayout.LayoutParams actionLp = new LinearLayout.LayoutParams(0, dp(48), 1f);
        actionLp.setMarginEnd(dp(6));
        actions.addView(permission, actionLp);
        LinearLayout.LayoutParams actionLp2 = new LinearLayout.LayoutParams(0, dp(48), 1f);
        actionLp2.setMarginStart(dp(6));
        actions.addView(maps, actionLp2);
        root.addView(actions);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(15), dp(13), dp(15), dp(15));
        card.setBackground(gradientCard());

        meta = text("● GOOGLE MAPS", 11, true, ACCENT);
        meta.setLetterSpacing(0.08f);
        card.addView(meta);

        LinearLayout navRow = new LinearLayout(this);
        navRow.setOrientation(LinearLayout.HORIZONTAL);
        navRow.setGravity(Gravity.CENTER_VERTICAL);
        navRow.setPadding(0, dp(11), 0, 0);

        arrowView = new ImageView(this);
        arrowView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        arrowView.setPadding(dp(8), dp(8), dp(8), dp(8));
        arrowView.setBackground(rounded(Color.rgb(10,25,38), 22, Color.rgb(56,124,166)));
        arrowView.setImageBitmap(NavIconRenderer.render("↑", "Continuez tout droit", dp(160)));
        navRow.addView(arrowView, new LinearLayout.LayoutParams(dp(96), dp(96)));

        LinearLayout navText = new LinearLayout(this);
        navText.setOrientation(LinearLayout.VERTICAL);
        navText.setPadding(dp(15), 0, 0, 0);
        distanceView = text("", 27, true, ACCENT);
        instruction = text("En attente d’un trajet", 22, true, TEXT);
        detail = text("Lance Google Maps et démarre la navigation", 14, false, MUTED);
        detail.setPadding(0, dp(4), 0, 0);
        navText.addView(distanceView);
        navText.addView(instruction);
        navText.addView(detail);
        navRow.addView(navText, new LinearLayout.LayoutParams(0, -2, 1f));
        card.addView(navRow);
        root.addView(card);

        Button simulate = button("Tester la manœuvre suivante");
        simulate.setOnClickListener(v -> simulateNext());
        LinearLayout.LayoutParams simLp = new LinearLayout.LayoutParams(-1, dp(48));
        simLp.setMargins(0, dp(10), 0, 0);
        root.addView(simulate, simLp);

        TextView rawTitle = text("Diagnostic Google Maps", 13, true, MUTED);
        rawTitle.setPadding(0, dp(14), 0, dp(6));
        root.addView(rawTitle);

        ScrollView scroll = new ScrollView(this);
        raw = text("Aucune donnée reçue.", 12, false, Color.rgb(194,207,219));
        raw.setTextIsSelectable(true);
        raw.setPadding(dp(12), dp(10), dp(12), dp(10));
        raw.setBackground(rounded(Color.rgb(10,17,24), 14, Color.rgb(37,65,86)));
        scroll.addView(raw);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        Button clear = button("Effacer les données du test");
        clear.setOnClickListener(v -> {
            getSharedPreferences(MapsNotificationListener.PREFS, MODE_PRIVATE).edit().clear().apply();
            MapsNavWidget.updateAll(this);
            refresh();
        });
        LinearLayout.LayoutParams clearLp = new LinearLayout.LayoutParams(-1, dp(46));
        clearLp.setMargins(0, dp(10), 0, 0);
        root.addView(clear, clearLp);

        setContentView(root);
    }

    private void applySystemBarInsets(final View root) {
        final int left = dp(18), top = dp(18), right = dp(18), bottom = dp(16);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int l, t, r, b;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                l = bars.left; t = bars.top; r = bars.right; b = bars.bottom;
            } else {
                l = insets.getSystemWindowInsetLeft();
                t = insets.getSystemWindowInsetTop();
                r = insets.getSystemWindowInsetRight();
                b = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(left + l, top + t, right + r, bottom + b);
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
        accessStatus.setText(enabled ? "● Accès notifications activé" : "● Active l’accès aux notifications");
        accessStatus.setTextColor(enabled ? ACCENT : Color.rgb(255,184,92));

        SharedPreferences p = getSharedPreferences(MapsNotificationListener.PREFS, MODE_PRIVATE);
        String arrow = p.getString("arrow", "");
        String distance = p.getString("distance", "");
        String primary = p.getString("primary", "");
        String secondary = p.getString("secondary", "");
        String stamp = p.getString("timestamp", "");
        String rawText = p.getString("raw", "");
        boolean simulated = p.getBoolean("simulated", false);

        if (TextUtils.isEmpty(primary)) {
            arrowView.setImageBitmap(NavIconRenderer.render("↑", "Continuez tout droit", dp(160)));
            distanceView.setText("");
            instruction.setText("En attente d’un trajet");
            detail.setText("Lance Google Maps et démarre la navigation");
            meta.setText("● GOOGLE MAPS");
        } else {
            arrowView.setImageBitmap(NavIconRenderer.render(arrow, primary, dp(160)));
            distanceView.setText(distance);
            instruction.setText(primary);
            detail.setText(TextUtils.isEmpty(secondary) ? "Google Maps" : secondary);
            String metaText = simulated ? "● MODE TEST" : "● GOOGLE MAPS";
            if (!TextUtils.isEmpty(stamp)) metaText += "   •   " + stamp;
            meta.setText(metaText);
        }

        raw.setText(TextUtils.isEmpty(rawText) ? "Aucune donnée reçue de Google Maps." : rawText);
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
                {"↗", "450 m", "Restez à droite", "Direction Sion"},
                {"↶", "80 m", "Faites demi-tour", "Route principale"},
                {"⟳", "120 m", "Prenez la 3e sortie", "Rond-point"},
                {"⚑", "", "Vous êtes arrivé", "Destination"}
        };
        String[] t = tests[testIndex % tests.length];
        testIndex++;
        getSharedPreferences(MapsNotificationListener.PREFS, MODE_PRIVATE).edit()
                .putString("arrow", t[0])
                .putString("distance", t[1])
                .putString("primary", t[2])
                .putString("secondary", t[3])
                .putString("timestamp", MapsNotificationListener.now())
                .putString("raw", "SIMULATION v1.4.0\narrow=" + t[0] + "\ndistance=" + t[1] + "\ninstruction=" + t[2] + "\nroad=" + t[3])
                .putBoolean("simulated", true)
                .apply();
        MapsNavWidget.updateAll(this);
        refresh();
    }

    private TextView text(String value, float size, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setAllCaps(false);
        b.setTextColor(Color.rgb(3,17,25));
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(116,238,255), ACCENT_DARK});
        g.setCornerRadius(dp(15));
        g.setStroke(dp(1), Color.rgb(147,243,255));
        b.setBackground(g);
        return b;
    }

    private GradientDrawable gradientCard() {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(16,31,45), Color.rgb(8,15,23)});
        g.setCornerRadius(dp(24));
        g.setStroke(dp(1), Color.rgb(48,103,139));
        return g;
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
