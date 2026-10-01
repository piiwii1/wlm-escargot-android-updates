package ch.piiwii.mapsnavprobe;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.view.Surface;
import android.view.WindowManager;

/** Lightweight sensor listener used only while an idle widget is in compass mode. */
public final class CompassController implements SensorEventListener {
    private static final CompassController INSTANCE = new CompassController();
    private static final long MIN_UPDATE_MS = 160L;
    private static final long FORCE_UPDATE_MS = 900L;
    private static final float MIN_VISIBLE_CHANGE = 0.35f;

    private Context appContext;
    private SensorManager sensorManager;
    private Sensor rotationVector;
    private Sensor accelerometer;
    private Sensor magneticField;
    private boolean running;
    private final float[] gravity = new float[3];
    private final float[] geomagnetic = new float[3];
    private boolean haveGravity;
    private boolean haveGeomagnetic;
    private float smoothedHeading = Float.NaN;
    private float lastPublishedHeading = Float.NaN;
    private long lastWidgetUpdate;

    private CompassController() {}

    public static synchronized void start(Context context) {
        INSTANCE.startInternal(context.getApplicationContext());
    }

    public static synchronized void stop() {
        INSTANCE.stopInternal();
    }

    public static float getLastHeading(Context context) {
        return context.getSharedPreferences(MapsNotificationListener.PREFS, Context.MODE_PRIVATE)
                .getFloat("compass_heading", 0f);
    }

    private void startInternal(Context context) {
        if (running) return;
        appContext = context;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager == null) return;

        rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (rotationVector == null) {
            rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
        }
        if (rotationVector != null) {
            running = sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_GAME);
            if (running) return;
        }

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (accelerometer != null && magneticField != null) {
            boolean a = sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            boolean m = sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_GAME);
            running = a && m;
        }
    }

    private void stopInternal() {
        if (sensorManager != null) {
            try { sensorManager.unregisterListener(this); } catch (Throwable ignored) {}
        }
        running = false;
        haveGravity = false;
        haveGeomagnetic = false;
        smoothedHeading = Float.NaN;
        lastPublishedHeading = Float.NaN;
        lastWidgetUpdate = 0L;
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (appContext == null || event == null || event.sensor == null) return;

        float heading;
        int type = event.sensor.getType();
        if (type == Sensor.TYPE_ROTATION_VECTOR || type == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) {
            heading = headingFromRotationVector(event.values);
        } else {
            if (type == Sensor.TYPE_ACCELEROMETER) {
                lowPass(event.values, gravity, 0.14f);
                haveGravity = true;
            } else if (type == Sensor.TYPE_MAGNETIC_FIELD) {
                lowPass(event.values, geomagnetic, 0.12f);
                haveGeomagnetic = true;
            }
            if (!haveGravity || !haveGeomagnetic) return;
            heading = headingFromAccelMag();
        }

        if (Float.isNaN(heading)) return;
        heading = normalize(heading);

        if (Float.isNaN(smoothedHeading)) {
            smoothedHeading = heading;
        } else {
            float delta = shortestDelta(smoothedHeading, heading);
            float abs = Math.abs(delta);
            float alpha;
            if (abs < 2f) alpha = 0.20f;
            else if (abs < 8f) alpha = 0.30f;
            else if (abs < 25f) alpha = 0.45f;
            else alpha = 0.62f;
            smoothedHeading = normalize(smoothedHeading + delta * alpha);
        }

        long now = SystemClock.elapsedRealtime();
        if (now - lastWidgetUpdate < MIN_UPDATE_MS) return;

        boolean changedEnough = Float.isNaN(lastPublishedHeading)
                || Math.abs(shortestDelta(lastPublishedHeading, smoothedHeading)) >= MIN_VISIBLE_CHANGE;
        boolean forceRefresh = now - lastWidgetUpdate >= FORCE_UPDATE_MS;
        if (!changedEnough && !forceRefresh) return;

        lastWidgetUpdate = now;
        lastPublishedHeading = smoothedHeading;

        appContext.getSharedPreferences(MapsNotificationListener.PREFS, Context.MODE_PRIVATE)
                .edit().putFloat("compass_heading", smoothedHeading).apply();
        MapsNavWidget.updateCompassWidgets(appContext, smoothedHeading);
        MapsNavSquareWidget.updateCompassWidgets(appContext, smoothedHeading);
    }

    private float headingFromRotationVector(float[] values) {
        float[] rotation = new float[9];
        SensorManager.getRotationMatrixFromVector(rotation, values);
        return headingFromMatrix(rotation);
    }

    private float headingFromAccelMag() {
        float[] rotation = new float[9];
        if (!SensorManager.getRotationMatrix(rotation, null, gravity, geomagnetic)) return Float.NaN;
        return headingFromMatrix(rotation);
    }

    private float headingFromMatrix(float[] rotation) {
        float[] adjusted = new float[9];
        int screenRotation = Surface.ROTATION_0;
        try {
            WindowManager wm = (WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE);
            if (wm != null) screenRotation = wm.getDefaultDisplay().getRotation();
        } catch (Throwable ignored) {}

        int x = SensorManager.AXIS_X;
        int y = SensorManager.AXIS_Y;
        if (screenRotation == Surface.ROTATION_90) {
            x = SensorManager.AXIS_Y;
            y = SensorManager.AXIS_MINUS_X;
        } else if (screenRotation == Surface.ROTATION_180) {
            x = SensorManager.AXIS_MINUS_X;
            y = SensorManager.AXIS_MINUS_Y;
        } else if (screenRotation == Surface.ROTATION_270) {
            x = SensorManager.AXIS_MINUS_Y;
            y = SensorManager.AXIS_X;
        }

        if (!SensorManager.remapCoordinateSystem(rotation, x, y, adjusted)) return Float.NaN;
        float[] orientation = new float[3];
        SensorManager.getOrientation(adjusted, orientation);
        return (float) Math.toDegrees(orientation[0]);
    }

    private static void lowPass(float[] input, float[] output, float alpha) {
        for (int i = 0; i < 3 && i < input.length; i++) {
            output[i] = output[i] == 0f ? input[i] : output[i] + alpha * (input[i] - output[i]);
        }
    }

    private static float shortestDelta(float from, float to) {
        return ((to - from + 540f) % 360f) - 180f;
    }

    private static float normalize(float value) {
        float n = value % 360f;
        return n < 0f ? n + 360f : n;
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
