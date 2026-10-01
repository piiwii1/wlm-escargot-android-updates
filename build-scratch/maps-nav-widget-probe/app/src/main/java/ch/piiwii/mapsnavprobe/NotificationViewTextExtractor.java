package ch.piiwii.mapsnavprobe;

import android.app.Notification;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;
import java.util.ArrayList;

/**
 * Extracts text that Google Maps may render inside custom notification
 * RemoteViews without exposing it through the usual Notification extras.
 * This is used as a best-effort fallback for ETA / total distance / duration.
 */
public final class NotificationViewTextExtractor {
    private NotificationViewTextExtractor() {}

    public static String[] extract(Context context, Notification notification) {
        ArrayList<String> out = new ArrayList<>();
        if (context == null || notification == null) return new String[0];

        collectRemoteViews(context, notification.contentView, out);
        collectRemoteViews(context, notification.bigContentView, out);
        collectRemoteViews(context, notification.headsUpContentView, out);

        // Android can synthesize the final decorated notification even when the
        // direct RemoteViews fields are null. Recover the builder and inspect
        // those generated layouts too.
        if (Build.VERSION.SDK_INT >= 24) {
            try {
                Notification.Builder b = Notification.Builder.recoverBuilder(context, notification);
                collectRemoteViews(context, b.createContentView(), out);
                collectRemoteViews(context, b.createBigContentView(), out);
                collectRemoteViews(context, b.createHeadsUpContentView(), out);
            } catch (Throwable ignored) {}
        }

        return out.toArray(new String[0]);
    }

    private static void collectRemoteViews(Context context, RemoteViews remoteViews, ArrayList<String> out) {
        if (remoteViews == null) return;
        try {
            View root = remoteViews.apply(context, null);
            collectView(root, out, 0);
        } catch (Throwable ignored) {
            // Some OEM/Google layouts cannot be applied outside SystemUI.
            // The normal extras parser remains active when this happens.
        }
    }

    private static void collectView(View view, ArrayList<String> out, int depth) {
        if (view == null || depth > 12) return;

        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            if (!TextUtils.isEmpty(text)) addUnique(out, text.toString());
        }

        CharSequence description = view.getContentDescription();
        if (!TextUtils.isEmpty(description)) addUnique(out, description.toString());

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collectView(group.getChildAt(i), out, depth + 1);
            }
        }
    }

    private static void addUnique(ArrayList<String> out, String value) {
        if (value == null) return;
        String clean = value.replace('\u00A0', ' ').replace('\u202F', ' ')
                .replaceAll("\\s+", " ").trim();
        if (clean.isEmpty()) return;
        for (String existing : out) if (existing.equals(clean)) return;
        out.add(clean);
    }
}
