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
    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) updateOne(context, manager, id);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager,
                                          int appWidgetId, android.os.Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, manager, appWidgetId, newOptions);
        updateOne(context, manager, appWidgetId);
    }

    private static int immutableFlag() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
    }

    private static void updateOne(Context context, AppWidgetManager manager, int id) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_gti_gps);
        boolean maps = GoogleMapsWindow.isInstalled(context);
        rv.setTextViewText(R.id.widget_status, maps
                ? "Google Maps prêt · toucher pour l'afficher directement au-dessus de l'accueil"
                : "Google Maps n'est pas installé sur cet appareil");

        // 2.1.0 : PendingIntent d'Activity, pas de BroadcastReceiver.
        // C'est volontaire : certains firmwares ignorent les launchBounds lorsqu'une
        // app externe est lancée directement depuis un receiver de widget.
        Intent open = new Intent(context, WindowLaunchActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        PendingIntent openPi = PendingIntent.getActivity(context, 1000 + id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | immutableFlag());
        rv.setOnClickPendingIntent(R.id.widget_open_maps, openPi);

        Intent settings = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
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
