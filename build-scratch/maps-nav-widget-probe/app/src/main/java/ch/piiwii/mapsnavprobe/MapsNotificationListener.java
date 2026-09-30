package ch.piiwii.mapsnavprobe;

import android.app.Notification;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MapsNotificationListener extends NotificationListenerService {
    public static final String MAPS_PACKAGE = "com.google.android.apps.maps";
    public static final String PREFS = "maps_nav_probe";
    public static final String ACTION_UPDATE = "ch.piiwii.mapsnavprobe.NAV_UPDATE";

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || !MAPS_PACKAGE.equals(sbn.getPackageName())) return;
        Notification n = sbn.getNotification();
        if (n == null) return;
        Bundle e = n.extras;
        String title = value(e, Notification.EXTRA_TITLE);
        String text = value(e, Notification.EXTRA_TEXT);
        String big = value(e, Notification.EXTRA_BIG_TEXT);
        String sub = value(e, Notification.EXTRA_SUB_TEXT);
        String summary = value(e, Notification.EXTRA_SUMMARY_TEXT);
        String[] lines = textLines(e);

        NavInstructionParser.Result parsed = NavInstructionParser.parse(title, text, big, sub, summary, lines);
        String raw = "title=" + title +
                "\ntext=" + text +
                "\nbigText=" + big +
                "\nsubText=" + sub +
                "\nsummary=" + summary +
                "\ntextLines=" + joinLines(lines) +
                "\n\nPARSED" +
                "\narrow=" + parsed.arrow +
                "\ndistance=" + parsed.distance +
                "\ninstruction=" + parsed.instruction +
                "\nroad=" + parsed.road +
                "\nsource=" + parsed.source;

        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        p.edit()
                .putString("arrow", parsed.arrow)
                .putString("distance", parsed.distance)
                .putString("primary", parsed.instruction)
                .putString("secondary", parsed.road)
                .putString("raw", raw)
                .putString("timestamp", now())
                .putBoolean("simulated", false)
                .apply();
        MapsNavWidget.updateAll(this);
        sendBroadcast(new Intent(ACTION_UPDATE).setPackage(getPackageName()));
    }

    @Override public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn == null || !MAPS_PACKAGE.equals(sbn.getPackageName())) return;
        // On ne vide pas immédiatement la dernière consigne : Google Maps peut remplacer
        // sa notification pendant une mise à jour. La prochaine notification prend le relais.
    }

    private static String value(Bundle b, String key) {
        if (b == null) return "";
        Object v = b.get(key);
        return v == null ? "" : String.valueOf(v).trim();
    }

    private static String[] textLines(Bundle b) {
        if (b == null) return new String[0];
        CharSequence[] raw = b.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (raw == null) return new String[0];
        String[] out = new String[raw.length];
        for (int i = 0; i < raw.length; i++) out[i] = raw[i] == null ? "" : raw[i].toString().trim();
        return out;
    }

    private static String joinLines(String[] lines) {
        if (lines == null || lines.length == 0) return "";
        StringBuilder s = new StringBuilder();
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) continue;
            if (s.length() > 0) s.append(" | ");
            s.append(line.trim());
        }
        return s.toString();
    }

    public static String now() {
        return new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
    }
}
