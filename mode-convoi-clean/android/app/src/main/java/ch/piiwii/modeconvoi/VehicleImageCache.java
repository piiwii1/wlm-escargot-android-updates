package ch.piiwii.modeconvoi;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.LruCache;

public final class VehicleImageCache {
    private static final int MAX_ENTRIES = 24;
    private static final LruCache<String, Bitmap> CACHE = new LruCache<>(MAX_ENTRIES);

    private VehicleImageCache() {}

    public static Bitmap decode(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        String key = Integer.toHexString(base64.hashCode()) + ':' + base64.length();
        Bitmap cached = CACHE.get(key);
        if (cached != null && !cached.isRecycled()) return cached;
        try {
            byte[] raw = Base64.decode(base64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.length);
            if (bitmap != null) CACHE.put(key, bitmap);
            return bitmap;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static void clear() {
        CACHE.evictAll();
    }
}
