package ch.piiwii.mapsnavprobe;

import android.app.Notification;
import android.content.Context;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts Google Maps' destination summary from the app's own notification
 * layout. Google Maps often renders the footer (duration · distance · ETA)
 * in resource ids such as nav_time/header_text instead of exposing each value
 * as a normal Notification extra.
 */
public final class GoogleMapsTripInfoExtractor {
    private GoogleMapsTripInfoExtractor() {}

    public static final class Result {
        public final String distance;
        public final String duration;
        public final String eta;
        public final String debug;

        Result(String distance, String duration, String eta, String debug) {
            this.distance = distance;
            this.duration = duration;
            this.eta = eta;
            this.debug = debug;
        }
    }

    private static final Pattern DISTANCE = Pattern.compile(
            "(?iu)(\\d+(?:[.,]\\d+)?)\\s*(km|m|mi|ft|yd|kilom(?:è|e)tre(?:s)?|mile(?:s)?)\\b");
    private static final Pattern DURATION = Pattern.compile(
            "(?iu)\\b(?:(\\d{1,2})\\s*(?:h|hr|hrs|heure|heures|hour|hours)\\s*)?(\\d{1,3})\\s*(?:min|mins|minute|minutes)\\b");
    private static final Pattern HOUR_ONLY = Pattern.compile(
            "(?iu)\\b(\\d{1,2})\\s*(?:h|hr|hrs|heure|heures|hour|hours)\\b");
    private static final Pattern ETA = Pattern.compile(
            "(?iu)(?<!\\d)([01]?\\d|2[0-3])[:h.]([0-5]\\d)(?:\\s*(AM|PM))?(?!\\d)");

    public static Result extract(Context hostContext, StatusBarNotification sbn) {
        if (hostContext == null || sbn == null || sbn.getNotification() == null) {
            return new Result("", "", "", "");
        }

        Context mapsContext;
        try {
            mapsContext = hostContext.createPackageContext(sbn.getPackageName(), Context.CONTEXT_IGNORE_SECURITY);
        } catch (Throwable t) {
            return new Result("", "", "", "packageContext failed: " + t.getClass().getSimpleName());
        }

        Notification n = sbn.getNotification();
        List<RemoteViews> views = new ArrayList<>();
        addView(views, n.contentView);
        addView(views, n.bigContentView);
        addView(views, n.headsUpContentView);

        if (Build.VERSION.SDK_INT >= 24) {
            try {
                Notification.Builder builder = Notification.Builder.recoverBuilder(hostContext, n);
                addView(views, builder.createContentView());
                addView(views, builder.createBigContentView());
                addView(views, builder.createHeadsUpContentView());
            } catch (Throwable ignored) {}
        }

        Collector collector = new Collector(mapsContext);
        for (RemoteViews rv : views) collector.inspect(rv);
        return collector.finish();
    }

    private static void addView(List<RemoteViews> out, RemoteViews rv) {
        if (rv == null) return;
        for (RemoteViews existing : out) {
            if (existing == rv) return;
            try {
                if (existing.getLayoutId() == rv.getLayoutId()) return;
            } catch (Throwable ignored) {}
        }
        out.add(rv);
    }

    private static final class Collector {
        final Context mapsContext;
        String distance = "";
        String duration = "";
        String eta = "";
        final StringBuilder debug = new StringBuilder();

        Collector(Context mapsContext) { this.mapsContext = mapsContext; }

        void inspect(RemoteViews rv) {
            if (rv == null) return;
            try {
                LayoutInflater inflater = (LayoutInflater) mapsContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                if (inflater == null) return;
                View root = inflater.inflate(rv.getLayoutId(), null, false);
                rv.reapply(mapsContext, root);
                walk(root, 0);
            } catch (Throwable t) {
                appendDebug("inflate", t.getClass().getSimpleName());
            }
        }

        void walk(View view, int depth) {
            if (view == null || depth > 16) return;

            if (view instanceof TextView) {
                CharSequence cs = ((TextView) view).getText();
                String text = clean(cs == null ? "" : cs.toString());
                if (!TextUtils.isEmpty(text)) {
                    String name = resourceName(view.getId());
                    appendDebug(TextUtils.isEmpty(name) ? "text" : name, text);

                    if (isSummaryResource(name)) {
                        parseSummary(text);
                    } else if (isEtaResource(name) && TextUtils.isEmpty(eta)) {
                        String foundEta = extractEta(text);
                        if (!TextUtils.isEmpty(foundEta)) eta = foundEta;
                    }
                }
            }

            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) {
                    walk(group.getChildAt(i), depth + 1);
                }
            }
        }

        String resourceName(int id) {
            if (id <= 0) return "";
            try { return mapsContext.getResources().getResourceEntryName(id); }
            catch (Throwable ignored) { return ""; }
        }

        boolean isSummaryResource(String name) {
            if (TextUtils.isEmpty(name)) return false;
            String n = name.toLowerCase(Locale.ROOT);
            return "nav_time".equals(n)
                    || "header_text".equals(n)
                    || n.contains("nav_time")
                    || n.contains("eta_card")
                    || n.contains("trip_summary")
                    || n.contains("remaining_time");
        }

        boolean isEtaResource(String name) {
            if (TextUtils.isEmpty(name)) return false;
            String n = name.toLowerCase(Locale.ROOT);
            return n.contains("eta") || "text".equals(n) || n.contains("oneliner");
        }

        void parseSummary(String text) {
            if (TextUtils.isEmpty(text)) return;

            if (TextUtils.isEmpty(distance)) {
                Matcher dm = DISTANCE.matcher(text);
                String found = "";
                while (dm.find()) found = normalizeDistance(dm.group(1), dm.group(2));
                if (!TextUtils.isEmpty(found)) distance = found;
            }

            if (TextUtils.isEmpty(duration)) {
                Matcher dur = DURATION.matcher(text);
                if (dur.find()) {
                    int h = parseInt(dur.group(1));
                    int m = parseInt(dur.group(2));
                    int total = h * 60 + m;
                    if (total > 0) duration = formatDuration(total);
                } else {
                    Matcher hour = HOUR_ONLY.matcher(text);
                    if (hour.find()) {
                        int h = parseInt(hour.group(1));
                        if (h > 0) duration = h + " h";
                    }
                }
            }

            if (TextUtils.isEmpty(eta)) eta = extractEta(text);
        }

        Result finish() {
            return new Result(distance, duration, eta, debug.toString());
        }

        void appendDebug(String key, String value) {
            if (debug.length() > 0) debug.append(" | ");
            debug.append(key).append('=').append(value);
        }
    }

    private static String extractEta(String text) {
        if (TextUtils.isEmpty(text)) return "";
        Matcher m = ETA.matcher(text);
        String result = "";
        while (m.find()) {
            int h = parseInt(m.group(1));
            int min = parseInt(m.group(2));
            String ap = m.group(3);
            if (!TextUtils.isEmpty(ap)) {
                if ("PM".equalsIgnoreCase(ap) && h < 12) h += 12;
                if ("AM".equalsIgnoreCase(ap) && h == 12) h = 0;
            }
            if (h >= 0 && h <= 23 && min >= 0 && min <= 59) {
                result = String.format(Locale.FRANCE, "%02d:%02d", h, min);
            }
        }
        return result;
    }

    private static String normalizeDistance(String number, String unit) {
        if (TextUtils.isEmpty(number) || TextUtils.isEmpty(unit)) return "";
        String u = unit.toLowerCase(Locale.ROOT);
        try {
            double value = Double.parseDouble(number.replace(',', '.'));
            if ("km".equals(u) || u.startsWith("kilom")) {
                if (Math.abs(value - Math.rint(value)) < 0.05) return String.format(Locale.FRANCE, "%.0f km", value);
                return String.format(Locale.FRANCE, "%.1f km", value);
            }
            if ("m".equals(u)) return String.format(Locale.FRANCE, "%.0f m", value);
            if ("mi".equals(u) || u.startsWith("mile")) return number.replace('.', ',') + " mi";
            return number + " " + unit;
        } catch (Throwable ignored) {
            return number + " " + unit;
        }
    }

    private static String formatDuration(int minutes) {
        if (minutes < 60) return minutes + " min";
        int h = minutes / 60;
        int m = minutes % 60;
        return m == 0 ? h + " h" : String.format(Locale.FRANCE, "%d h %02d min", h, m);
    }

    private static int parseInt(String s) {
        if (TextUtils.isEmpty(s)) return 0;
        try { return Integer.parseInt(s); } catch (Throwable ignored) { return 0; }
    }

    private static String clean(String s) {
        return s == null ? "" : s.replace('\u00A0', ' ').replace('\u202F', ' ')
                .replaceAll("\\s+", " ").trim();
    }
}
