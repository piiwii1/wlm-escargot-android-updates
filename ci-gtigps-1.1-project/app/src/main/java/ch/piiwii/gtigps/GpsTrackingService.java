package ch.piiwii.gtigps;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

public class GpsTrackingService extends Service implements LocationListener {
    private static final int NOTIFICATION_ID = 1201;
    private static final String CHANNEL_ID = "gti_gps_tracking";

    private LocationManager locationManager;
    private Location lastAccepted;
    private long lastRouteRequestAt;
    private Location lastRouteOrigin;
    private boolean routeRequestInFlight;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : ActionReceiver.ACTION_START_GPS;
        if (ActionReceiver.ACTION_STOP_GPS.equals(action)) {
            stopTracking();
            stopSelf();
            return START_NOT_STICKY;
        }
        startForeground(NOTIFICATION_ID, buildNotification("Recherche de position…"));
        startTracking();
        return START_STICKY;
    }

    private void startTracking() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            GpsState.prefs(this).edit().putBoolean(GpsState.KEY_TRACKING_ENABLED, false).apply();
            stopSelf();
            return;
        }
        GpsState.prefs(this).edit().putBoolean(GpsState.KEY_TRACKING_ENABLED, true).apply();
        try {
            Location gpsLast = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location netLast = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            Location best = newer(gpsLast, netLast);
            if (best != null) onLocationChanged(best);
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0.5f, this);
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 5f, this);
            }
        } catch (Exception ignored) {
        }
        GTIGpsWidgetProvider.updateAll(this);
    }

    private Location newer(Location a, Location b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.getTime() >= b.getTime() ? a : b;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location == null) return;
        if (lastAccepted != null && location.getTime() < lastAccepted.getTime() - 2000L) return;
        if (lastAccepted != null && !LocationManager.GPS_PROVIDER.equals(location.getProvider())
                && lastAccepted.hasAccuracy() && location.hasAccuracy()
                && lastAccepted.getAccuracy() <= location.getAccuracy()
                && System.currentTimeMillis() - lastAccepted.getTime() < 5000L) return;

        lastAccepted = location;
        GpsState.prefs(this).edit()
                .putLong(GpsState.KEY_LAST_LAT, Double.doubleToRawLongBits(location.getLatitude()))
                .putLong(GpsState.KEY_LAST_LON, Double.doubleToRawLongBits(location.getLongitude()))
                .putFloat(GpsState.KEY_LAST_ACC, location.hasAccuracy() ? location.getAccuracy() : -1f)
                .putLong(GpsState.KEY_LAST_TIME, location.getTime())
                .putFloat(GpsState.KEY_LAST_BEARING, location.hasBearing() ? location.getBearing() : 0f)
                .putBoolean(GpsState.KEY_TRACKING_ENABLED, true)
                .apply();

        String acc = location.hasAccuracy() ? Math.round(location.getAccuracy()) + " m" : "position reçue";
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, buildNotification("GPS actif · " + acc));
        GTIGpsWidgetProvider.updateAll(this);
        maybeRefreshRoute(location);
    }

    private void maybeRefreshRoute(Location location) {
        if (!GpsState.navigationActive(this) || routeRequestInFlight) return;
        long now = System.currentTimeMillis();
        String geometry = GpsState.prefs(this).getString(GpsState.KEY_ROUTE_GEOMETRY, "");
        boolean routeMissing = geometry == null || geometry.length() < 10;
        boolean movedEnough = lastRouteOrigin == null || location.distanceTo(lastRouteOrigin) >= 45f;
        boolean timeEnough = now - lastRouteRequestAt >= 30000L;
        if (!routeMissing && !(movedEnough && timeEnough)) return;

        routeRequestInFlight = true;
        lastRouteRequestAt = now;
        lastRouteOrigin = new Location(location);
        NavigationEngine.requestRoute(this,
                location.getLatitude(), location.getLongitude(),
                GpsState.destLat(this), GpsState.destLon(this),
                new NavigationEngine.RouteCallback() {
                    @Override public void onRoute(double distanceMeters, double durationSeconds, String instruction) {
                        routeRequestInFlight = false;
                        GTIGpsWidgetProvider.updateAll(GpsTrackingService.this);
                    }
                    @Override public void onError(String message) {
                        routeRequestInFlight = false;
                    }
                });
    }

    private Notification buildNotification(String text) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent openPi = PendingIntent.getActivity(this, 120, open,
                PendingIntent.FLAG_UPDATE_CURRENT | immutableFlag());
        Intent stop = new Intent(this, ActionReceiver.class).setAction(ActionReceiver.ACTION_STOP_GPS);
        PendingIntent stopPi = PendingIntent.getBroadcast(this, 121, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | immutableFlag());

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("GTI GPS Widget")
                .setContentText(text)
                .setOngoing(true)
                .setContentIntent(openPi)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_menu_close_clear_cancel, "Arrêter GPS", stopPi).build())
                .build();
    }

    private int immutableFlag() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "GTI GPS · localisation", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Localisation GPS continue indépendante du panneau carte");
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(channel);
        }
    }

    private void stopTracking() {
        try {
            if (locationManager != null) locationManager.removeUpdates(this);
        } catch (Exception ignored) {}
        GpsState.prefs(this).edit().putBoolean(GpsState.KEY_TRACKING_ENABLED, false).apply();
        GTIGpsWidgetProvider.updateAll(this);
    }

    @Override public void onProviderEnabled(String provider) {}
    @Override public void onProviderDisabled(String provider) {}
    @SuppressWarnings("deprecation")
    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onDestroy() {
        stopTracking();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
