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

        String[] extraLines = allTextValues(n);
        String[] renderedLines = NotificationViewTextExtractor.extract(this, n);
        String[] lines = mergeUnique(extraLines, renderedLines);

        NavInstructionParser.Result parsed = NavInstructionParser.parse(title, text, big, sub, summary, lines);

        // Strongest source: Google Maps' own notification layout. On many
        // versions nav_time/header_text contains "duration · distance · ETA".
        GoogleMapsTripInfoExtractor.Result mapsTrip = GoogleMapsTripInfoExtractor.extract(this, sbn);

        String recoveredTripDistance = mapsTrip.distance;
        if (TextUtils.isEmpty(recoveredTripDistance)) {
            recoveredTripDistance = TripDataRecovery.recoverTripDistance(
                    lines, parsed.distance, parsed.tripDistance);
        }

        String finalEta = !TextUtils.isEmpty(mapsTrip.eta) ? mapsTrip.eta : parsed.eta;
        String finalDuration = !TextUtils.isEmpty(mapsTrip.duration) ? mapsTrip.duration : parsed.tripDuration;

        String raw = "title=" + title +
                "\ntext=" + text +
                "\nbigText=" + big +
                "\nsubText=" + sub +
                "\nsummary=" + summary +
                "\nextraText=" + joinLines(extraLines) +
                "\nrenderedText=" + joinLines(renderedLines) +
                "\nlayoutText=" + mapsTrip.debug +
                "\nallText=" + joinLines(lines) +
                "\n\nPARSED" +
                "\narrow=" + parsed.arrow +
                "\ndistance=" + parsed.distance +
                "\ninstruction=" + parsed.instruction +
                "\nroad=" + parsed.road +
                "\netaParser=" + parsed.eta +
                "\netaLayout=" + mapsTrip.eta +
                "\netaFinal=" + finalEta +
                "\ntripDistanceParser=" + parsed.tripDistance +
                "\ntripDistanceLayout=" + mapsTrip.distance +
                "\ntripDistanceFinal=" + recoveredTripDistance +
                "\ntripDurationParser=" + parsed.tripDuration +
                "\ntripDurationLayout=" + mapsTrip.duration +
                "\ntripDurationFinal=" + finalDuration +
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

        if (!TextUtils.isEmpty(finalEta)) edit.putString("eta", finalEta);
        if (!TextUtils.isEmpty(recoveredTripDistance)) edit.putString("trip_distance", recoveredTripDistance);
        if (!TextUtils.isEmpty(finalDuration)) edit.putString("trip_duration", finalDuration);

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

    private static String[] allTextValues(Notification n) {
        ArrayList<String> out = new ArrayList<>();
        if (n == null) return new String[0];

        collectValue(out, n.extras, 0);
        if (n.tickerText != null) addUnique(out, n.tickerText.toString());

        if (n.actions != null) {
            for (Notification.Action action : n.actions) {
                if (action != null && action.title != null) addUnique(out, action.title.toString());
                if (action != null && action.getExtras() != null) collectValue(out, action.getExtras(), 0);
            }
        }

        if (n.publicVersion != null && n.publicVersion != n) {
            collectValue(out, n.publicVersion.extras, 0);
            if (n.publicVersion.tickerText != null) addUnique(out, n.publicVersion.tickerText.toString());
        }

        return out.toArray(new String[0]);
    }

    private static void collectValue(ArrayList<String> out, Object value, int depth) {
        if (value == null || depth > 6) return;

        if (value instanceof CharSequence) {
            addUnique(out, value.toString());
            return;
        }
        if (value instanceof Bundle) {
            Bundle b = (Bundle) value;
            for (String key : b.keySet()) {
                try { collectValue(out, b.get(key), depth + 1); }
                catch (Throwable ignored) {}
            }
            return;
        }
        if (value instanceof CharSequence[]) {
            for (CharSequence item : (CharSequence[]) value) collectValue(out, item, depth + 1);
            return;
        }
        if (value instanceof Object[]) {
            for (Object item : (Object[]) value) collectValue(out, item, depth + 1);
            return;
        }
        if (value instanceof Iterable) {
            try {
                for (Object item : (Iterable<?>) value) collectValue(out, item, depth + 1);
            } catch (Throwable ignored) {}
        }
    }

    private static String[] mergeUnique(String[] first, String[] second) {
        ArrayList<String> out = new ArrayList<>();
        if (first != null) for (String s : first) addUnique(out, s);
        if (second != null) for (String s : second) addUnique(out, s);
        return out.toArray(new String[0]);
    }

    private static void addUnique(ArrayList<String> out, String value) {
        if (value == null) return;
        String clean = value.replace('\u00A0', ' ').replace('\u202F', ' ')
                .replaceAll("\\s+", " ").trim();
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
