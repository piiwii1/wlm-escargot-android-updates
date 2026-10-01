package ch.piiwii.mapsnavprobe;

import android.app.Notification;
import android.os.Bundle;
import android.text.TextUtils;

import java.lang.reflect.Array;
import java.util.Locale;

/**
 * Reads Android ProgressStyle navigation data without touching the widget/UI.
 *
 * ProgressStyle uses one numeric coordinate system for progress, segments and
 * points. Google Maps can expose route progression there even when the visible
 * notification only contains an ETA. We only promote it to a trip distance
 * when the scale looks route-like and is not the next-manoeuvre distance.
 */
public final class ProgressStyleDistanceExtractor {
    private ProgressStyleDistanceExtractor() {}

    private static final String K_PROGRESS = "android.progress";
    private static final String K_PROGRESS_MAX = "android.progressMax";
    private static final String K_INDETERMINATE = "android.progressIndeterminate";
    private static final String K_SEGMENTS = "android.progressSegments";
    private static final String K_POINTS = "android.progressPoints";
    private static final String K_TEMPLATE = "android.template";

    public static final class Result {
        public final String distance;
        public final String debug;
        public final int progress;
        public final int progressMax;
        public final int segmentSum;

        Result(String distance, String debug, int progress, int progressMax, int segmentSum) {
            this.distance = distance;
            this.debug = debug;
            this.progress = progress;
            this.progressMax = progressMax;
            this.segmentSum = segmentSum;
        }
    }

    public static Result extract(Notification notification, String maneuverDistance) {
        if (notification == null || notification.extras == null) {
            return new Result("", "no extras", -1, -1, -1);
        }

        Bundle e = notification.extras;
        int progress = intValue(e.get(K_PROGRESS), -1);
        int explicitMax = intValue(e.get(K_PROGRESS_MAX), -1);
        boolean indeterminate = boolValue(e.get(K_INDETERMINATE));
        int segmentSum = sumSegments(e.get(K_SEGMENTS));
        int progressMax = segmentSum > 0 ? segmentSum : explicitMax;

        String template = stringValue(e.get(K_TEMPLATE));
        boolean progressStyle = containsIgnoreCase(template, "ProgressStyle")
                || e.containsKey(K_SEGMENTS)
                || e.containsKey(K_POINTS)
                || (e.containsKey(K_PROGRESS) && e.containsKey(K_PROGRESS_MAX));

        StringBuilder debug = new StringBuilder();
        append(debug, "template", template);
        append(debug, "progress", String.valueOf(progress));
        append(debug, "progressMaxExtra", String.valueOf(explicitMax));
        append(debug, "segmentSum", String.valueOf(segmentSum));
        append(debug, "effectiveMax", String.valueOf(progressMax));
        append(debug, "indeterminate", String.valueOf(indeterminate));
        append(debug, "segments", describe(e.get(K_SEGMENTS), 0));
        append(debug, "points", describe(e.get(K_POINTS), 0));
        append(debug, "numericExtras", numericExtras(e));

        if (!progressStyle || indeterminate || progress < 0 || progressMax <= 0 || progress > progressMax) {
            return new Result("", debug.toString(), progress, progressMax, segmentSum);
        }

        int remainingUnits = progressMax - progress;
        if (remainingUnits <= 0) {
            return new Result("", debug.toString(), progress, progressMax, segmentSum);
        }

        /*
         * A default ProgressStyle without segments normally uses a 0..100
         * percentage scale. Never interpret that as metres. A larger explicit
         * scale, or a real segment scale whose lengths sum to the max, is the
         * only case we accept as a distance candidate.
         */
        boolean routeLikeScale = segmentSum > 100 || explicitMax > 100;
        if (!routeLikeScale) {
            append(debug, "candidateRejected", "percentage/default scale");
            return new Result("", debug.toString(), progress, progressMax, segmentSum);
        }

        double maneuverMeters = parseMeters(maneuverDistance);
        double remainingMeters = remainingUnits;

        // Reject obvious collisions with the immediate next-turn distance.
        if (maneuverMeters > 0) {
            double delta = Math.abs(remainingMeters - maneuverMeters);
            if (delta < Math.max(20d, maneuverMeters * 0.02d)) {
                append(debug, "candidateRejected", "same as manoeuvre distance");
                return new Result("", debug.toString(), progress, progressMax, segmentSum);
            }
            if (remainingMeters + 20d < maneuverMeters) {
                append(debug, "candidateRejected", "below manoeuvre distance");
                return new Result("", debug.toString(), progress, progressMax, segmentSum);
            }
        }

        // Navigation totals above 5000 km are not useful here and usually mean
        // that the ProgressStyle units are not metres.
        if (remainingMeters > 5_000_000d) {
            append(debug, "candidateRejected", "implausible metre scale");
            return new Result("", debug.toString(), progress, progressMax, segmentSum);
        }

        String candidate = formatMeters(remainingMeters);
        append(debug, "candidate", candidate);
        return new Result(candidate, debug.toString(), progress, progressMax, segmentSum);
    }

    private static int sumSegments(Object value) {
        if (!(value instanceof Iterable)) return -1;
        long sum = 0;
        int count = 0;
        try {
            for (Object item : (Iterable<?>) value) {
                if (!(item instanceof Bundle)) continue;
                int length = intValue(((Bundle) item).get("length"), -1);
                if (length <= 0) continue;
                sum += length;
                count++;
                if (sum > Integer.MAX_VALUE) return -1;
            }
        } catch (Throwable ignored) {
            return -1;
        }
        return count == 0 ? -1 : (int) sum;
    }

    private static String numericExtras(Bundle bundle) {
        StringBuilder out = new StringBuilder();
        if (bundle == null) return "";
        try {
            for (String key : bundle.keySet()) {
                Object value;
                try { value = bundle.get(key); } catch (Throwable t) { continue; }
                if (value instanceof Number || value instanceof Boolean) {
                    if (out.length() > 0) out.append(", ");
                    out.append(key).append('=').append(value)
                            .append('(').append(value.getClass().getSimpleName()).append(')');
                }
            }
        } catch (Throwable ignored) {}
        return out.toString();
    }

    private static String describe(Object value, int depth) {
        if (value == null) return "null";
        if (depth > 3) return value.getClass().getSimpleName();
        if (value instanceof Bundle) {
            StringBuilder s = new StringBuilder("{");
            Bundle b = (Bundle) value;
            try {
                for (String key : b.keySet()) {
                    if (s.length() > 1) s.append(", ");
                    Object child;
                    try { child = b.get(key); } catch (Throwable t) { child = "<error>"; }
                    s.append(key).append('=').append(describe(child, depth + 1));
                    if (s.length() > 900) { s.append("…"); break; }
                }
            } catch (Throwable ignored) {}
            return s.append('}').toString();
        }
        if (value instanceof Iterable) {
            StringBuilder s = new StringBuilder("[");
            int count = 0;
            try {
                for (Object child : (Iterable<?>) value) {
                    if (count++ > 0) s.append(", ");
                    s.append(describe(child, depth + 1));
                    if (count >= 20 || s.length() > 1200) { s.append("…"); break; }
                }
            } catch (Throwable ignored) {}
            return s.append(']').toString();
        }
        Class<?> c = value.getClass();
        if (c.isArray()) {
            StringBuilder s = new StringBuilder("[");
            int n = Math.min(Array.getLength(value), 20);
            for (int i = 0; i < n; i++) {
                if (i > 0) s.append(", ");
                s.append(describe(Array.get(value, i), depth + 1));
            }
            if (Array.getLength(value) > n) s.append("…");
            return s.append(']').toString();
        }
        String text = String.valueOf(value);
        if (text.length() > 240) text = text.substring(0, 240) + "…";
        return c.getSimpleName() + ':' + text;
    }

    private static int intValue(Object value, int fallback) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value == null) return fallback;
        try { return Integer.parseInt(String.valueOf(value)); }
        catch (Throwable ignored) { return fallback; }
    }

    private static boolean boolValue(Object value) {
        if (value instanceof Boolean) return (Boolean) value;
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static boolean containsIgnoreCase(String value, String needle) {
        return !TextUtils.isEmpty(value) && value.toLowerCase(Locale.ROOT)
                .contains(needle.toLowerCase(Locale.ROOT));
    }

    private static void append(StringBuilder s, String key, String value) {
        if (s.length() > 0) s.append(" | ");
        s.append(key).append('=').append(value == null ? "" : value);
    }

    private static double parseMeters(String value) {
        if (TextUtils.isEmpty(value)) return -1d;
        String clean = value.replace('\u00A0', ' ').replace('\u202F', ' ')
                .toLowerCase(Locale.ROOT).trim();
        try {
            String number = clean.replaceAll("[^0-9,.]", "").replace(',', '.');
            if (number.isEmpty()) return -1d;
            double v = Double.parseDouble(number);
            if (clean.contains("km") || clean.contains("kilo")) return v * 1000d;
            if (clean.contains("m")) return v;
        } catch (Throwable ignored) {}
        return -1d;
    }

    private static String formatMeters(double meters) {
        if (meters < 1000d) return String.format(Locale.FRANCE, "%.0f m", meters);
        double km = meters / 1000d;
        if (Math.abs(km - Math.rint(km)) < 0.05d) {
            return String.format(Locale.FRANCE, "%.0f km", km);
        }
        return String.format(Locale.FRANCE, "%.1f km", km);
    }
}
