package ch.piiwii.roadbookgeneve;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public final class AlarmScheduler {
    public static final String PREF_ENABLED = "alerts_enabled";

    private AlarmScheduler() {}

    public static boolean isEnabled(Context context) {
        return Itinerary.prefs(context).getBoolean(PREF_ENABLED, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        Itinerary.prefs(context).edit().putBoolean(PREF_ENABLED, enabled).apply();
        if (enabled) scheduleAll(context);
        else cancelAll(context);
    }

    public static boolean canExact(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return am != null && am.canScheduleExactAlarms();
    }

    public static void scheduleAll(Context context) {
        cancelAll(context);
        if (!isEnabled(context)) return;

        int index = 0;
        for (Itinerary.Stop stop : Itinerary.STOPS) {
            long at = Itinerary.getTimeMillis(context, stop);
            if (stop.alertBeforeMinutes > 0) {
                schedule(context, at - stop.alertBeforeMinutes * 60_000L,
                        1000 + index * 2,
                        "Dans " + stop.alertBeforeMinutes + " min",
                        reminderText(stop));
            }
            if (stop.alertAtTime) {
                schedule(context, at,
                        1001 + index * 2,
                        stop.title,
                        notificationText(stop));
            }
            index++;
        }
    }

    private static String reminderText(Itinerary.Stop stop) {
        if ("depart_sion".equals(stop.id)) return "Prépare-toi : départ de Sion à " + stop.defaultTime + ".";
        if ("leave_rolle".equals(stop.id)) return "Il reste 15 min avant de quitter le meeting pour Genève.";
        if ("leave_village".equals(stop.id)) return "Il reste 15 min avant de partir vers le Jardin Anglais.";
        if ("rock".equals(stop.id)) return "Rock & Pop à 22h00 : commence à te placer au Jardin Anglais.";
        return stop.title + " à " + stop.defaultTime;
    }

    private static String notificationText(Itinerary.Stop stop) {
        if ("depart_sion".equals(stop.id)) return "C'est l'heure de partir pour Rolle.";
        if ("leave_rolle".equals(stop.id)) return "Direction Genève / Geneva Vice.";
        if ("leave_village".equals(stop.id)) return "Pars maintenant vers le Jardin Anglais.";
        if ("rock".equals(stop.id)) return "La séance Rock & Pop commence maintenant.";
        if ("choice".equals(stop.id)) return "22h20 : choisis la suite selon la motivation.";
        return stop.note;
    }

    private static void schedule(Context context, long triggerAtMillis, int requestCode, String title, String text) {
        if (triggerAtMillis <= System.currentTimeMillis()) return;
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("text", text);
        intent.putExtra("notification_id", requestCode);
        PendingIntent pi = PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
        }
    }

    public static void cancelAll(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        for (int index = 0; index < Itinerary.STOPS.length; index++) {
            cancel(context, am, 1000 + index * 2);
            cancel(context, am, 1001 + index * 2);
        }
    }

    private static void cancel(Context context, AlarmManager am, int requestCode) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) {
            am.cancel(pi);
            pi.cancel();
        }
    }
}
