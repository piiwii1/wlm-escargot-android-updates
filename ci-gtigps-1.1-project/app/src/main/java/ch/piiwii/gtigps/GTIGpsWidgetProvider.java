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

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        updateAll(context);
    }

    private static int immutableFlag() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
    }

    private static void updateOne(Context context, AppWidgetManager manager, int id) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_gti_gps);
        boolean panel = GpsState.prefs(context).getBoolean(GpsState.KEY_PANEL_ENABLED, false);
        boolean tracker = GpsState.prefs(context).getBoolean(GpsState.KEY_TRACKING_ENABLED, false);
        rv.setTextViewText(R.id.widget_status,
                panel ? "Carte MapLibre active · navigation prête"
                        : tracker ? "GPS actif · panneau carte masqué" : "Carte prête · GPS en attente");

        long t = GpsState.prefs(context).getLong(GpsState.KEY_LAST_TIME, 0L);
        if (t > 0L) {
            float acc = GpsState.prefs(context).getFloat(GpsState.KEY_LAST_ACC, -1f);
            long age = Math.max(0L, (System.currentTimeMillis() - t) / 1000L);
            String text = acc >= 0
                    ? String.format(Locale.getDefault(), "GPS · %.0f m · %d s", acc, age)
                    : "GPS · position reçue · " + age + " s";
            rv.setTextViewText(R.id.widget_position, text);
        } else {
            rv.setTextViewText(R.id.widget_position, "GPS : aucune position enregistrée");
        }

        if (GpsState.navigationActive(context)) {
            String name = shortName(GpsState.prefs(context).getString(GpsState.KEY_DEST_NAME, "Destination"));
            float distance = GpsState.prefs(context).getFloat(GpsState.KEY_ROUTE_DISTANCE_M, 0f);
            float duration = GpsState.prefs(context).getFloat(GpsState.KEY_ROUTE_DURATION_S, 0f);
            String next = GpsState.prefs(context).getString(GpsState.KEY_NEXT_INSTRUCTION, "Suivez l’itinéraire");
            rv.setTextViewText(R.id.widget_note,
                    "► " + name + "\n" + MainActivity.formatDistance(distance) + " · " + MainActivity.formatDuration(duration) + "\n" + next);
        } else {
            rv.setTextViewText(R.id.widget_note,
                    "Carte MapLibre + GPS indépendant. Ouvre RÉGLAGES pour choisir une destination et calculer un itinéraire.");
        }

        Intent show = new Intent(context, MainActivity.class).setAction(MainActivity.ACTION_REQUEST_SHOW_MAP);
        PendingIntent showPi = PendingIntent.getActivity(context, 100 + id, show,
                PendingIntent.FLAG_UPDATE_CURRENT | immutableFlag());
        rv.setOnClickPendingIntent(R.id.widget_open_map, showPi);

        Intent settings = new Intent(context, MainActivity.class);
        PendingIntent settingsPi = PendingIntent.getActivity(context, 200 + id, settings,
                PendingIntent.FLAG_UPDATE_CURRENT | immutableFlag());
        rv.setOnClickPendingIntent(R.id.widget_settings, settingsPi);
        manager.updateAppWidget(id, rv);
    }

    private static String shortName(String s) {
        if (s == null || s.trim().isEmpty()) return "Destination";
        String[] p = s.split(",");
        if (p.length >= 2) return p[0].trim() + ", " + p[1].trim();
        return s.trim();
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName provider = new ComponentName(context, GTIGpsWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(provider);
        for (int id : ids) updateOne(context, manager, id);
    }
}
