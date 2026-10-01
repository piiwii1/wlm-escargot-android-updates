package ch.piiwii.mapsnavprobe;

import android.app.Notification;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
        String[] lines = allTextValues(e);

        NavInstructionParser.Result parsed = NavInstructionParser.parse(title, text, big, sub, summary, lines);
        String recoveredTripDistance = parsed.tripDistance;
        if (TextUtils.isEmpty(recoveredTripDistance)) {
            recoveredTripDistance = TripDataRecovery.recoverTripDistance(lines, parsed.distance);
        }

        String raw = "title=" + title +
                "\ntext=" + text +
                "\nbigText=" + big +
                "\nsubText=" + sub +
                "\nsummary=" + summary +
                "\nallText=" + joinLines(lines) +
                "\n\nPARSED" +
                "\narrow=" + parsed.arrow +
                "\ndistance=" + parsed.distance +
                "\ninstruction=" + parsed.instruction +
                "\nroad=" + parsed.road +
                "\neta=" + parsed.eta +
                "\ntripDistance=" + recoveredTripDistance +
                "\ntripDuration=" + parsed.tripDuration +
                "\nsource=" + parsed.source;

        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        SharedPreferences.Editor edit = p.edit()
                .putString("arrow", parsed.arrow)
                .putString("distance", parsed.distance)
                .putString("primary", parsed.instruction)
                .putString("secondary", parsed.road)
                .putString("raw", raw)
                .putString("timestamp", now())
                .putBoolean("simulated", false);

        if (!TextUtils.isEmpty(parsed.eta)) edit.putString("eta", parsed.eta);
        if (!TextUtils.isEmpty(recoveredTripDistance)) edit.putString("trip_distance", recoveredTripDistance);
        if (!TextUtils.isEmpty(parsed.tripDuration)) edit.putString("trip_duration", parsed.tripDuration);

        if ("Vous êtes arrivé".equals(parsed.instruction)) {
            edit.remove("eta").remove("trip_distance").remove("trip_duration");
        }
        edit.apply();

        MapsNavWidget.updateAll(this);
        sendBroadcast(new Intent(ACTION_UPDATE).setPackage(getPackageName()));
    }

    @Override public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn == null || !MAPS_PACKAGE.equals(sbn.getPackageName())) return;
        // Google Maps remplace souvent sa notification pendant une mise à jour.
        // On garde donc la dernière consigne et le résumé trajet jusqu'à la suivante.
    }

    private static String value(Bundle b, String key) {
        if (b == null) return "";
        Object v = b.get(key);
        return v == null ? "" : String.valueOf(v).trim();
    }

    private static String[] allTextValues(Bundle b) {
        if (b == null) return new String[0];
        ArrayList<String> out = new ArrayList<>();
        for (String key : b.keySet()) {
            Object v;
            try { v = b.get(key); } catch (Throwable ignored) { continue; }
            if (v instanceof CharSequence) addUnique(out, v.toString());
            else if (v instanceof CharSequence[]) {
                for (CharSequence s : (CharSequence[]) v) if (s != null) addUnique(out, s.toString());
            }
        }
        return out.toArray(new String[0]);
    }

    private static void addUnique(ArrayList<String> out, String value) {
        if (value == null) return;
        String clean = value.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
        if (clean.isEmpty()) return;
        for (String existing : out) if (existing.equals(clean)) return;
        out.add(clean);
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
