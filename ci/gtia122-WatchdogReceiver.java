package ch.piiwii.gtialtimeter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WatchdogReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!ServiceWatchdog.hasWidgets(context)) return;
        GtiWidgetUpdater.updateAll(context);
        AltitudeService.startIfPermitted(context);
        ServiceWatchdog.schedule(context, ServiceWatchdog.NORMAL_DELAY_MS);
    }
}
