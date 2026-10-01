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
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

public class MapsNavSquareWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) updateOne(context, manager, id);
    }

    @Override public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager, int id, Bundle newOptions) {
        updateOne(context, manager, id);
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, MapsNavSquareWidget.class));
        for (int id : ids) updateOne(context, manager, id);
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

        NavStateExpiry.ensureScheduled(context, p, simulated);

        boolean empty = TextUtils.isEmpty(primary);
        if (empty) {
            arrow = "↑";
            distance = "";
            primary = "En attente d’un trajet";
            secondary = "Démarre une navigation dans Google Maps";
        }
        if (TextUtils.isEmpty(arrow)) arrow = "↑";

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_navigation_square);
        Bundle options = manager.getAppWidgetOptions(id);
        int minW = options == null ? 0 : options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
        int minH = options == null ? 0 : options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);
        boolean small = (minW > 0 && minW < 230) || (minH > 0 && minH < 230);

        float density = context.getResources().getDisplayMetrics().density;
        int iconDp = small ? 112 : 150;
        int iconPx = Math.max(256, Math.round(iconDp * density));
        views.setImageViewBitmap(R.id.square_arrow, NavIconSelector.render(arrow, primary, iconPx));
        views.setTextViewText(R.id.square_instruction, primary);
        views.setTextViewText(R.id.square_meta, "GOOGLE MAPS");
        views.setTextViewText(R.id.square_status, simulated ? "TEST" : (empty ? "PRÊT" : "LIVE"));
        views.setTextViewTextSize(R.id.square_distance, TypedValue.COMPLEX_UNIT_SP, small ? 29f : 36f);
        views.setTextViewTextSize(R.id.square_instruction, TypedValue.COMPLEX_UNIT_SP, small ? 17f : 21f);
        views.setTextViewTextSize(R.id.square_detail, TypedValue.COMPLEX_UNIT_SP, small ? 10f : 12f);
        views.setTextViewTextSize(R.id.square_eta_value, TypedValue.COMPLEX_UNIT_SP, small ? 13f : 16f);
        views.setTextViewTextSize(R.id.square_trip_distance_value, TypedValue.COMPLEX_UNIT_SP, small ? 13f : 16f);
        views.setTextViewTextSize(R.id.square_trip_duration_value, TypedValue.COMPLEX_UNIT_SP, small ? 13f : 16f);

        if (TextUtils.isEmpty(distance)) {
            views.setViewVisibility(R.id.square_distance, View.GONE);
        } else {
            views.setViewVisibility(R.id.square_distance, View.VISIBLE);
            views.setTextViewText(R.id.square_distance, distance);
        }

        if (TextUtils.isEmpty(secondary) || (small && primary.length() > 28)) {
            views.setViewVisibility(R.id.square_detail, View.GONE);
        } else {
            views.setViewVisibility(R.id.square_detail, View.VISIBLE);
            views.setTextViewText(R.id.square_detail, secondary);
        }

        if (empty) {
            views.setViewVisibility(R.id.square_trip_metrics, View.GONE);
        } else {
            views.setViewVisibility(R.id.square_trip_metrics, View.VISIBLE);
            views.setTextViewText(R.id.square_eta_value, TextUtils.isEmpty(eta) ? "—" : eta);
            views.setTextViewText(R.id.square_trip_distance_value, TextUtils.isEmpty(tripDistance) ? "— km" : tripDistance);
            views.setTextViewText(R.id.square_trip_duration_value, TextUtils.isEmpty(tripDuration) ? "—" : tripDuration);
        }

        PendingIntent maps = PendingIntent.getActivity(context, 3000 + id, mapsIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent settings = PendingIntent.getActivity(context, 4000 + id, new Intent(context, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.square_root, maps);
        views.setOnClickPendingIntent(R.id.square_status, settings);
        views.setOnClickPendingIntent(R.id.square_meta, settings);
        manager.updateAppWidget(id, views);
    }

    private static Intent mapsIntent(Context context) {
        Intent i = context.getPackageManager().getLaunchIntentForPackage(MapsNotificationListener.MAPS_PACKAGE);
        if (i != null) return i;
        return new Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com"));
    }
}
