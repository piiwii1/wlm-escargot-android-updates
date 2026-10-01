package ch.piiwii.mapsnavprobe;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * Renders the approved GTI compass artwork.
 * The complete dial stays fixed; only the red north needle moves.
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
        float c = size * 0.5f;

        // Exact approved artwork: frame, graduations, N/E/S/O and the whole rose stay fixed.
        canvas.drawBitmap(reference, 0f, 0f, paint);

        // Remove only the original fixed red north needle by replacing it with the
        // exact silver south point from the same reference, mirrored 180 degrees.
        Path northReplacement = needleMask(size, true);
        canvas.save();
        canvas.clipPath(northReplacement);
        canvas.rotate(180f, c, c);
        canvas.drawBitmap(reference, 0f, 0f, paint);
        canvas.restore();

        // Draw only the original red needle and rotate that one element according
        // to the live heading. The rest of the compass never moves.
        canvas.save();
        canvas.rotate(-normalize(headingDegrees), c, c);
        canvas.clipPath(needleMask(size, false));
        canvas.drawBitmap(reference, 0f, 0f, paint);
        canvas.restore();

        // Repaint the centre hub from the untouched reference so it remains fixed,
        // perfectly round and visually identical to the approved image.
        Path hub = new Path();
        hub.addCircle(c, c, size * 0.075f, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(hub);
        canvas.drawBitmap(reference, 0f, 0f, paint);
        canvas.restore();

        return out;
    }

    private static Path needleMask(int size, boolean replacement) {
        float c = size * 0.5f;
        Path p = new Path();
        if (replacement) {
            // Slightly wider mask removes the original red edge/shadow completely.
            p.moveTo(c, size * 0.190f);
            p.lineTo(size * 0.430f, size * 0.468f);
            p.lineTo(size * 0.466f, size * 0.515f);
            p.lineTo(size * 0.534f, size * 0.515f);
            p.lineTo(size * 0.570f, size * 0.468f);
        } else {
            // Exact moving needle area from the approved reference.
            p.moveTo(c, size * 0.198f);
            p.lineTo(size * 0.438f, size * 0.458f);
            p.lineTo(size * 0.470f, size * 0.505f);
            p.lineTo(size * 0.530f, size * 0.505f);
            p.lineTo(size * 0.562f, size * 0.458f);
        }
        p.close();
        return p;
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
