package ch.piiwii.mapsnavprobe;

import android.text.TextUtils;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recovers total trip distance when Google Maps exposes it in a notification
 * field that is not recognised by NavInstructionParser's summary heuristics.
 */
public final class TripDataRecovery {
    private TripDataRecovery() {}

    private static final Pattern DISTANCE = Pattern.compile(
            "(?iu)(\\d+(?:[.,]\\d+)?)\\s*(km|m|kilomètre(?:s)?|kilometre(?:s)?|meter(?:s)?|metre(?:s)?|mètre(?:s)?)\\b");

    public static String recoverTripDistance(String[] values, String maneuverDistance) {
        if (values == null || values.length == 0) return "";

        double maneuverKm = parseKm(maneuverDistance);
        double bestKm = -1d;

        for (String raw : values) {
            if (TextUtils.isEmpty(raw)) continue;
            Matcher m = DISTANCE.matcher(raw.replace('\u00A0', ' '));
            while (m.find()) {
                double value;
                try {
                    value = Double.parseDouble(m.group(1).replace(',', '.'));
                } catch (Throwable ignored) {
                    continue;
                }

                String unit = m.group(2).toLowerCase(Locale.ROOT);
                double km = unit.startsWith("kilo") || "km".equals(unit) ? value : value / 1000d;
                if (km <= 0d || km > 5000d) continue;

                // Total remaining distance cannot be smaller than the distance
                // to the next manoeuvre. Give priority to the largest plausible
                // distance exposed anywhere in the notification.
                if (maneuverKm > 0d && km + 0.02d < maneuverKm) continue;
                if (km > bestKm) bestKm = km;
            }
        }

        if (bestKm < 0d) return "";
        if (bestKm < 1d) return String.format(Locale.FRANCE, "%.0f m", bestKm * 1000d);
        if (Math.abs(bestKm - Math.rint(bestKm)) < 0.05d) {
            return String.format(Locale.FRANCE, "%.0f km", bestKm);
        }
        return String.format(Locale.FRANCE, "%.1f km", bestKm);
    }

    private static double parseKm(String value) {
        if (TextUtils.isEmpty(value)) return -1d;
        Matcher m = DISTANCE.matcher(value.replace('\u00A0', ' '));
        if (!m.find()) return -1d;
        try {
            double number = Double.parseDouble(m.group(1).replace(',', '.'));
            String unit = m.group(2).toLowerCase(Locale.ROOT);
            return unit.startsWith("kilo") || "km".equals(unit) ? number : number / 1000d;
        } catch (Throwable ignored) {
            return -1d;
        }
    }
}
