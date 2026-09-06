package ch.piiwii.gtialtimeter;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class GTIAltimeterWidgetProvider extends AppWidgetProvider {
    @Override public void onEnabled(Context context) {
        super.onEnabled(context);
        GtiWidgetUpdater.updateAll(context);
        AltitudeService.startIfPermitted(context);
        ServiceWatchdog.schedule(context, ServiceWatchdog.NORMAL_DELAY_MS);
    }

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) GtiWidgetUpdater.update(context, manager, id);
        AltitudeService.startIfPermitted(context);
        ServiceWatchdog.schedule(context, ServiceWatchdog.NORMAL_DELAY_MS);
    }

    @Override public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, manager, appWidgetId, newOptions);
        GtiWidgetUpdater.update(context, manager, appWidgetId);
        AltitudeService.startIfPermitted(context);
        ServiceWatchdog.schedule(context, ServiceWatchdog.NORMAL_DELAY_MS);
    }

    @Override public void onDisabled(Context context) {
        super.onDisabled(context);
        ServiceWatchdog.cancel(context);
        context.stopService(new Intent(context, AltitudeService.class));
    }
}
