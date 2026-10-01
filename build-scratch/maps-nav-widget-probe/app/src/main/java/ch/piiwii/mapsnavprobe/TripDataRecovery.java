package ch.piiwii.mapsnavprobe;

import android.text.TextUtils;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conservative fallback for total remaining distance. A distance is accepted
 * only when it appears in the same text as a trip duration and an ETA.
 * Standalone values such as "300 m" are therefore never promoted to the
 * journey distance.
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
        if (values == null || values.length == 0) return "";

        double maneuverKm = parseKm(maneuverDistance);
        String best = "";
        double bestKm = -1d;

        for (String raw : values) {
            if (TextUtils.isEmpty(raw)) continue;
            String clean = raw.replace('\u00A0', ' ').replace('\u202F', ' ');

            // This is the key safety rule: a trip total must come from a
            // footer/summary containing both remaining time and arrival time.
            if (!DURATION.matcher(clean).find() || !ETA.matcher(clean).find()) continue;

            Matcher m = DISTANCE.matcher(clean);
            while (m.find()) {
                if (isSpeed(clean, m.end(), m.group(2))) continue;

                double km = toKm(m.group(1), m.group(2));
                if (km <= 0d || km > 5000d) continue;

                // If the same value as the next manoeuvre leaks into a summary,
                // do not use it as a fallback total. The dedicated Maps-layout
                // extractor is allowed to accept it when it is truly verified.
                if (maneuverKm > 0d) {
                    double delta = Math.abs(km - maneuverKm);
                    if (delta < Math.max(0.03d, maneuverKm * 0.01d)) continue;
                    if (km + 0.02d < maneuverKm) continue;
                }

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
