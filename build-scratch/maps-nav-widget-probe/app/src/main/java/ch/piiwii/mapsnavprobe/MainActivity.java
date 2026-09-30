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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(13,17,23);
    private static final int CARD = Color.rgb(24,32,41);
    private static final int TEXT = Color.WHITE;
    private static final int MUTED = Color.rgb(174,184,196);
    private static final int ACCENT = Color.rgb(121,227,138);

    private TextView accessStatus, arrowView, distanceView, instruction, detail, meta, raw;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { refresh(); }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(16));
        root.setBackgroundColor(BG);

        TextView title = text("Maps Nav Probe", 27, true, TEXT);
        root.addView(title);
        TextView intro = text("v1.2.0 · aperçu navigation simplifié depuis Google Maps", 14, false, MUTED);
        intro.setPadding(0, dp(3), 0, dp(13));
        root.addView(intro);

        accessStatus = text("", 14, true, MUTED);
        accessStatus.setPadding(dp(12), dp(10), dp(12), dp(10));
        accessStatus.setBackground(rounded(Color.rgb(25,35,44), 14, Color.rgb(54,70,84)));
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
        card.setPadding(dp(14), dp(12), dp(14), dp(14));
        card.setBackground(rounded(CARD, 22, Color.rgb(61,76,93)));

        meta = text("● GOOGLE MAPS", 11, true, ACCENT);
        meta.setLetterSpacing(0.06f);
        card.addView(meta);

        LinearLayout navRow = new LinearLayout(this);
        navRow.setOrientation(LinearLayout.HORIZONTAL);
        navRow.setGravity(Gravity.CENTER_VERTICAL);
        navRow.setPadding(0, dp(10), 0, 0);

        arrowView = text("↑", 43, true, Color.rgb(13,20,26));
        arrowView.setGravity(Gravity.CENTER);
        arrowView.setBackground(rounded(ACCENT, 100, Color.rgb(167,242,178)));
        navRow.addView(arrowView, new LinearLayout.LayoutParams(dp(78), dp(78)));

        LinearLayout navText = new LinearLayout(this);
        navText.setOrientation(LinearLayout.VERTICAL);
        navText.setPadding(dp(14), 0, 0, 0);
        distanceView = text("", 22, true, ACCENT);
        instruction = text("En attente d’un trajet", 23, true, TEXT);
        detail = text("Lance Google Maps et démarre la navigation", 14, false, MUTED);
        detail.setPadding(0, dp(3), 0, 0);
        navText.addView(distanceView);
        navText.addView(instruction);
        navText.addView(detail);
        navRow.addView(navText, new LinearLayout.LayoutParams(0, -2, 1f));
        card.addView(navRow);
        root.addView(card);

        Button simulate = button("Tester une consigne : gauche à 100 m");
        simulate.setOnClickListener(v -> simulate());
        LinearLayout.LayoutParams simLp = new LinearLayout.LayoutParams(-1, dp(48));
        simLp.setMargins(0, dp(10), 0, 0);
        root.addView(simulate, simLp);

        TextView rawTitle = text("Diagnostic Google Maps", 14, true, MUTED);
        rawTitle.setPadding(0, dp(14), 0, dp(6));
        root.addView(rawTitle);

        ScrollView scroll = new ScrollView(this);
        raw = text("Aucune donnée reçue.", 12, false, Color.rgb(203,211,220));
        raw.setTextIsSelectable(true);
        raw.setPadding(dp(12), dp(10), dp(12), dp(10));
        raw.setBackground(rounded(Color.rgb(18,24,31), 14, Color.rgb(45,58,71)));
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
            arrowView.setText("↑");
            distanceView.setText("");
            instruction.setText("En attente d’un trajet");
            detail.setText("Lance Google Maps et démarre la navigation");
            meta.setText("● GOOGLE MAPS");
        } else {
            arrowView.setText(TextUtils.isEmpty(arrow) ? "↑" : arrow);
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

    private void simulate() {
        getSharedPreferences(MapsNotificationListener.PREFS, MODE_PRIVATE).edit()
                .putString("arrow", "←")
                .putString("distance", "100 m")
                .putString("primary", "Tournez à gauche")
                .putString("secondary", "Rue de Lausanne")
                .putString("timestamp", MapsNotificationListener.now())
                .putString("raw", "SIMULATION\ntext=À 100 m, tournez à gauche sur Rue de Lausanne\n\nPARSED\narrow=←\ndistance=100 m\ninstruction=Tournez à gauche\nroad=Rue de Lausanne")
                .putBoolean("simulated", true)
                .apply();
        MapsNavWidget.updateAll(this);
        refresh();
        Toast.makeText(this, "Simulation envoyée au widget", Toast.LENGTH_SHORT).show();
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
        b.setTextColor(Color.rgb(13,20,26));
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(rounded(ACCENT, 14, Color.rgb(167,242,178)));
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
