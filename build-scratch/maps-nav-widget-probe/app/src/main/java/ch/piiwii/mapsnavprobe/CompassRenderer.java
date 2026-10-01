package ch.piiwii.mapsnavprobe;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.io.InputStream;

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
        canvas.save();
        canvas.clipPath(needleMask(size, true));
        canvas.rotate(180f, c, c);
        canvas.drawBitmap(reference, 0f, 0f, paint);
        canvas.restore();

        // Only the original red needle rotates with the live heading.
        canvas.save();
        canvas.rotate(-normalize(headingDegrees), c, c);
        canvas.clipPath(needleMask(size, false));
        canvas.drawBitmap(reference, 0f, 0f, paint);
        canvas.restore();

        // Centre hub remains fixed and pixel-identical to the approved reference.
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
            p.moveTo(c, size * 0.190f);
            p.lineTo(size * 0.430f, size * 0.468f);
            p.lineTo(size * 0.466f, size * 0.515f);
            p.lineTo(size * 0.534f, size * 0.515f);
            p.lineTo(size * 0.570f, size * 0.468f);
        } else {
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
            try (InputStream in = context.getAssets().open("piiwii_compass_reference.png")) {
                sourceReference = BitmapFactory.decodeStream(in);
            } catch (Throwable ignored) {
                sourceReference = null;
            }
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
