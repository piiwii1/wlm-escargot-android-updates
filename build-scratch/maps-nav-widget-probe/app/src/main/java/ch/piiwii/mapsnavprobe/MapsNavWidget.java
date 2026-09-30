package ch.piiwii.mapsnavprobe;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;

public class MapsNavWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) updateOne(context, manager, id);
    }

    public static void updateAll(Context context) {
        AppWidgetManager m = AppWidgetManager.getInstance(context);
        int[] ids = m.getAppWidgetIds(new ComponentName(context, MapsNavWidget.class));
        for (int id : ids) updateOne(context, m, id);
    }

    private static void updateOne(Context context, AppWidgetManager manager, int id) {
        SharedPreferences p = context.getSharedPreferences(MapsNotificationListener.PREFS, Context.MODE_PRIVATE);
        String arrow = p.getString("arrow", "");
        String distance = p.getString("distance", "");
        String primary = p.getString("primary", "");
        String secondary = p.getString("secondary", "");
        String stamp = p.getString("timestamp", "");
        boolean simulated = p.getBoolean("simulated", false);

        boolean empty = TextUtils.isEmpty(primary);
        if (empty) {
            arrow = "↑";
            distance = "";
            primary = "En attente d’un trajet";
            secondary = "Lance Google Maps et démarre la navigation";
        }
        if (TextUtils.isEmpty(arrow)) arrow = "↑";

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_navigation);
        views.setTextViewText(R.id.widget_arrow, arrow);
        views.setTextViewText(R.id.widget_instruction, primary);

        String meta = simulated ? "● MODE TEST" : "● GOOGLE MAPS";
        if (!TextUtils.isEmpty(stamp)) meta += "   •   " + stamp;
        views.setTextViewText(R.id.widget_meta, meta);

        if (TextUtils.isEmpty(distance)) {
            views.setViewVisibility(R.id.widget_distance, View.GONE);
        } else {
            views.setViewVisibility(R.id.widget_distance, View.VISIBLE);
            views.setTextViewText(R.id.widget_distance, distance);
        }

        if (TextUtils.isEmpty(secondary)) {
            views.setViewVisibility(R.id.widget_detail, View.GONE);
        } else {
            views.setViewVisibility(R.id.widget_detail, View.VISIBLE);
            views.setTextViewText(R.id.widget_detail, secondary);
        }

        Intent open = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 100, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pi);
        manager.updateAppWidget(id, views);
    }
}
