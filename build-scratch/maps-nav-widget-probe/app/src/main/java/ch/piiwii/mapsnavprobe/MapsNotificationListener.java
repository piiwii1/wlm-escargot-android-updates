package ch.piiwii.mapsnavprobe;

import android.app.Notification;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import java.text.SimpleDateFormat;
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
        String primary = firstUseful(text, big, title);
        String secondary = joinNonEmpty(sub, summary, (!title.equals(primary) ? title : ""));
        String raw = "title=" + title + "\ntext=" + text + "\nbigText=" + big + "\nsubText=" + sub + "\nsummary=" + summary;
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        p.edit().putString("primary", primary).putString("secondary", secondary).putString("raw", raw).putString("timestamp", now()).putBoolean("simulated", false).apply();
        MapsNavWidget.updateAll(this);
        sendBroadcast(new Intent(ACTION_UPDATE).setPackage(getPackageName()));
    }

    private static String value(Bundle b, String key) { if (b == null) return ""; Object v = b.get(key); return v == null ? "" : String.valueOf(v).trim(); }
    private static String firstUseful(String... values) { for (String v : values) if (!TextUtils.isEmpty(v) && !"Google Maps".equalsIgnoreCase(v) && !"Maps".equalsIgnoreCase(v)) return v; return "Google Maps : notification reçue"; }
    private static String joinNonEmpty(String... values) { StringBuilder s = new StringBuilder(); for (String v : values) { if (TextUtils.isEmpty(v) || "Google Maps".equalsIgnoreCase(v) || "Maps".equalsIgnoreCase(v)) continue; if (s.length() > 0) s.append(" · "); s.append(v); } return s.toString(); }
    public static String now() { return new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()); }
}
