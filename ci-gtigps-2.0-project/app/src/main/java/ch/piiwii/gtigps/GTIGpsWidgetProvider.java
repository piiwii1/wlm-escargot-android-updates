package ch.piiwii.gtigps;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

public class GTIGpsWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_OPEN_MAPS_WINDOW = "ch.piiwii.gtigps.OPEN_GOOGLE_MAPS_WINDOW";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) updateOne(context, manager, id);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent != null && ACTION_OPEN_MAPS_WINDOW.equals(intent.getAction())) {
            GoogleMapsWindow.launchWindow(context);
            updateAll(context);
        }
    }

    private static int immutableFlag() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
    }

    private static void updateOne(Context context, AppWidgetManager manager, int id) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_gti_gps);
        boolean maps = GoogleMapsWindow.isInstalled(context);
        rv.setTextViewText(R.id.widget_status, maps
                ? "Google Maps prêt · la vraie app sera lancée dans la zone calibrée"
                : "Google Maps n’est pas installé sur cet appareil");

        Intent open = new Intent(context, GTIGpsWidgetProvider.class).setAction(ACTION_OPEN_MAPS_WINDOW);
        PendingIntent openPi = PendingIntent.getBroadcast(context, 1000 + id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | immutableFlag());
        rv.setOnClickPendingIntent(R.id.widget_open_maps, openPi);

        Intent settings = new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent settingsPi = PendingIntent.getActivity(context, 2000 + id, settings,
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
