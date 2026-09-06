package ch.piiwii.gtigps;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import java.util.Locale;

public class GTIGpsWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) updateOne(context, manager, id);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager, int appWidgetId, android.os.Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, manager, appWidgetId, newOptions);
        updateOne(context, manager, appWidgetId);
    }

    private static int immutableFlag() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
    }

    private static void updateOne(Context context, AppWidgetManager manager, int id) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_gti_gps);
        boolean active = GpsState.prefs(context).getBoolean(GpsState.KEY_PANEL_ENABLED, false);
        rv.setTextViewText(R.id.widget_status, active ? "Carte MapLibre active · panneau 380 × 350" : "Carte prête · panneau externe masqué");

        long t = GpsState.prefs(context).getLong(GpsState.KEY_LAST_TIME, 0L);
        if (t > 0L) {
            double lat = Double.longBitsToDouble(GpsState.prefs(context).getLong(GpsState.KEY_LAST_LAT, 0L));
            double lon = Double.longBitsToDouble(GpsState.prefs(context).getLong(GpsState.KEY_LAST_LON, 0L));
            float acc = GpsState.prefs(context).getFloat(GpsState.KEY_LAST_ACC, -1f);
            long age = Math.max(0L, (System.currentTimeMillis() - t) / 1000L);
            String text = String.format(Locale.US, "GPS %.5f, %.5f · %.0f m · %d s", lat, lon, acc, age);
            rv.setTextViewText(R.id.widget_position, text);
        } else {
            rv.setTextViewText(R.id.widget_position, "GPS : aucune position enregistrée");
        }

        Intent show = new Intent(context, ActionReceiver.class).setAction(ActionReceiver.ACTION_SHOW);
        PendingIntent showPi = PendingIntent.getBroadcast(context, 100 + id, show,
                PendingIntent.FLAG_UPDATE_CURRENT | immutableFlag());
        rv.setOnClickPendingIntent(R.id.widget_open_map, showPi);

        Intent settings = new Intent(context, MainActivity.class);
        PendingIntent settingsPi = PendingIntent.getActivity(context, 200 + id, settings,
                PendingIntent.FLAG_UPDATE_CURRENT | immutableFlag());
        rv.setOnClickPendingIntent(R.id.widget_settings, settingsPi);
        manager.updateAppWidget(id, rv);
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName provider = new ComponentName(context, GTIGpsWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(provider);
        for (int id : ids) updateOne(context, manager, id);
    }
}
