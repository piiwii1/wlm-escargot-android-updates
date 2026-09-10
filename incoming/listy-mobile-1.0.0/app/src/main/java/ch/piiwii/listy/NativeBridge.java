package ch.piiwii.listy;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.provider.CalendarContract;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public final class NativeBridge {
    private final Activity activity;

    public NativeBridge(Activity activity) {
        this.activity = activity;
    }

    @JavascriptInterface public void updateReminders(String json) {
        WidgetStore.saveReminders(activity.getApplicationContext(), json);
        WidgetUpdater.refreshAll(activity.getApplicationContext());
    }

    @JavascriptInterface public void addCalendarEvent(String title, String note, String location, String startMs) {
        final long start;
        try { start = Long.parseLong(startMs); }
        catch (Exception e) { return; }
        activity.runOnUiThread(() -> {
            Intent intent = new Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.Events.TITLE, title == null ? "Pense-bête ListY" : title)
                    .putExtra(CalendarContract.Events.DESCRIPTION, note == null ? "" : note)
                    .putExtra(CalendarContract.Events.EVENT_LOCATION, location == null ? "" : location)
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + 30L * 60L * 1000L);
            try { activity.startActivity(intent); }
            catch (ActivityNotFoundException e) { Toast.makeText(activity, "Aucune application Agenda disponible.", Toast.LENGTH_LONG).show(); }
        });
    }

    @JavascriptInterface public void openCalendar() {
        activity.runOnUiThread(() -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, CalendarContract.CONTENT_URI);
            try { activity.startActivity(intent); }
            catch (ActivityNotFoundException e) { Toast.makeText(activity, "Aucune application Agenda disponible.", Toast.LENGTH_LONG).show(); }
        });
    }
}
