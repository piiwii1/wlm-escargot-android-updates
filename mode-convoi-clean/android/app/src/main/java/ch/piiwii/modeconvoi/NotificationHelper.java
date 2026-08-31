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
    public static final String TALKIE_CHANNEL = "convoy_talkie_v1";
    private static final int TALKIE_NOTIFICATION_ID = 22001;
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

    public static void ensureTalkieChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;
        NotificationChannel c = new NotificationChannel(
                TALKIE_CHANNEL,
                "Talkie-walkie Mode Convoi",
                NotificationManager.IMPORTANCE_HIGH);
        c.setDescription("Affiche brièvement le nom de la personne qui parle dans le convoi");
        c.enableVibration(false);
        c.setSound(null, null);
        c.enableLights(false);
        c.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        c.setShowBadge(false);
        nm.createNotificationChannel(c);
    }

    public static void notifyTalkieSpeaker(Context context, String speakerName) {
        if (Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        ensureTalkieChannel(context);
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;
        String who = speakerName == null || speakerName.trim().isEmpty() ? "Un participant" : speakerName.trim();
        Intent open = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                context,
                TALKIE_NOTIFICATION_ID,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(context, TALKIE_CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(Color.rgb(70,180,255))
                .setContentTitle("🔊 " + who + " parle")
                .setContentText("Talkie-walkie du convoi")
                .setContentIntent(pi)
                .setOngoing(true)
                .setOnlyAlertOnce(false)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_HIGH)
                .setShowWhen(false)
                .setTimeoutAfter(3500)
                .build();
        nm.notify(TALKIE_NOTIFICATION_ID, n);
    }

    public static void clearTalkieSpeaker(Context context) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null) nm.cancel(TALKIE_NOTIFICATION_ID);
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
