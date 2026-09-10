package ch.piiwii.listy;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;
import java.util.List;

public final class WidgetUpdater {
    private WidgetUpdater() {}

    public static PendingIntent appIntent(Context context, String action, int requestCode) {
        Intent intent = new Intent(context, MainActivity.class)
                .putExtra(MainActivity.EXTRA_ACTION, action)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void refreshAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] quickIds = manager.getAppWidgetIds(new ComponentName(context, QuickAddWidgetProvider.class));
        for (int id : quickIds) updateQuick(context, manager, id);
        int[] upcomingIds = manager.getAppWidgetIds(new ComponentName(context, UpcomingWidgetProvider.class));
        for (int id : upcomingIds) updateUpcoming(context, manager, id);
    }

    public static void updateQuick(Context context, AppWidgetManager manager, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_quick_add);
        views.setOnClickPendingIntent(R.id.widget_quick_root, appIntent(context, MainActivity.ACTION_ADD_MEMO, 1000 + widgetId));
        manager.updateAppWidget(widgetId, views);
    }

    public static void updateUpcoming(Context context, AppWidgetManager manager, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_upcoming);
        List<WidgetStore.Reminder> items = WidgetStore.reminders(context);
        views.setTextViewText(R.id.widget_count, items.isEmpty() ? "" : String.valueOf(items.size()));
        views.setTextViewText(R.id.widget_empty, items.isEmpty() ? "Aucun rappel synchronisé\nOuvre ListY une fois pour actualiser." : "");
        views.setViewVisibility(R.id.widget_empty, items.isEmpty() ? View.VISIBLE : View.GONE);
        setRow(views, 0, items);
        setRow(views, 1, items);
        setRow(views, 2, items);
        PendingIntent open = appIntent(context, MainActivity.ACTION_OPEN_MEMO, 2000 + widgetId);
        views.setOnClickPendingIntent(R.id.widget_upcoming_root, open);
        views.setOnClickPendingIntent(R.id.widget_header, open);
        manager.updateAppWidget(widgetId, views);
    }

    private static void setRow(RemoteViews views, int index, List<WidgetStore.Reminder> items) {
        int rowId = index == 0 ? R.id.widget_row_1 : (index == 1 ? R.id.widget_row_2 : R.id.widget_row_3);
        int timeId = index == 0 ? R.id.widget_time_1 : (index == 1 ? R.id.widget_time_2 : R.id.widget_time_3);
        int titleId = index == 0 ? R.id.widget_title_1 : (index == 1 ? R.id.widget_title_2 : R.id.widget_title_3);
        if (index < items.size()) {
            WidgetStore.Reminder item = items.get(index);
            views.setViewVisibility(rowId, View.VISIBLE);
            views.setTextViewText(timeId, item.when);
            views.setTextViewText(titleId, item.name);
        } else {
            views.setViewVisibility(rowId, View.GONE);
        }
    }
}
