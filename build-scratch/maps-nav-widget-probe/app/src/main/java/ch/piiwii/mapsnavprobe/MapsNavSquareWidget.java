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

        boolean empty = TextUtils.isEmpty(primary);
        if (empty) {
            arrow = "↑";
            distance = "";
            primary = "En attente d’un trajet";
            secondary = "Démarre une navigation dans Google Maps";
        }
        if (TextUtils.isEmpty(arrow)) arrow = "↑";

        Bundle options = manager.getAppWidgetOptions(id);
        int minW = options == null ? 0 : options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
        int minH = options == null ? 0 : options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);

        // A 4x4 cell on TS18/LEHX can be much shorter than the nominal Android
        // size. Use dedicated layouts so the bottom metrics can never be pushed
        // outside the host bounds.
        boolean tiny = minH > 0 && minH < 220;
        boolean compact = minH <= 0 || (!tiny && minH < 380);
        int layout = tiny ? R.layout.widget_navigation_square_tiny
                : (compact ? R.layout.widget_navigation_square_compact : R.layout.widget_navigation_square);
        RemoteViews views = new RemoteViews(context.getPackageName(), layout);

        float density = context.getResources().getDisplayMetrics().density;
        int iconDp = tiny ? 72 : (compact ? 96 : 150);
        int iconPx = Math.max(192, Math.round(iconDp * density));
        views.setImageViewBitmap(R.id.square_arrow, NavIconSelector.render(arrow, primary, iconPx));
        views.setTextViewText(R.id.square_instruction, primary);
        views.setTextViewText(R.id.square_meta, compact || tiny ? "MAPS" : "GOOGLE MAPS");
        views.setTextViewText(R.id.square_status, simulated ? "TEST" : (empty ? "PRÊT" : "LIVE"));

        float distanceSp = tiny ? 23f : (compact ? 28f : 36f);
        float instructionSp = tiny ? 14f : (compact ? 16f : 21f);
        float detailSp = tiny ? 8f : (compact ? 9f : 12f);
        float metricSp = tiny ? 11f : (compact ? 13f : 16f);
        views.setTextViewTextSize(R.id.square_distance, TypedValue.COMPLEX_UNIT_SP, distanceSp);
        views.setTextViewTextSize(R.id.square_instruction, TypedValue.COMPLEX_UNIT_SP, instructionSp);
        views.setTextViewTextSize(R.id.square_detail, TypedValue.COMPLEX_UNIT_SP, detailSp);
        views.setTextViewTextSize(R.id.square_eta_value, TypedValue.COMPLEX_UNIT_SP, metricSp);
        views.setTextViewTextSize(R.id.square_trip_distance_value, TypedValue.COMPLEX_UNIT_SP, metricSp);
        views.setTextViewTextSize(R.id.square_trip_duration_value, TypedValue.COMPLEX_UNIT_SP, metricSp);

        if (TextUtils.isEmpty(distance)) {
            views.setViewVisibility(R.id.square_distance, View.GONE);
        } else {
            views.setViewVisibility(R.id.square_distance, View.VISIBLE);
            views.setTextViewText(R.id.square_distance, distance);
        }

        boolean hideDetail = tiny || TextUtils.isEmpty(secondary)
                || (compact && ((minH > 0 && minH < 280) || primary.length() > 28));
        if (hideDetail) {
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
