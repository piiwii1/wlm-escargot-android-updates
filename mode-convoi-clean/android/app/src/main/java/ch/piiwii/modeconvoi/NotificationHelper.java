package ch.piiwii.modeconvoi;

import android.Manifest;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import org.json.JSONObject;

public final class NotificationHelper {
    public static final String ALERTS_CHANNEL = "convoy_alerts_v2";
    private static final int ACCENT = Color.rgb(255,181,20);

    private NotificationHelper() {}

    public static void ensureAlertChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;
        NotificationChannel c = new NotificationChannel(
                ALERTS_CHANNEL,
                "Alertes Mode Convoi",
                NotificationManager.IMPORTANCE_HIGH);
        c.setDescription("Alertes des participants, regroupements et événements importants du convoi");
        c.enableVibration(true);
        c.setVibrationPattern(new long[]{0,220,110,260});
        c.enableLights(true);
        c.setLightColor(ACCENT);
        c.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        c.setShowBadge(true);
        nm.createNotificationChannel(c);
    }

    public static void notifyEvent(Context context, JSONObject event) {
        if (Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        ensureAlertChannel(context);
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;
        long eventId = event.optLong("id");
        Intent open = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                context,
                (int)(eventId % 100000),
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String title = event.optString("participantName", "Mode Convoi");
        String msg = event.optString("label", "Nouvelle information");
        Notification n = new Notification.Builder(context, ALERTS_CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ACCENT)
                .setContentTitle(title)
                .setContentText(msg)
                .setStyle(new Notification.BigTextStyle().bigText(msg))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_HIGH)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .build();
        nm.notify((int)(10000 + eventId % 100000), n);
    }
}
