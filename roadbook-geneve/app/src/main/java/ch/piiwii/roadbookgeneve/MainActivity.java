package ch.piiwii.roadbookgeneve;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {
    private final Handler handler = new Handler();
    private TextView nextTitle;
    private TextView countdown;
    private TextView nextTime;
    private TextView alertStatus;
    private Button alertToggle;
    private LinearLayout programRows;
    private boolean uiReady = false;

    private static final int BG = Color.rgb(9, 14, 23);
    private static final int CARD = Color.rgb(22, 30, 43);
    private static final int CARD_SOFT = Color.rgb(29, 39, 55);
    private static final int TEXT = Color.rgb(244, 247, 251);
    private static final int MUTED = Color.rgb(158, 170, 188);
    private static final int ACCENT = Color.rgb(255, 181, 71);
    private static final int GREEN = Color.rgb(82, 211, 143);
    private static final int BLUE = Color.rgb(90, 166, 235);
    private static final int PURPLE = Color.rgb(133, 101, 201);

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable rounded(int color, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private GradientDrawable roundedStroke(int color, int strokeColor, float radiusDp) {
        GradientDrawable d = rounded(color, radiusDp);
        d.setStroke(dp(1), strokeColor);
        return d;
    }

    private TextView text(String value, float sp, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setIncludeFontPadding(false);
        return v;
    }

    private Button button(String label, int background) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(13);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(12), 0, dp(12), 0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setBackground(rounded(background, 12));
        return b;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AlarmReceiver.createChannel(this);

        Window w = getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(BG);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) w.setNavigationBarDividerColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        applySystemBarInsets(root);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(14), dp(16), dp(20));

        addHeader(content);
        addNextCard(content);
        addAlertsCard(content);
        addProgramCard(content);
        addChoiceCard(content);

        TextView footer = text("Touchez une heure pour la modifier. Les rappels se recalculent automatiquement.", 11.5f, MUTED);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerLp = matchWrap();
        footerLp.topMargin = dp(16);
        footerLp.bottomMargin = dp(4);
        content.addView(footer, footerLp);

        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

        uiReady = true;
        updateAlertStatus();
        handler.post(ticker);
    }

    private void applySystemBarInsets(View root) {
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top;
            int bottom;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                top = bars.top;
                bottom = bars.bottom;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(0, top, 0, bottom);
            return insets;
        });
        root.requestApplyInsets();
    }

    private void addHeader(LinearLayout content) {
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);

        TextView kicker = text("SAMEDI 29 AOÛT", 11.5f, ACCENT);
        kicker.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        kicker.setLetterSpacing(0.12f);
        names.addView(kicker, matchWrap());

        TextView title = text("Roadbook Genève", 27, TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = matchWrap();
        titleLp.topMargin = dp(3);
        names.addView(title, titleLp);

        top.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView version = text("v1.1", 11.5f, MUTED);
        version.setGravity(Gravity.CENTER);
        version.setPadding(dp(10), dp(6), dp(10), dp(6));
        version.setBackground(rounded(CARD, 20));
        top.addView(version, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams topLp = matchWrap();
        topLp.bottomMargin = dp(14);
        content.addView(top, topLp);
    }

    private void addNextCard(LinearLayout content) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(17), dp(15), dp(17), dp(15));
        card.setBackground(roundedStroke(CARD_SOFT, Color.rgb(47, 62, 82), 18));

        TextView label = text("PROCHAINE ÉTAPE", 10.5f, GREEN);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setLetterSpacing(0.14f);
        card.addView(label, matchWrap());

        nextTitle = text("—", 19, TEXT);
        nextTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = matchWrap();
        titleLp.topMargin = dp(7);
        card.addView(nextTitle, titleLp);

        countdown = text("—", 32, ACCENT);
        countdown.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        LinearLayout.LayoutParams countLp = matchWrap();
        countLp.topMargin = dp(5);
        card.addView(countdown, countLp);

        nextTime = text("", 12.5f, MUTED);
        LinearLayout.LayoutParams timeLp = matchWrap();
        timeLp.topMargin = dp(1);
        card.addView(nextTime, timeLp);

        content.addView(card, matchWrap());
    }

    private void addAlertsCard(LinearLayout content) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(rounded(CARD, 15));
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = dp(10);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("Rappels", 15.5f, TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        labels.addView(title, matchWrap());
        alertStatus = text("", 12, MUTED);
        LinearLayout.LayoutParams statusLp = matchWrap();
        statusLp.topMargin = dp(3);
        labels.addView(alertStatus, statusLp);
        top.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        alertToggle = button("Activer", Color.rgb(48, 118, 84));
        alertToggle.setOnClickListener(v -> toggleAlerts());
        top.addView(alertToggle, new LinearLayout.LayoutParams(dp(86), dp(40)));

        Button test = button("Test", Color.rgb(49, 66, 88));
        LinearLayout.LayoutParams testLp = new LinearLayout.LayoutParams(dp(62), dp(40));
        testLp.leftMargin = dp(7);
        test.setOnClickListener(v -> sendTestNotification());
        top.addView(test, testLp);

        card.addView(top, matchWrap());
        content.addView(card, lp);
    }

    private void addProgramCard(LinearLayout content) {
        TextView section = text("Programme", 20, TEXT);
        section.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams sectionLp = matchWrap();
        sectionLp.topMargin = dp(20);
        sectionLp.bottomMargin = dp(8);
        content.addView(section, sectionLp);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(4), dp(14), dp(4));
        card.setBackground(rounded(CARD, 17));

        programRows = new LinearLayout(this);
        programRows.setOrientation(LinearLayout.VERTICAL);
        card.addView(programRows, matchWrap());
        rebuildProgram();

        content.addView(card, matchWrap());
    }

    private void rebuildProgram() {
        if (programRows == null) return;
        programRows.removeAllViews();
        for (int i = 0; i < Itinerary.STOPS.length; i++) {
            addProgramRow(Itinerary.STOPS[i]);
            if (i < Itinerary.STOPS.length - 1) {
                View divider = new View(this);
                divider.setBackgroundColor(Color.rgb(43, 53, 69));
                LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
                dlp.leftMargin = dp(72);
                programRows.addView(divider, dlp);
            }
        }
    }

    private void addProgramRow(Itinerary.Stop stop) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.TOP);
        row.setPadding(0, dp(11), 0, dp(11));

        TextView time = text(Itinerary.getTime(this, stop), 16.5f, stop.fixedEvent ? ACCENT : BLUE);
        time.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        time.setGravity(Gravity.CENTER);
        time.setPadding(dp(6), dp(7), dp(6), dp(7));
        time.setBackground(rounded(Color.rgb(32, 43, 60), 10));
        time.setOnClickListener(v -> editTime(stop));
        row.addView(time, new LinearLayout.LayoutParams(dp(66), ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams detailsLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        detailsLp.leftMargin = dp(10);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = text(stop.title, 15.5f, TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (stop.fixedEvent) {
            TextView fixed = text("FIXE", 9.5f, ACCENT);
            fixed.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            fixed.setGravity(Gravity.CENTER);
            fixed.setPadding(dp(7), dp(4), dp(7), dp(4));
            fixed.setBackground(rounded(Color.rgb(59, 48, 31), 12));
            LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            flp.leftMargin = dp(6);
            titleRow.addView(fixed, flp);
        }
        details.addView(titleRow, matchWrap());

        TextView note = text(stop.note, 12.5f, MUTED);
        note.setLineSpacing(0, 1.08f);
        LinearLayout.LayoutParams noteLp = matchWrap();
        noteLp.topMargin = dp(4);
        details.addView(note, noteLp);

        if (stop.destination != null) {
            Button gps = button("GPS → " + stop.gpsLabel, Color.rgb(36, 91, 136));
            LinearLayout.LayoutParams gpsLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
            gpsLp.topMargin = dp(8);
            gps.setOnClickListener(v -> navigate(stop.destination));
            details.addView(gps, gpsLp);
        }

        row.addView(details, detailsLp);
        programRows.addView(row, matchWrap());
    }

    private void addChoiceCard(LinearLayout content) {
        TextView section = text("Après 22h20", 20, TEXT);
        section.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams sectionLp = matchWrap();
        sectionLp.topMargin = dp(20);
        sectionLp.bottomMargin = dp(8);
        content.addView(section, sectionLp);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(rounded(Color.rgb(32, 27, 47), 17));

        TextView desc = text("Tu décides sur le moment selon la motivation.", 12.5f, MUTED);
        LinearLayout.LayoutParams descLp = matchWrap();
        descLp.bottomMargin = dp(10);
        card.addView(desc, descLp);

        Button classic = button("22h30  ·  Rester pour le Classique", PURPLE);
        classic.setOnClickListener(v -> {
            Itinerary.prefs(this).edit().putBoolean("stay_classic", true).apply();
            updateCountdown();
            Toast.makeText(this, "Classique 22h30 retenu", Toast.LENGTH_SHORT).show();
        });
        card.addView(classic, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));

        Button tomato = button("🍅  Fête de la Tomate  ·  Lausanne", Color.rgb(175, 78, 68));
        LinearLayout.LayoutParams tomatoLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        tomatoLp.topMargin = dp(8);
        tomato.setOnClickListener(v -> {
            Itinerary.prefs(this).edit().putBoolean("stay_classic", false).apply();
            navigate(Itinerary.TOMATO_DESTINATION);
        });
        card.addView(tomato, tomatoLp);

        TextView tomatoWhere = text("Gymnase de la Cité • Place de la Cathédrale 1", 11.5f, MUTED);
        tomatoWhere.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams whereLp = matchWrap();
        whereLp.topMargin = dp(5);
        card.addView(tomatoWhere, whereLp);

        Button home = button("⌂  Retour Sion", Color.rgb(47, 101, 76));
        LinearLayout.LayoutParams homeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        homeLp.topMargin = dp(9);
        home.setOnClickListener(v -> {
            Itinerary.prefs(this).edit().putBoolean("stay_classic", false).apply();
            navigate(Itinerary.HOME_DESTINATION);
        });
        card.addView(home, homeLp);

        Button other = button("Autre destination…", Color.rgb(54, 57, 75));
        LinearLayout.LayoutParams otherLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42));
        otherLp.topMargin = dp(8);
        other.setOnClickListener(v -> showOtherDestinationDialog());
        card.addView(other, otherLp);

        content.addView(card, matchWrap());
    }

    private void showOtherDestinationDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Lieu ou adresse");
        input.setText(Itinerary.prefs(this).getString("other_destination", ""));
        int pad = dp(18);
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setPadding(pad, dp(4), pad, 0);
        wrapper.addView(input, matchWrap());

        new AlertDialog.Builder(this)
                .setTitle("Autre destination")
                .setView(wrapper)
                .setNegativeButton("Annuler", null)
                .setPositiveButton("GPS", (dialog, which) -> {
                    String dest = input.getText().toString().trim();
                    if (dest.isEmpty()) {
                        Toast.makeText(this, "Entre un lieu ou une adresse", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Itinerary.prefs(this).edit()
                            .putString("other_destination", dest)
                            .putBoolean("stay_classic", false)
                            .apply();
                    navigate(dest);
                })
                .show();
    }

    private void editTime(Itinerary.Stop stop) {
        String[] p = Itinerary.getTime(this, stop).split(":");
        int hour = Integer.parseInt(p[0]);
        int minute = Integer.parseInt(p[1]);
        TimePickerDialog dialog = new TimePickerDialog(this, (view, h, m) -> {
            Itinerary.setTime(this, stop, h, m);
            if (AlarmScheduler.isEnabled(this)) AlarmScheduler.scheduleAll(this);
            rebuildProgram();
            updateCountdown();
            Toast.makeText(this, "Horaire mis à jour", Toast.LENGTH_SHORT).show();
        }, hour, minute, true);
        dialog.setTitle(stop.title);
        dialog.show();
    }

    private void toggleAlerts() {
        if (AlarmScheduler.isEnabled(this)) {
            AlarmScheduler.setEnabled(this, false);
            updateAlertStatus();
            Toast.makeText(this, "Rappels désactivés", Toast.LENGTH_SHORT).show();
        } else {
            enableAlerts();
        }
    }

    private void enableAlerts() {
        AlarmScheduler.setEnabled(this, true);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 501);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !AlarmScheduler.canExact(this)) {
            try {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                Toast.makeText(this, "Autorise « Alarmes et rappels » puis reviens dans l'app.", Toast.LENGTH_LONG).show();
            } catch (ActivityNotFoundException ignored) {
                AlarmScheduler.scheduleAll(this);
            }
        } else {
            AlarmScheduler.scheduleAll(this);
            Toast.makeText(this, "Rappels programmés", Toast.LENGTH_SHORT).show();
        }
        updateAlertStatus();
    }

    private void sendTestNotification() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 502);
            return;
        }
        Intent i = new Intent(this, AlarmReceiver.class);
        i.putExtra("title", "Test Roadbook Genève");
        i.putExtra("text", "Parfait : les notifications fonctionnent.");
        i.putExtra("notification_id", 9090);
        sendBroadcast(i);
    }

    private void updateAlertStatus() {
        if (alertStatus == null || alertToggle == null) return;
        if (!AlarmScheduler.isEnabled(this)) {
            alertStatus.setText("Inactifs");
            alertStatus.setTextColor(MUTED);
            alertToggle.setText("Activer");
            alertToggle.setBackground(rounded(Color.rgb(48, 118, 84), 12));
            return;
        }
        alertToggle.setText("Couper");
        alertToggle.setBackground(rounded(Color.rgb(128, 65, 65), 12));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !AlarmScheduler.canExact(this)) {
            alertStatus.setText("Actifs • précision souple");
            alertStatus.setTextColor(ACCENT);
        } else {
            alertStatus.setText("Actifs • heures précises");
            alertStatus.setTextColor(GREEN);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (AlarmScheduler.isEnabled(this)) AlarmScheduler.scheduleAll(this);
        if (uiReady) {
            updateAlertStatus();
            updateCountdown();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (AlarmScheduler.isEnabled(this)) AlarmScheduler.scheduleAll(this);
        updateAlertStatus();
    }

    private void navigate(String destination) {
        try {
            Uri uri = Uri.parse("google.navigation:q=" + Uri.encode(destination));
            Intent map = new Intent(Intent.ACTION_VIEW, uri);
            map.setPackage("com.google.android.apps.maps");
            startActivity(map);
        } catch (ActivityNotFoundException e) {
            Uri web = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(destination));
            startActivity(new Intent(Intent.ACTION_VIEW, web));
        }
    }

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateCountdown();
            handler.postDelayed(this, 1000);
        }
    };

    private void updateCountdown() {
        if (nextTitle == null) return;
        long now = System.currentTimeMillis();
        Itinerary.Stop next = null;
        long nextMillis = Long.MAX_VALUE;

        for (Itinerary.Stop stop : Itinerary.STOPS) {
            long when = Itinerary.getTimeMillis(this, stop);
            if (when >= now && when < nextMillis) {
                next = stop;
                nextMillis = when;
            }
        }

        boolean stayClassic = Itinerary.prefs(this).getBoolean("stay_classic", false);
        long classic = Itinerary.classicMillis();
        boolean classicIsNext = stayClassic && classic >= now && classic < nextMillis;

        if (next == null && !classicIsNext) {
            nextTitle.setText("Fin de soirée libre");
            countdown.setText("À toi de voir 😎");
            countdown.setTextSize(23);
            nextTime.setText("Classique, Lausanne, Sion ou autre sortie.");
            return;
        }

        countdown.setTextSize(32);
        if (classicIsNext) {
            nextTitle.setText("Rêve d'Eau — Classique");
            nextMillis = classic;
            nextTime.setText("Prévu à 22:30 • Jardin Anglais");
        } else {
            nextTitle.setText(next.title);
            nextTime.setText("Prévu à " + Itinerary.getTime(this, next) + " • heure Suisse");
        }

        long diff = Math.max(0, nextMillis - now);
        long totalSeconds = diff / 1000;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (days > 0) {
            countdown.setText(String.format(Locale.ROOT, "%dj %02d:%02d:%02d", days, hours, minutes, seconds));
        } else {
            countdown.setText(String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds));
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(ticker);
        super.onDestroy();
    }
}
