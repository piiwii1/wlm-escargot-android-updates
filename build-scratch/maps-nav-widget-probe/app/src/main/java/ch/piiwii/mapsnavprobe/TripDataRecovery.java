package ch.piiwii.mapsnavprobe;

import android.text.TextUtils;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recovers total remaining distance from every textual value exposed by
 * Google Maps' notification. This is deliberately independent from the
 * manoeuvre parser so a distance placed in an unusual notification field
 * can still be displayed.
 */
public final class TripDataRecovery {
    private TripDataRecovery() {}

    private static final Pattern DISTANCE = Pattern.compile(
            "(?iu)(\\d+(?:[.,]\\d+)?)\\s*(km|m|kilomètre(?:s)?|kilometre(?:s)?|kilometer(?:s)?|kilometre(?:s)?|meter(?:s)?|metre(?:s)?|mètre(?:s)?)\\b");

    /**
     * Returns the most plausible TOTAL remaining distance.
     * The total must normally be greater than the distance to the next
     * manoeuvre, which prevents e.g. \"600 m\" from being reused as the
     * trip distance when Google Maps did not expose a total.
     */
    public static String recoverTripDistance(String[] values, String maneuverDistance, String parsedTripDistance) {
        double maneuverKm = parseKm(maneuverDistance);
        double bestKm = -1d;

        // Keep the parser result as a candidate, but do not trust it blindly:
        // older heuristics could accidentally capture the manoeuvre distance.
        double parsedKm = parseKm(parsedTripDistance);
        if (isPlausibleTotal(parsedKm, maneuverKm)) bestKm = parsedKm;

        if (values != null) {
            for (String raw : values) {
                if (TextUtils.isEmpty(raw)) continue;
                String clean = raw.replace('\u00A0', ' ').replace('\u202F', ' ');
                Matcher m = DISTANCE.matcher(clean);
                while (m.find()) {
                    if (isSpeed(clean, m.end(), m.group(2))) continue;

                    double value;
                    try {
                        value = Double.parseDouble(m.group(1).replace(',', '.'));
                    } catch (Throwable ignored) {
                        continue;
                    }

                    String unit = m.group(2).toLowerCase(Locale.ROOT);
                    double km = isKmUnit(unit) ? value : value / 1000d;
                    if (!isPlausibleTotal(km, maneuverKm)) continue;
                    if (km > bestKm) bestKm = km;
                }
            }
        }

        if (bestKm < 0d) return "";
        return format(bestKm);
    }

    // Compatibility with older callers.
    public static String recoverTripDistance(String[] values, String maneuverDistance) {
        return recoverTripDistance(values, maneuverDistance, "");
    }

    private static boolean isPlausibleTotal(double km, double maneuverKm) {
        if (km <= 0d || km > 5000d) return false;
        if (maneuverKm <= 0d) return true;

        // The route total cannot be below the next-manoeuvre distance. Also
        // reject an exact duplicate of the manoeuvre value: that is almost
        // always the same field seen twice rather than a real trip total.
        if (km + 0.02d < maneuverKm) return false;
        double delta = Math.abs(km - maneuverKm);
        if (delta < Math.max(0.03d, maneuverKm * 0.01d)) return false;
        return true;
    }

    private static boolean isSpeed(String source, int matchEnd, String unit) {
        if (!isKmUnit(unit)) return false;
        int end = Math.min(source.length(), matchEnd + 8);
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
        try {
            double number = Double.parseDouble(m.group(1).replace(',', '.'));
            String unit = m.group(2).toLowerCase(Locale.ROOT);
            return isKmUnit(unit) ? number : number / 1000d;
        } catch (Throwable ignored) {
            return -1d;
        }
    }
}
