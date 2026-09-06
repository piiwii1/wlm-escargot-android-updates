package ch.piiwii.gtialtimeter;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public class AltitudeService extends Service implements LocationListener, SensorEventListener {
    private static final String CHANNEL_ID = "gti_altimeter_gps";
    private static final int NOTIFICATION_ID = 1042;
    private static final long HEARTBEAT_MS = 4000L;
    private static final long GPS_REREQUEST_AFTER_MS = 25000L;
    private static final long GPS_REQUEST_THROTTLE_MS = 18000L;
    private static final long BARO_BOOTSTRAP_MAX_AGE_MS = 6L * 60L * 60L * 1000L;

    private final AltitudeFilter filter = new AltitudeFilter();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private LocationManager locationManager;
    private SensorManager sensorManager;
    private Sensor pressureSensor;
    private Sensor rotationSensor;
    private Sensor accelerometer;
    private Sensor magneticSensor;
    private final float[] gravity = new float[3];
    private final float[] geomagnetic = new float[3];
    private boolean haveGravity;
    private boolean haveMagnetic;
    private double trendAnchorAlt = Double.NaN;
    private long trendAnchorTime;
    private String trendAnchorSource = "";
    private long lastGpsRequestAt;
    private long lastBaroUiAt;

    private final Runnable heartbeat = new Runnable() {
        @Override public void run() {
            AltitudeState.touchService(AltitudeService.this);
            refreshProviderState();
            long now = System.currentTimeMillis();
            long lastCallback = AltitudeState.lastGpsCallback(AltitudeService.this);
            if (AltitudeState.gpsEnabled(AltitudeService.this)
                    && (lastCallback <= 0L || now - lastCallback > GPS_REREQUEST_AFTER_MS)) {
                requestGps(false);
            }
            updateNotificationFromState();
            GtiWidgetUpdater.updateAll(AltitudeService.this);
            handler.postDelayed(this, HEARTBEAT_MS);
        }
    };

    public static void startIfPermitted(Context context) {
        if (Build.VERSION.SDK_INT >= 23
                && context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            GtiWidgetUpdater.updateAll(context);
            return;
        }
        Intent i = new Intent(context, AltitudeService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i); else context.startService(i);
        } catch (RuntimeException ignored) {
            ServiceWatchdog.schedule(context, 30000L);
        }
    }

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(NOTIFICATION_ID, buildNotification("Recherche GPS…"));
        AltitudeState.touchService(this);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            if (pressureSensor != null) sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);
            if (rotationSensor != null) {
                sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI);
            } else {
                if (accelerometer != null) sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
                if (magneticSensor != null) sensorManager.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_UI);
            }
        }
        AltitudeState.saveBarometer(this, pressureSensor != null, Float.NaN);
        AltitudeState.setCompassSensors(this, rotationSensor != null || (accelerometer != null && magneticSensor != null));

        refreshProviderState();
        requestGps(true);
        ServiceWatchdog.schedule(this, ServiceWatchdog.NORMAL_DELAY_MS);
        handler.post(heartbeat);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification("GTI Altimeter actif"));
        AltitudeState.touchService(this);
        refreshProviderState();
        requestGps(true);
        ServiceWatchdog.schedule(this, ServiceWatchdog.NORMAL_DELAY_MS);
        return START_STICKY;
    }

    private void requestGps(boolean force) {
        if (locationManager == null) return;
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        long now = System.currentTimeMillis();
        if (!force && now - lastGpsRequestAt < GPS_REQUEST_THROTTLE_MS) return;
        lastGpsRequestAt = now;
        try {
            locationManager.removeUpdates(this);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this, Looper.getMainLooper());
        } catch (SecurityException | IllegalArgumentException ignored) { }
    }

    private void refreshProviderState() {
        boolean enabled = false;
        try { enabled = locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER); }
        catch (Exception ignored) { }
        AltitudeState.setGpsEnabled(this, enabled);
    }

    @Override public void onLocationChanged(Location location) {
        if (location == null) return;
        long now = System.currentTimeMillis();
        long fixTime = location.getTime() > 0 ? location.getTime() : now;
        AltitudeState.noteGpsCallback(this, fixTime);
        AltitudeState.touchService(this);

        if (location.hasBearing() && (!location.hasSpeed() || location.getSpeed() >= 1.2f)) {
            AltitudeState.saveHeading(this, location.getBearing(), "GPS");
        }

        float hAcc = location.hasAccuracy() ? location.getAccuracy() : Float.NaN;
        float vAcc = Float.NaN;
        if (Build.VERSION.SDK_INT >= 26 && location.hasVerticalAccuracy()) vAcc = location.getVerticalAccuracyMeters();
        double raw = location.hasAltitude() ? location.getAltitude() : Double.NaN;

        if (Math.abs(now - fixTime) > 20000L) {
            AltitudeState.saveRejected(this, location.getLatitude(), location.getLongitude(), hAcc, raw, vAcc, fixTime,
                    "position GPS périmée");
            updateNotificationFromState();
            GtiWidgetUpdater.updateAll(this);
            return;
        }
        if (!location.hasAltitude()) {
            AltitudeState.saveRejected(this, location.getLatitude(), location.getLongitude(), hAcc, Double.NaN, vAcc, fixTime,
                    "altitude GPS absente");
            updateNotificationFromState();
            GtiWidgetUpdater.updateAll(this);
            return;
        }

        double corrected = raw;
        AltitudeFilter.Result result = filter.push(corrected, hAcc, vAcc, fixTime);
        if (result.accepted) {
            AltitudeState.saveLocation(this, location.getLatitude(), location.getLongitude(), hAcc,
                    raw, corrected, result.filtered, vAcc, fixTime, "GPS", false);
            maybeCalibrateBarometer(result.filtered);
            updateTrend(result.filtered, fixTime, "GPS");
        } else {
            AltitudeState.saveRejected(this, location.getLatitude(), location.getLongitude(), hAcc, raw, vAcc, fixTime, result.reason);
        }
        updateNotificationFromState();
        GtiWidgetUpdater.updateAll(this);
    }

    private void maybeCalibrateBarometer(double acceptedAltitude) {
        float pressure = AltitudeState.pressure(this);
        long pressureTime = AltitudeState.pressureTime(this);
        if (Float.isNaN(pressure) || pressureTime <= 0L) return;
        if (System.currentTimeMillis() - pressureTime > 10000L) return;
        AltitudeState.calibrateBarometer(this, pressure, acceptedAltitude);
    }

    private void bootstrapBarometerIfNeeded(float pressure) {
        if (AltitudeState.baroCalibrated(this)) return;
        double stored = AltitudeState.filtered(this);
        long last = AltitudeState.lastUpdate(this);
        if (Double.isNaN(stored) || Double.isInfinite(stored) || last <= 0L) return;
        if (System.currentTimeMillis() - last > BARO_BOOTSTRAP_MAX_AGE_MS) return;
        AltitudeState.calibrateBarometer(this, pressure, stored);
    }

    private void updateBarometricAltitude(float pressure) {
        bootstrapBarometerIfNeeded(pressure);
        if (!AltitudeState.baroCalibrated(this)) return;
        float anchorPressure = AltitudeState.baroAnchorPressure(this);
        double anchorAltitude = AltitudeState.baroAnchorAltitude(this);
        if (Float.isNaN(anchorPressure) || anchorPressure <= 100f || Double.isNaN(anchorAltitude)) return;

        double delta = 44330.0 * (1.0 - Math.pow(pressure / anchorPressure, 0.19029495718363465));
        double estimate = anchorAltitude + delta;
        double previous = AltitudeState.baroAltitude(this);
        long previousTime = AltitudeState.baroAltitudeTime(this);
        if (!Double.isNaN(previous) && previousTime > 0L && System.currentTimeMillis() - previousTime < 30000L) {
            estimate = previous + 0.28 * (estimate - previous);
        }
        AltitudeState.saveBaroAltitude(this, estimate);

        if (!AltitudeState.isGpsFresh(this)) {
            updateTrend(estimate, System.currentTimeMillis(), "BARO");
            long now = System.currentTimeMillis();
            if (now - lastBaroUiAt >= 1000L) {
                lastBaroUiAt = now;
                updateNotificationFromState();
                GtiWidgetUpdater.updateAll(this);
            }
        }
    }

    private void updateTrend(double altitude, long time, String source) {
        if (Double.isNaN(trendAnchorAlt) || !source.equals(trendAnchorSource)
                || trendAnchorTime <= 0L || time - trendAnchorTime > 120000L) {
            trendAnchorAlt = altitude;
            trendAnchorTime = time;
            trendAnchorSource = source;
            AltitudeState.setTrend(this, 0.0);
            return;
        }
        if (time - trendAnchorTime >= 30000L) {
            AltitudeState.setTrend(this, altitude - trendAnchorAlt);
            trendAnchorAlt = altitude;
            trendAnchorTime = time;
            trendAnchorSource = source;
        }
    }

    private static float bestAccuracy(float hAcc, float vAcc) {
        if (!Float.isNaN(vAcc)) return vAcc;
        if (!Float.isNaN(hAcc)) return hAcc;
        return Float.NaN;
    }

    @Override public void onProviderEnabled(String provider) {
        refreshProviderState();
        requestGps(true);
        GtiWidgetUpdater.updateAll(this);
    }
    @Override public void onProviderDisabled(String provider) {
        refreshProviderState();
        GtiWidgetUpdater.updateAll(this);
    }
    @Override public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PRESSURE && event.values.length > 0) {
            float pressure = event.values[0];
            AltitudeState.saveBarometer(this, true, pressure);
            updateBarometricAltitude(pressure);
            return;
        }
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] rotation = new float[9];
            float[] orientation = new float[3];
            SensorManager.getRotationMatrixFromVector(rotation, event.values);
            SensorManager.getOrientation(rotation, orientation);
            saveSensorHeading(orientation[0]);
            return;
        }
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            lowPass(event.values, gravity);
            haveGravity = true;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            lowPass(event.values, geomagnetic);
            haveMagnetic = true;
        }
        if (haveGravity && haveMagnetic) {
            float[] rotation = new float[9];
            float[] inclination = new float[9];
            if (SensorManager.getRotationMatrix(rotation, inclination, gravity, geomagnetic)) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(rotation, orientation);
                saveSensorHeading(orientation[0]);
            }
        }
    }

    private void saveSensorHeading(float azimuthRad) {
        long gpsHeadingAge = System.currentTimeMillis() - AltitudeState.headingTime(this);
        if ("GPS".equals(AltitudeState.headingSource(this)) && gpsHeadingAge < 5000L) return;
        float degrees = (float) Math.toDegrees(azimuthRad);
        if (degrees < 0f) degrees += 360f;
        AltitudeState.saveHeading(this, degrees, "Boussole");
    }

    private static void lowPass(float[] input, float[] output) {
        final float alpha = 0.18f;
        for (int i = 0; i < 3; i++) output[i] += alpha * (input[i] - output[i]);
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "GTI Altimeter GPS", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Maintient altitude, précision et cap à jour pour le widget");
            channel.setShowBadge(false);
            nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String text) {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        return b.setSmallIcon(R.drawable.ic_altimeter_notification)
                .setContentTitle("GTI Altimeter Widget")
                .setContentText(text)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }

    private void updateNotificationFromState() {
        double altitude = AltitudeState.displayAltitude(this);
        String source = AltitudeState.displaySource(this);
        String text;
        if (Double.isNaN(altitude)) {
            text = AltitudeState.gpsEnabled(this) ? "Recherche GPS… · service actif" : "GPS désactivé";
        } else if ("BARO".equals(source)) {
            text = Math.round(altitude) + " m · BARO";
        } else {
            float acc = bestAccuracy(AltitudeState.acceptedHAcc(this), AltitudeState.acceptedVAcc(this));
            text = Math.round(altitude) + " m" + (Float.isNaN(acc) ? "" : " · ±" + Math.round(acc) + " m");
        }
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, buildNotification(text));
    }

    @Override public void onTaskRemoved(Intent rootIntent) {
        ServiceWatchdog.schedule(this, 5000L);
        super.onTaskRemoved(rootIntent);
    }

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (locationManager != null) {
            try { locationManager.removeUpdates(this); } catch (SecurityException ignored) { }
        }
        if (sensorManager != null) sensorManager.unregisterListener(this);
        if (ServiceWatchdog.hasWidgets(this)) ServiceWatchdog.schedule(this, 5000L);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
