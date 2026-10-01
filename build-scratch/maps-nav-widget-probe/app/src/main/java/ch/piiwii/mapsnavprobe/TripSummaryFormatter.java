package ch.piiwii.mapsnavprobe;

import android.text.TextUtils;

public final class TripSummaryFormatter {
    private TripSummaryFormatter() {}

    public static String build(String eta, String distance, String duration) {
        StringBuilder out = new StringBuilder();
        append(out, TextUtils.isEmpty(eta) ? "" : "Arrivée " + eta);
        append(out, distance);
        append(out, duration);
        return out.toString();
    }

    private static void append(StringBuilder out, String value) {
        if (TextUtils.isEmpty(value)) return;
        if (out.length() > 0) out.append("  •  ");
        out.append(value);
    }
}
