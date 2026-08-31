package ch.piiwii.modeconvoi;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class ConvoyEventProcessor {
    private static final Set<String> NOTIFIED_TYPES = new HashSet<>(Arrays.asList(
            "status", "rally", "leave", "join", "remove", "rename", "close", "role",
            "emergency-stop", "status-clear-auto"));

    private ConvoyEventProcessor() {}

    public static synchronized void process(Context context, AppPrefs prefs, JSONObject snapshot) {
        JSONArray events = snapshot == null ? null : snapshot.optJSONArray("events");
        if (events == null) return;
        long last = prefs.getLong("lastEventId", 0);
        long max = last;
        String me = prefs.get("participantId", "");
        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.optJSONObject(i);
            if (event == null) continue;
            long eventId = event.optLong("id");
            max = Math.max(max, eventId);
            if (eventId <= last || me.equals(event.optString("participantId"))) continue;
            if (NOTIFIED_TYPES.contains(event.optString("type"))) {
                boolean handledForeground = ConvoyForegroundAlertBus.dispatch(event);
                if (!handledForeground) NotificationHelper.notifyEvent(context, event);
            }
        }
        if (max != last) prefs.putLong("lastEventId", max);
    }
}
