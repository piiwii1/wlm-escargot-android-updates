package ch.piiwii.mapsnavprobe;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;

/**
 * Watchdog for stale navigation/test data.
 *
 * The widget stores the last Maps command so it can survive launcher redraws.
 * This receiver makes that state temporary: each genuinely new command extends
 * the deadline, while an unchanged state eventually falls back to the idle art.
 */
public class NavStateExpiry extends BroadcastReceiver {
    private static final String ACTION_EXPIRE = "ch.piiwii.mapsnavprobe.EXPIRE_NAV_STATE";
    private static final String KEY_LAST_SOURCE_STAMP = "_expiry_source_stamp";
    private static final String KEY_LAST_MODE = "_expiry_mode";
    private static final String KEY_DEADLINE = "_expiry_deadline";

    // Tests are deliberately short so the idle artwork can be checked quickly.
    private static final long TEST_TIMEOUT_MS = 20_000L;
    // Real navigation gets a longer grace period between Maps notification updates.
    private static final long LIVE_TIMEOUT_MS = 120_000L;

    public static void ensureScheduled(Context context, SharedPreferences prefs, boolean simulated) {
        String primary = prefs.getString("primary", "");
        if (TextUtils.isEmpty(primary)) {
            clearExpiryMetadata(prefs);
            cancel(context);
            return;
        }

        String sourceStamp = prefs.getString("timestamp", "");
        String mode = simulated ? "test" : "live";
        String previousStamp = prefs.getString(KEY_LAST_SOURCE_STAMP, "");
        String previousMode = prefs.getString(KEY_LAST_MODE, "");
        long deadline = prefs.getLong(KEY_DEADLINE, 0L);
        long now = System.currentTimeMillis();

        // timestamp is rewritten on every real Maps command and on every test.
        // Only a genuinely new command extends the lifetime.
        if (deadline <= 0L || !mode.equals(previousMode) || !sourceStamp.equals(previousStamp)) {
            deadline = now + (simulated ? TEST_TIMEOUT_MS : LIVE_TIMEOUT_MS);
            prefs.edit()
                    .putString(KEY_LAST_SOURCE_STAMP, sourceStamp)
                    .putString(KEY_LAST_MODE, mode)
                    .putLong(KEY_DEADLINE, deadline)
                    .apply();
        }

        schedule(context, Math.max(deadline, now + 500L));
    }

    @Override public void onReceive(Context context, Intent intent) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(MapsNotificationListener.PREFS, Context.MODE_PRIVATE);
        String primary = prefs.getString("primary", "");
        if (TextUtils.isEmpty(primary)) {
            clearExpiryMetadata(prefs);
            cancel(context);
            return;
        }

        long deadline = prefs.getLong(KEY_DEADLINE, 0L);
        long now = System.currentTimeMillis();
        if (deadline <= 0L) {
            ensureScheduled(context, prefs, prefs.getBoolean("simulated", false));
            return;
        }
        if (now < deadline) {
            schedule(context, deadline);
            return;
        }

        // Keep diagnostics, but remove every value that can leave stale navigation
        // visible in the app or widgets.
        prefs.edit()
                .remove("arrow")
                .remove("distance")
                .remove("primary")
                .remove("secondary")
                .remove("eta")
                .remove("trip_distance")
                .remove("trip_duration")
                .remove(KEY_LAST_SOURCE_STAMP)
                .remove(KEY_LAST_MODE)
                .remove(KEY_DEADLINE)
                .putBoolean("simulated", false)
                .apply();

        MapsNavWidget.updateAll(context);
        context.sendBroadcast(new Intent(MapsNotificationListener.ACTION_UPDATE).setPackage(context.getPackageName()));
    }

    private static void schedule(Context context, long whenMillis) {
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm == null) return;
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pendingIntent(context));
    }

    private static void cancel(Context context) {
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm != null) alarm.cancel(pendingIntent(context));
    }

    private static PendingIntent pendingIntent(Context context) {
        Intent i = new Intent(context, NavStateExpiry.class).setAction(ACTION_EXPIRE);
        return PendingIntent.getBroadcast(context, 8175, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void clearExpiryMetadata(SharedPreferences prefs) {
        if (!prefs.contains(KEY_LAST_SOURCE_STAMP) && !prefs.contains(KEY_LAST_MODE) && !prefs.contains(KEY_DEADLINE)) return;
        prefs.edit()
                .remove(KEY_LAST_SOURCE_STAMP)
                .remove(KEY_LAST_MODE)
                .remove(KEY_DEADLINE)
                .apply();
    }
}
