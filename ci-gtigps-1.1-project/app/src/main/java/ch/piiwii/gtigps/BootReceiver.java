package ch.piiwii.gtigps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        if (GpsState.prefs(context).getBoolean(GpsState.KEY_TRACKING_ENABLED, false)) {
            start(context, new Intent(context, GpsTrackingService.class).setAction(ActionReceiver.ACTION_START_GPS));
        }

        boolean resumePanel = GpsState.prefs(context).getBoolean(GpsState.KEY_AUTO_RESUME, false)
                && GpsState.prefs(context).getBoolean(GpsState.KEY_PANEL_ENABLED, false);
        if (resumePanel) {
            start(context, new Intent(context, MapPanelService.class).setAction(ActionReceiver.ACTION_SHOW));
        }
    }

    private void start(Context context, Intent service) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(service);
        else context.startService(service);
    }
}
