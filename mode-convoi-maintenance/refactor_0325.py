from pathlib import Path
import re

ROOT = Path('mode-convoi-clean/android/app/src/main/java/ch/piiwii/modeconvoi')
main = ROOT / 'MainActivity.java'
service = ROOT / 'LocationShareService.java'
gradle = Path('mode-convoi-clean/android/app/build.gradle')

# --- New centralized helpers ---
(ROOT / 'NotificationHelper.java').write_text(r'''package ch.piiwii.modeconvoi;

import android.Manifest;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import org.json.JSONObject;

public final class NotificationHelper {
    public static final String ALERTS_CHANNEL = "convoy_alerts_v2";
    private static final int ACCENT = Color.rgb(255,181,20);

    private NotificationHelper() {}

    public static void ensureAlertChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;
        NotificationChannel c = new NotificationChannel(
                ALERTS_CHANNEL,
                "Alertes Mode Convoi",
                NotificationManager.IMPORTANCE_HIGH);
        c.setDescription("Alertes des participants, regroupements et événements importants du convoi");
        c.enableVibration(true);
        c.setVibrationPattern(new long[]{0,220,110,260});
        c.enableLights(true);
        c.setLightColor(ACCENT);
        c.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        c.setShowBadge(true);
        nm.createNotificationChannel(c);
    }

    public static void notifyEvent(Context context, JSONObject event) {
        if (Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        ensureAlertChannel(context);
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;
        long eventId = event.optLong("id");
        Intent open = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                context,
                (int)(eventId % 100000),
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String title = event.optString("participantName", "Mode Convoi");
        String msg = event.optString("label", "Nouvelle information");
        Notification n = new Notification.Builder(context, ALERTS_CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ACCENT)
                .setContentTitle(title)
                .setContentText(msg)
                .setStyle(new Notification.BigTextStyle().bigText(msg))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_HIGH)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .build();
        nm.notify((int)(10000 + eventId % 100000), n);
    }
}
''')

(ROOT / 'ConvoyEventProcessor.java').write_text(r'''package ch.piiwii.modeconvoi;

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
                NotificationHelper.notifyEvent(context, event);
            }
        }
        if (max != last) prefs.putLong("lastEventId", max);
    }
}
''')

# --- MainActivity: delegate event logic to helpers ---
s = main.read_text()
s = s.replace('    private static final String EVENTS_CHANNEL = "convoy_alerts_v2";\n', '')
s = s.replace('        createEventChannel();', '        NotificationHelper.ensureAlertChannel(this);')
s = s.replace('snapshot=s;processEvents(); consecutivePollFailures=0;',
              'snapshot=s;ConvoyEventProcessor.process(this,prefs,s); consecutivePollFailures=0;')
pattern = re.compile(r'\n    private void processEvents\(\)\{.*?\n    private View participantAvatar', re.S)
m = pattern.search(s)
if not m:
    raise SystemExit('MainActivity event block not found')
s = s[:m.start()] + '\n    private View participantAvatar' + s[m.end():]
main.write_text(s)

# --- LocationShareService: remove legacy voice-note engine and delegate alerts ---
s = service.read_text()
for imp in [
    'import android.media.AudioAttributes;\n',
    'import android.media.MediaPlayer;\n',
    'import android.util.Base64;\n',
    'import java.io.File;\n',
    'import java.io.FileOutputStream;\n',
    'import java.util.concurrent.ConcurrentLinkedQueue;\n',
]:
    s = s.replace(imp, '')
s = s.replace('    private static final String EVENTS_CHANNEL = "convoy_alerts_v2";\n', '')
for field in [
    '    private final ScheduledExecutorService voiceIo = Executors.newSingleThreadScheduledExecutor();\n',
    '    private volatile boolean voicePollingStarted = false;\n',
    '    private volatile long voiceRetryAt = 0;\n',
    '    private final ConcurrentLinkedQueue<JSONObject> voiceQueue = new ConcurrentLinkedQueue<>();\n',
    '    private volatile boolean voicePlaying = false;\n',
    '    private final Handler mainHandler = new Handler(Looper.getMainLooper());\n',
]:
    s = s.replace(field, '')
# Remove the entire obsolete voice polling/playback block.
voice_pattern = re.compile(r'\n    private void startVoicePolling\(\) \{.*?\n    private void pollEvents\(\) \{', re.S)
m = voice_pattern.search(s)
if not m:
    raise SystemExit('Legacy voice block not found')
s = s[:m.start()] + '\n    private void pollEvents() {' + s[m.end():]
s = s.replace('            processEvents(snapshot);', '            ConvoyEventProcessor.process(this,prefs,snapshot);')
# Remove duplicate processEvents + notifyEvent methods, keep buildNotification.
event_pattern = re.compile(r'\n    private void processEvents\(JSONObject snapshot\) \{.*?\n    private Notification buildNotification', re.S)
m = event_pattern.search(s)
if not m:
    raise SystemExit('Service event block not found')
s = s[:m.start()] + '\n    private Notification buildNotification' + s[m.end():]
# Keep foreground channel local; delegate the shared alert channel.
old_alert_channel = '''\n            NotificationChannel e = new NotificationChannel(EVENTS_CHANNEL, "Alertes Mode Convoi", NotificationManager.IMPORTANCE_HIGH);\n            e.setDescription("Alertes des participants, regroupements et événements importants du convoi");\n            e.enableVibration(true);\n            e.setVibrationPattern(new long[]{0,220,110,260});\n            e.enableLights(true);\n            e.setLightColor(android.graphics.Color.rgb(255,181,20));\n            e.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);\n            e.setShowBadge(true);\n            nm.createNotificationChannel(e);'''
if old_alert_channel not in s:
    raise SystemExit('Service alert channel block not found')
s = s.replace(old_alert_channel, '\n            NotificationHelper.ensureAlertChannel(this);')
s = s.replace('        voiceIo.shutdownNow();\n', '')
service.write_text(s)

# --- Version ---
g = gradle.read_text()
g = re.sub(r'versionCode\s+\d+', 'versionCode 28', g)
g = re.sub(r"versionName\s+'[^']+'", "versionName '0.3.25'", g)
gradle.write_text(g)

# Safety checks: no functionality additions; only delegation/removal of dead legacy code.
main_text = main.read_text()
service_text = service.read_text()
assert 'ConvoyEventProcessor.process(this,prefs,s);' in main_text
assert 'private void processEvents()' not in main_text
assert 'createEventChannel' not in main_text
assert 'ConvoyEventProcessor.process(this,prefs,snapshot);' in service_text
assert 'startVoicePolling' not in service_text
assert 'pollVoice' not in service_text
assert 'playNextVoice' not in service_text
assert 'voiceIo' not in service_text
assert 'private void processEvents(JSONObject snapshot)' not in service_text
assert "versionName '0.3.25'" in gradle.read_text()
print('0.3.25 structural refactor applied successfully')
