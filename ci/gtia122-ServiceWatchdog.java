package ch.piiwii.gtialtimeter;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

final class ServiceWatchdog {
    static final String ACTION = "ch.piiwii.gtialtimeter.WATCHDOG";
    static final long NORMAL_DELAY_MS = 120000L;
    private static final int REQUEST_CODE = 1043;

    private ServiceWatchdog() {}

    static boolean hasWidgets(Context context) {
        int[] ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(new ComponentName(context, GTIAltimeterWidgetProvider.class));
        return ids != null && ids.length > 0;
    }

    static void schedule(Context context, long delayMs) {
        if (!hasWidgets(context)) return;
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        long at = System.currentTimeMillis() + Math.max(5000L, delayMs);
        PendingIntent pi = pendingIntent(context);
        if (Build.VERSION.SDK_INT >= 23) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        } else {
            am.set(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }

    static void cancel(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(pendingIntent(context));
    }

    private static PendingIntent pendingIntent(Context context) {
        Intent i = new Intent(context, WatchdogReceiver.class).setAction(ACTION);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, REQUEST_CODE, i, flags);
    }
}
