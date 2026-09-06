package ch.piiwii.gtialtimeter;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        int[] ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(new ComponentName(context, GTIAltimeterWidgetProvider.class));
        if (ids != null && ids.length > 0) {
            GtiWidgetUpdater.updateAll(context);
            AltitudeService.startIfPermitted(context);
            ServiceWatchdog.schedule(context, ServiceWatchdog.NORMAL_DELAY_MS);
        }
    }
}
