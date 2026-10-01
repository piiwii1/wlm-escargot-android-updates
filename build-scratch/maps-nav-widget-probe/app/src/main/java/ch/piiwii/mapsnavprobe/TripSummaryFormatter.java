package ch.piiwii.mapsnavprobe;

import android.text.TextUtils;

public final class TripSummaryFormatter {
    private TripSummaryFormatter() {}

    public static String build(String eta, String distance, String duration) {
        if (TextUtils.isEmpty(eta) && TextUtils.isEmpty(distance) && TextUtils.isEmpty(duration)) return "";
        String arrival = TextUtils.isEmpty(eta) ? "—" : eta;
        String remaining = TextUtils.isEmpty(distance) ? "— km" : distance;
        String time = TextUtils.isEmpty(duration) ? "—" : duration;
        return "ARRIVÉE   " + arrival + "\nDISTANCE  " + remaining + "\nDURÉE     " + time;
    }
}
