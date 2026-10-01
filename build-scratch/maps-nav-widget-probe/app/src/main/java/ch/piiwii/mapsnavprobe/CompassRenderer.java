package ch.piiwii.mapsnavprobe;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * Renders the approved GTI compass artwork. The outer dial stays fixed;
 * only the inner compass rose rotates with the live device heading.
 */
public final class CompassRenderer {
    private CompassRenderer() {}

    private static Bitmap sourceReference;
    private static Bitmap scaledReference;
    private static int scaledSize = -1;

    public static Bitmap render(Context context, float headingDegrees, int requestedSizePx) {
        int size = Math.max(240, Math.min(420, requestedSizePx));
        Bitmap reference = getScaledReference(context, size);

        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

        // Exact approved artwork as the fixed compass body.
        canvas.drawBitmap(reference, 0f, 0f, paint);

        // Only the central rose / inner ring moves. N/E/S/O, graduations and
        // the outer GTI frame remain perfectly still on the launcher.
        float c = size * 0.5f;
        float movingRadius = size * 0.274f;
        Path movingArea = new Path();
        movingArea.addCircle(c, c, movingRadius, Path.Direction.CW);

        canvas.save();
        canvas.clipPath(movingArea);
        canvas.rotate(-normalize(headingDegrees), c, c);
        canvas.drawBitmap(reference, 0f, 0f, paint);
        canvas.restore();

        return out;
    }

    private static synchronized Bitmap getScaledReference(Context context, int size) {
        if (sourceReference == null || sourceReference.isRecycled()) {
            sourceReference = BitmapFactory.decodeResource(
                    context.getResources(), R.drawable.piiwii_compass_reference);
            if (sourceReference == null) {
                return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            }
        }

        if (scaledReference == null || scaledReference.isRecycled() || scaledSize != size) {
            scaledReference = Bitmap.createScaledBitmap(sourceReference, size, size, true);
            scaledSize = size;
        }
        return scaledReference;
    }

    private static float normalize(float value) {
        float n = value % 360f;
        return n < 0f ? n + 360f : n;
    }
}
