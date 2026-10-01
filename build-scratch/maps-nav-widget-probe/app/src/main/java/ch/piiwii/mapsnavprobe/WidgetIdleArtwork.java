package ch.piiwii.mapsnavprobe;

import android.graphics.Bitmap;

/** Small cached copy of the idle artwork, safe to send through RemoteViews. */
public final class WidgetIdleArtwork {
    private WidgetIdleArtwork() {}

    private static Bitmap cached;

    public static synchronized Bitmap get() {
        if (cached != null && !cached.isRecycled()) return cached;

        Bitmap source = IdleArtwork.get();
        if (source == null || source.isRecycled()) return null;

        final int maxWidth = 640;
        if (source.getWidth() <= maxWidth) {
            cached = source;
            return cached;
        }

        int width = maxWidth;
        int height = Math.max(1, Math.round(source.getHeight() * (width / (float) source.getWidth())));
        cached = Bitmap.createScaledBitmap(source, width, height, true);
        return cached;
    }
}
