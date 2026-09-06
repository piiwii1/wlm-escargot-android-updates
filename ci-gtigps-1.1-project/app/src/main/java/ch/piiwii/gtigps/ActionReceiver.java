package ch.piiwii.gtigps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ActionReceiver extends BroadcastReceiver {
    public static final String ACTION_SHOW = "ch.piiwii.gtigps.SHOW_MAP";
    public static final String ACTION_HIDE = "ch.piiwii.gtigps.HIDE_MAP";
    public static final String ACTION_REFRESH = "ch.piiwii.gtigps.REFRESH_MAP";
    public static final String ACTION_START_GPS = "ch.piiwii.gtigps.START_GPS";
    public static final String ACTION_STOP_GPS = "ch.piiwii.gtigps.STOP_GPS";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();
        if (ACTION_START_GPS.equals(action) || ACTION_STOP_GPS.equals(action)) {
            Intent tracker = new Intent(context, GpsTrackingService.class).setAction(action);
            if (ACTION_STOP_GPS.equals(action)) context.stopService(tracker);
            else startForegroundCompat(context, tracker);
            return;
        }

        if (ACTION_SHOW.equals(action)) {
            Intent tracker = new Intent(context, GpsTrackingService.class).setAction(ACTION_START_GPS);
            startForegroundCompat(context, tracker);
        }

        Intent service = new Intent(context, MapPanelService.class).setAction(action);
        if (ACTION_HIDE.equals(action)) context.stopService(service);
        else startForegroundCompat(context, service);
    }

    private static void startForegroundCompat(Context context, Intent service) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(service);
        else context.startService(service);
    }
}
