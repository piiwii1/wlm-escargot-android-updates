package ch.piiwii.roadbookgeneve;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class Itinerary {
    public static final String PREFS = "roadbook_prefs";
    public static final LocalDate DAY = LocalDate.of(2026, 8, 29);
    public static final ZoneId ZONE = ZoneId.of("Europe/Zurich");
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    public static final String AROMAT_DESTINATION = "Maison St-Charles, Rue du Château 126, 1680 Romont, Suisse";
    public static final String HOME_DESTINATION = "Sion, Suisse";

    public static class Stop {
        public final String id;
        public final String defaultTime;
        public final String title;
        public final String note;
        public final String destination;
        public final String gpsLabel;
        public final boolean alertAtTime;
        public final int alertBeforeMinutes;
        public final boolean fixedEvent;

        public Stop(String id, String defaultTime, String title, String note,
                    String destination, String gpsLabel,
                    boolean alertAtTime, int alertBeforeMinutes, boolean fixedEvent) {
            this.id = id;
            this.defaultTime = defaultTime;
            this.title = title;
            this.note = note;
            this.destination = destination;
            this.gpsLabel = gpsLabel;
            this.alertAtTime = alertAtTime;
            this.alertBeforeMinutes = alertBeforeMinutes;
            this.fixedEvent = fixedEvent;
        }
    }

    public static final Stop[] STOPS = new Stop[] {
            new Stop(
                    "depart_sion", "13:30", "Départ de Sion",
                    "La marge de circulation vers Rolle est déjà prévue.",
                    null, null, true, 15, false),
            new Stop(
                    "rolle", "14:55", "Meeting VW — Rolle",
                    "Château de Rolle • profite du meeting jusqu'à environ 16h50.",
                    "Château de Rolle, Grand-Rue 39, 1180 Rolle, Suisse", "Rolle",
                    false, 0, false),
            new Stop(
                    "leave_rolle", "16:50", "Départ pour Genève",
                    "Finis tranquillement ton tour et prends la route.",
                    null, null, true, 15, false),
            new Stop(
                    "geneva_rp", "18:00", "Geneva Vice / Geneva RP",
                    "Village du Soir • P+R Bachet-Praille conseillé à proximité.",
                    "Village du Soir, Route des Jeunes 24, 1212 Lancy, Suisse", "Geneva Vice",
                    false, 0, true),
            new Stop(
                    "leave_village", "20:45", "Départ pour Rêve d'Eau",
                    "Marge pour circulation, parking au centre et marche jusqu'au Jardin Anglais.",
                    null, null, true, 15, false),
            new Stop(
                    "rock", "22:00", "Rêve d'Eau — Rock & Pop",
                    "Jardin Anglais • vise une arrivée vers 21h30–21h40 pour bien te placer.",
                    "Jardin Anglais, Quai du Général-Guisan 34, 1204 Genève, Suisse", "Rêve d'Eau",
                    true, 20, true),
            new Stop(
                    "choice", "22:20", "Choix pour la suite",
                    "Classique 22h30, Nuit de l'Aromat à Romont, retour Sion ou autre destination.",
                    null, null, true, 0, false)
    };

    private Itinerary() {}

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String getTime(Context context, Stop stop) {
        return prefs(context).getString("time_" + stop.id, stop.defaultTime);
    }

    public static void setTime(Context context, Stop stop, int hour, int minute) {
        String value = String.format(java.util.Locale.ROOT, "%02d:%02d", hour, minute);
        prefs(context).edit().putString("time_" + stop.id, value).apply();
    }

    public static long getTimeMillis(Context context, Stop stop) {
        String[] parts = getTime(context, stop).split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        LocalDateTime local = DAY.atTime(hour, minute);
        ZonedDateTime zoned = local.atZone(ZONE);
        return zoned.toInstant().toEpochMilli();
    }

    public static long classicMillis() {
        return DAY.atTime(22, 30).atZone(ZONE).toInstant().toEpochMilli();
    }

    public static String formatTime(long millis) {
        return ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), ZONE).format(HHMM);
    }
}
