package ch.piiwii.listy;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public final class WidgetStore {
    private static final String PREFS = "listy_widget_data";
    private static final String KEY_REMINDERS = "reminders";

    public static final class Reminder {
        public final String name;
        public final String when;
        public final long startMs;
        Reminder(String name, String when, long startMs) {
            this.name = name;
            this.when = when;
            this.startMs = startMs;
        }
    }

    private WidgetStore() {}

    public static void saveReminders(Context context, String json) {
        if (json == null) json = "[]";
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_REMINDERS, json).apply();
    }

    public static List<Reminder> reminders(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_REMINDERS, "[]");
        List<Reminder> out = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json == null ? "[]" : json);
            long now = System.currentTimeMillis() - 10L * 60L * 1000L;
            for (int i = 0; i < array.length(); i++) {
                JSONObject row = array.optJSONObject(i);
                if (row == null) continue;
                long start = row.optLong("start_ms", 0L);
                if (start > 0L && start < now) continue;
                out.add(new Reminder(row.optString("name", "Pense-bête"), row.optString("when", ""), start));
                if (out.size() >= 6) break;
            }
        } catch (Exception ignored) {}
        return out;
    }
}
