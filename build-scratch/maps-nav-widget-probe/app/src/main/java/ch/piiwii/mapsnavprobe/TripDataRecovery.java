package ch.piiwii.mapsnavprobe;

import android.text.TextUtils;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conservative fallback for total remaining distance.
 *
 * The parser's own trip-distance result is now retained when it is plausible
 * and different from the next manoeuvre. If Maps split duration, distance and
 * ETA over different text fields, we also aggregate those fields globally.
 */
public final class TripDataRecovery {
    private TripDataRecovery() {}

    private static final Pattern DISTANCE = Pattern.compile(
            "(?iu)(\\d+(?:[.,]\\d+)?)\\s*(km|m|kilomètre(?:s)?|kilometre(?:s)?|kilometer(?:s)?|kilometre(?:s)?|meter(?:s)?|metre(?:s)?|mètre(?:s)?)\\b");
    private static final Pattern DURATION = Pattern.compile(
            "(?iu)\\b(?:(\\d{1,2})\\s*(?:h|hr|hrs|heure|heures|hour|hours)\\s*)?(\\d{1,3})\\s*(?:min|mins|minute|minutes)\\b|\\b\\d{1,2}\\s*(?:h|hr|hrs|heure|heures|hour|hours)\\b");
    private static final Pattern ETA = Pattern.compile(
            "(?iu)(?<!\\d)([01]?\\d|2[0-3])[:h.]([0-5]\\d)(?:\\s*(?:AM|PM))?(?!\\d)");

    public static String recoverTripDistance(String[] values, String maneuverDistance, String parsedTripDistance) {
        double maneuverKm = parseKm(maneuverDistance);
        String best = "";
        double bestKm = -1d;

        // The main parser only fills parsedTripDistance from text that already
        // looks like a trip summary. Keep it when it cannot be the next-turn
        // distance. Previous code received this argument but ignored it.
        double parsedKm = parseKm(parsedTripDistance);
        if (isPlausibleTotal(parsedKm, maneuverKm)) {
            bestKm = parsedKm;
            best = format(parsedKm);
        }

        if (values == null || values.length == 0) return best;

        boolean hasDuration = false;
        boolean hasEta = false;
        for (String raw : values) {
            if (TextUtils.isEmpty(raw)) continue;
            String clean = raw.replace('\u00A0', ' ').replace('\u202F', ' ');
            if (DURATION.matcher(clean).find()) hasDuration = true;
            if (ETA.matcher(clean).find()) hasEta = true;
        }

        // Only scan standalone distance fields when the same notification also
        // exposes explicit duration AND ETA somewhere. They may be in separate
        // TextViews; requiring one combined line was the old failure mode.
        if (!(hasDuration && hasEta)) return best;

        for (String raw : values) {
            if (TextUtils.isEmpty(raw)) continue;
            String clean = raw.replace('\u00A0', ' ').replace('\u202F', ' ');

            Matcher m = DISTANCE.matcher(clean);
            while (m.find()) {
                if (isSpeed(clean, m.end(), m.group(2))) continue;

                double km = toKm(m.group(1), m.group(2));
                if (!isPlausibleTotal(km, maneuverKm)) continue;

                if (km > bestKm) {
                    bestKm = km;
                    best = format(km);
                }
            }
        }
        return best;
    }

    public static String recoverTripDistance(String[] values, String maneuverDistance) {
        return recoverTripDistance(values, maneuverDistance, "");
    }

    private static boolean isPlausibleTotal(double km, double maneuverKm) {
        if (km <= 0d || km > 5000d) return false;
        if (maneuverKm <= 0d) return true;

        // A journey total cannot normally be below the distance to the next
        // manoeuvre, and an exact duplicate is almost always that same field.
        if (km + 0.02d < maneuverKm) return false;
        double delta = Math.abs(km - maneuverKm);
        if (delta < Math.max(0.03d, maneuverKm * 0.01d)) return false;
        return true;
    }

    private static double toKm(String number, String unit) {
        try {
            double value = Double.parseDouble(number.replace(',', '.'));
            String u = unit.toLowerCase(Locale.ROOT);
            return isKmUnit(u) ? value : value / 1000d;
        } catch (Throwable ignored) {
            return -1d;
        }
    }

    private static boolean isSpeed(String source, int matchEnd, String unit) {
        if (!isKmUnit(unit.toLowerCase(Locale.ROOT))) return false;
        int end = Math.min(source.length(), matchEnd + 10);
        String tail = source.substring(matchEnd, end).toLowerCase(Locale.ROOT);
        return tail.matches("^\\s*(?:/\\s*h|/\\s*heure|par\\s+heure).*" );
    }

    private static boolean isKmUnit(String unit) {
        return "km".equals(unit) || unit.startsWith("kilo");
    }

    private static String format(double km) {
        if (km < 1d) return String.format(Locale.FRANCE, "%.0f m", km * 1000d);
        if (Math.abs(km - Math.rint(km)) < 0.05d) {
            return String.format(Locale.FRANCE, "%.0f km", km);
        }
        return String.format(Locale.FRANCE, "%.1f km", km);
    }

    private static double parseKm(String value) {
        if (TextUtils.isEmpty(value)) return -1d;
        String clean = value.replace('\u00A0', ' ').replace('\u202F', ' ');
        Matcher m = DISTANCE.matcher(clean);
        if (!m.find()) return -1d;
        return toKm(m.group(1), m.group(2));
    }
}
