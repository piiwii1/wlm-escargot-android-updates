package ch.piiwii.mapsnavprobe;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;

public class MapsNavWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) updateOne(context, manager, id);
    }

    @Override public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager, int id, Bundle newOptions) {
        updateOne(context, manager, id);
    }

    public static void updateAll(Context context) {
        AppWidgetManager m = AppWidgetManager.getInstance(context);
        int[] ids = m.getAppWidgetIds(new ComponentName(context, MapsNavWidget.class));
        for (int id : ids) updateOne(context, m, id);
        MapsNavSquareWidget.updateAll(context);
    }

    private static void updateOne(Context context, AppWidgetManager manager, int id) {
        SharedPreferences p = context.getSharedPreferences(MapsNotificationListener.PREFS, Context.MODE_PRIVATE);
        String arrow = p.getString("arrow", "");
        String distance = p.getString("distance", "");
        String primary = p.getString("primary", "");
        String secondary = p.getString("secondary", "");
        String eta = p.getString("eta", "");
        String tripDistance = p.getString("trip_distance", "");
        String tripDuration = p.getString("trip_duration", "");
        boolean simulated = p.getBoolean("simulated", false);

        boolean empty = TextUtils.isEmpty(primary);
        if (empty) {
            arrow = "↑";
            distance = "";
            primary = "En attente d’un trajet";
            secondary = "Démarre une navigation dans Google Maps";
        }
        if (TextUtils.isEmpty(arrow)) arrow = "↑";

        Bundle options = manager.getAppWidgetOptions(id);
        int minHeight = options == null ? 0 : options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);
        boolean compact = minHeight > 0 && minHeight < 120;
        int layout = compact ? R.layout.widget_navigation_compact : R.layout.widget_navigation;
        RemoteViews views = new RemoteViews(context.getPackageName(), layout);
        if (!compact) {
            views.setViewVisibility(R.id.widget_idle_art, empty ? View.VISIBLE : View.GONE);
        }

        float density = context.getResources().getDisplayMetrics().density;
        int iconDp = compact ? 78 : 104;
        int iconPx = Math.max(192, Math.round(iconDp * density));
        views.setImageViewBitmap(R.id.widget_arrow, NavIconSelector.render(arrow, primary, iconPx));
        views.setTextViewText(R.id.widget_instruction, primary);
        views.setTextViewText(R.id.widget_meta, compact ? "MAPS" : "GOOGLE MAPS");
        views.setTextViewText(R.id.widget_status, simulated ? "TEST" : (empty ? "PRÊT" : "LIVE"));

        if (TextUtils.isEmpty(distance)) {
            views.setViewVisibility(R.id.widget_distance, View.GONE);
        } else {
            views.setViewVisibility(R.id.widget_distance, View.VISIBLE);
            views.setTextViewText(R.id.widget_distance, distance);
        }

        if (compact) {
            String compactTrip = buildCompactTrip(eta, tripDistance, tripDuration);
            String compactDetail = !TextUtils.isEmpty(compactTrip) ? compactTrip : secondary;
            if (TextUtils.isEmpty(compactDetail)) {
                views.setViewVisibility(R.id.widget_detail, View.GONE);
            } else {
                views.setViewVisibility(R.id.widget_detail, View.VISIBLE);
                views.setTextViewText(R.id.widget_detail, compactDetail);
            }
        } else {
            if (TextUtils.isEmpty(secondary)) {
                views.setViewVisibility(R.id.widget_detail, View.GONE);
            } else {
                views.setViewVisibility(R.id.widget_detail, View.VISIBLE);
                views.setTextViewText(R.id.widget_detail, secondary);
            }

            if (empty) {
                views.setViewVisibility(R.id.widget_trip_metrics, View.GONE);
            } else {
                views.setViewVisibility(R.id.widget_trip_metrics, View.VISIBLE);
                views.setTextViewText(R.id.widget_eta_value, TextUtils.isEmpty(eta) ? "—" : eta);
                views.setTextViewText(R.id.widget_trip_distance_value, TextUtils.isEmpty(tripDistance) ? "— km" : tripDistance);
                views.setTextViewText(R.id.widget_trip_duration_value, TextUtils.isEmpty(tripDuration) ? "—" : tripDuration);
            }
        }

        PendingIntent maps = PendingIntent.getActivity(context, 1000 + id, mapsIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent settings = PendingIntent.getActivity(context, 2000 + id, new Intent(context, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, maps);
        views.setOnClickPendingIntent(R.id.widget_status, settings);
        views.setOnClickPendingIntent(R.id.widget_meta, settings);

        manager.updateAppWidget(id, views);
    }

    private static String buildCompactTrip(String eta, String distance, String duration) {
        if (TextUtils.isEmpty(eta) && TextUtils.isEmpty(distance) && TextUtils.isEmpty(duration)) return "";
        String a = TextUtils.isEmpty(eta) ? "—" : eta;
        String d = TextUtils.isEmpty(distance) ? "— km" : distance;
        String t = TextUtils.isEmpty(duration) ? "—" : duration;
        return a + "  •  " + d + "  •  " + t;
    }

    private static Intent mapsIntent(Context context) {
        Intent i = context.getPackageManager().getLaunchIntentForPackage(MapsNotificationListener.MAPS_PACKAGE);
        if (i != null) return i;
        return new Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com"));
    }
}
