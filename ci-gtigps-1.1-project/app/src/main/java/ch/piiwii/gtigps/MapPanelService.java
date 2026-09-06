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
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.maplibre.android.MapLibre;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.Style;

import java.util.Calendar;
import java.util.Locale;

public class MapPanelService extends Service implements LocationListener {
    private static final int NOTIFICATION_ID = 1101;
    private static final String CHANNEL_ID = "gti_gps_panel";
    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_HEIGHT = 350;
    private static final int PANEL_X = 330;
    private static final int PANEL_Y = 47;

    private WindowManager windowManager;
    private FrameLayout panel;
    private MapView mapView;
    private MapLibreMap map;
    private TextView statusText;
    private TextView modeText;
    private CarMarkerView carMarker;
    private LocationManager locationManager;
    private Location lastLocation;
    private boolean following = true;
    private boolean panelAdded = false;
    private final Handler gpsWatchdog = new Handler(Looper.getMainLooper());
    private final Runnable gpsWatchdogTask = new Runnable() {
        @Override public void run() {
            updateGpsFreshnessStatus();
            gpsWatchdog.postDelayed(this, 2000L);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        MapLibre.getInstance(getApplicationContext());
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : ActionReceiver.ACTION_SHOW;
        if (ActionReceiver.ACTION_HIDE.equals(action)) {
            hidePanel();
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(NOTIFICATION_ID, buildNotification());

        if (ActionReceiver.ACTION_REFRESH.equals(action)) {
            if (panelAdded) {
                applyMapStyle();
                if (lastLocation != null) moveCamera(lastLocation, true);
                return START_STICKY;
            }
            stopSelf();
            return START_NOT_STICKY;
        }

        showPanel();
        return START_STICKY;
    }

    private void showPanel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            GpsState.prefs(this).edit().putBoolean(GpsState.KEY_PANEL_ENABLED, false).apply();
            GTIGpsWidgetProvider.updateAll(this);
            stopSelf();
            return;
        }
        if (panelAdded) return;

        panel = new FrameLayout(this);
        panel.setBackgroundColor(Color.rgb(9, 9, 9));

        mapView = new MapView(this);
        mapView.onCreate(null);
        panel.addView(mapView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        View topShade = makeBar(0xE6121212);
        FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, 42, Gravity.TOP);
        panel.addView(topShade, topLp);

        statusText = makeText("GPS · acquisition…", 14, Color.WHITE, true);
        FrameLayout.LayoutParams statusLp = new FrameLayout.LayoutParams(220, 42, Gravity.TOP | Gravity.START);
        statusLp.leftMargin = 10;
        panel.addView(statusText, statusLp);

        modeText = makeText("SUIVI", 11, 0xFFD71920, true);
        FrameLayout.LayoutParams modeLp = new FrameLayout.LayoutParams(62, 42, Gravity.TOP | Gravity.END);
        modeLp.rightMargin = 90;
        panel.addView(modeText, modeLp);

        Button followButton = makeButton("◎", 34);
        FrameLayout.LayoutParams followLp = new FrameLayout.LayoutParams(40, 36, Gravity.TOP | Gravity.END);
        followLp.topMargin = 3;
        followLp.rightMargin = 46;
        panel.addView(followButton, followLp);
        followButton.setOnClickListener(v -> {
            following = !following;
            modeText.setText(following ? "SUIVI" : "LIBRE");
            if (following && lastLocation != null) moveCamera(lastLocation, true);
        });

        Button closeButton = makeButton("×", 26);
        FrameLayout.LayoutParams closeLp = new FrameLayout.LayoutParams(40, 36, Gravity.TOP | Gravity.END);
        closeLp.topMargin = 3;
        closeLp.rightMargin = 3;
        panel.addView(closeButton, closeLp);
        closeButton.setOnClickListener(v -> {
            hidePanel();
            stopSelf();
        });

        View bottomShade = makeBar(0xE6121212);
        FrameLayout.LayoutParams bottomLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, 44, Gravity.BOTTOM);
        panel.addView(bottomShade, bottomLp);

        TextView navText = makeText("CARTE / POSITION · routage à venir", 12, 0xFFE6E6E6, false);
        FrameLayout.LayoutParams navLp = new FrameLayout.LayoutParams(260, 44, Gravity.BOTTOM | Gravity.START);
        navLp.leftMargin = 10;
        panel.addView(navText, navLp);

        Button minus = makeButton("−", 26);
        FrameLayout.LayoutParams minusLp = new FrameLayout.LayoutParams(40, 36, Gravity.BOTTOM | Gravity.END);
        minusLp.bottomMargin = 4;
        minusLp.rightMargin = 48;
        panel.addView(minus, minusLp);
        minus.setOnClickListener(v -> changeZoom(-0.7f));

        Button plus = makeButton("+", 24);
        FrameLayout.LayoutParams plusLp = new FrameLayout.LayoutParams(40, 36, Gravity.BOTTOM | Gravity.END);
        plusLp.bottomMargin = 4;
        plusLp.rightMargin = 4;
        panel.addView(plus, plusLp);
        plus.setOnClickListener(v -> changeZoom(0.7f));

        carMarker = new CarMarkerView(this);
        FrameLayout.LayoutParams markerLp = new FrameLayout.LayoutParams(36, 42, Gravity.CENTER);
        markerLp.bottomMargin = 10;
        panel.addView(carMarker, markerLp);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                PANEL_WIDTH,
                PANEL_HEIGHT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = PANEL_X;
        params.y = PANEL_Y;
        params.windowAnimations = 0;

        try {
            windowManager.addView(panel, params);
            panelAdded = true;
            GpsState.prefs(this).edit().putBoolean(GpsState.KEY_PANEL_ENABLED, true).apply();
            mapView.onStart();
            mapView.onResume();
            mapView.getMapAsync(mapLibreMap -> {
                map = mapLibreMap;
                map.getUiSettings().setLogoEnabled(false);
                map.getUiSettings().setCompassEnabled(false);
                map.getUiSettings().setRotateGesturesEnabled(true);
                map.getUiSettings().setTiltGesturesEnabled(false);
                applyMapStyle();
                LatLng swiss = new LatLng(46.8182, 8.2275);
                map.moveCamera(CameraUpdateFactory.newCameraPosition(
                        new CameraPosition.Builder().target(swiss).zoom(7.0).build()));
                if (lastLocation != null) moveCamera(lastLocation, false);
            });
            startGps();
            gpsWatchdog.removeCallbacks(gpsWatchdogTask);
            gpsWatchdog.postDelayed(gpsWatchdogTask, 2000L);
            GTIGpsWidgetProvider.updateAll(this);
        } catch (Exception e) {
            panelAdded = false;
            GpsState.prefs(this).edit().putBoolean(GpsState.KEY_PANEL_ENABLED, false).apply();
            stopSelf();
        }
    }

    private void applyMapStyle() {
        if (map == null) return;
        String pref = GpsState.theme(this);
        boolean night;
        if ("night".equals(pref)) night = true;
        else if ("day".equals(pref)) night = false;
        else {
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            night = hour < 7 || hour >= 19;
        }
        String styleUrl = night
                ? "https://tiles.openfreemap.org/styles/dark"
                : "https://tiles.openfreemap.org/styles/liberty";
        map.setStyle(new Style.Builder().fromUri(styleUrl));
    }

    private void updateGpsFreshnessStatus() {
        if (statusText == null || locationManager == null) return;
        try {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                statusText.setText("GPS · désactivé");
                return;
            }
        } catch (Exception ignored) {}

        if (lastLocation == null) {
            statusText.setText("GPS · recherche du signal…");
            return;
        }

        long ageMs = Math.max(0L, System.currentTimeMillis() - lastLocation.getTime());
        if (ageMs >= 15000L) {
            statusText.setText("GPS · signal perdu · " + (ageMs / 1000L) + " s");
        } else if (ageMs >= 6000L) {
            statusText.setText("GPS · position ancienne · " + (ageMs / 1000L) + " s");
        }
    }

    private void startGps() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            statusText.setText("GPS · permission requise");
            return;
        }
        try {
            Location gpsLast = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (gpsLast != null) onLocationChanged(gpsLast);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0.5f, this);
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 5f, this);
            }
        } catch (Exception e) {
            statusText.setText("GPS · indisponible");
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location == null) return;
        if (lastLocation != null && location.getTime() < lastLocation.getTime() - 2000L) return;
        if (lastLocation != null
                && location.getProvider() != null
                && !LocationManager.GPS_PROVIDER.equals(location.getProvider())
                && lastLocation.getAccuracy() <= location.getAccuracy()
                && System.currentTimeMillis() - lastLocation.getTime() < 5000L) return;

        lastLocation = location;
        GpsState.prefs(this).edit()
                .putLong(GpsState.KEY_LAST_LAT, Double.doubleToRawLongBits(location.getLatitude()))
                .putLong(GpsState.KEY_LAST_LON, Double.doubleToRawLongBits(location.getLongitude()))
                .putFloat(GpsState.KEY_LAST_ACC, location.hasAccuracy() ? location.getAccuracy() : -1f)
                .putLong(GpsState.KEY_LAST_TIME, location.getTime())
                .putFloat(GpsState.KEY_LAST_BEARING, location.hasBearing() ? location.getBearing() : 0f)
                .apply();

        if (statusText != null) {
            long ageSec = Math.max(0L, (System.currentTimeMillis() - location.getTime()) / 1000L);
            String accuracy = location.hasAccuracy() ? String.format(Locale.US, "%.0f m", location.getAccuracy()) : "? m";
            statusText.setText("GPS · " + accuracy + " · " + ageSec + " s");
        }
        if (following) moveCamera(location, true);
        updateMarkerRotation(location);
        GTIGpsWidgetProvider.updateAll(this);
    }

    private void moveCamera(Location location, boolean animate) {
        if (map == null || location == null) return;
        float bearing = location.hasBearing() ? location.getBearing() : GpsState.prefs(this).getFloat(GpsState.KEY_LAST_BEARING, 0f);
        boolean vehicle = "vehicle".equals(GpsState.orientation(this));
        CameraPosition cp = new CameraPosition.Builder()
                .target(new LatLng(location.getLatitude(), location.getLongitude()))
                .zoom(GpsState.zoom(this))
                .bearing(vehicle ? bearing : 0f)
                .tilt(0)
                .build();
        if (animate) map.easeCamera(CameraUpdateFactory.newCameraPosition(cp), 450);
        else map.moveCamera(CameraUpdateFactory.newCameraPosition(cp));
        updateMarkerRotation(location);
    }

    private void updateMarkerRotation(Location location) {
        if (carMarker == null || location == null) return;
        float bearing = location.hasBearing() ? location.getBearing() : GpsState.prefs(this).getFloat(GpsState.KEY_LAST_BEARING, 0f);
        boolean vehicle = "vehicle".equals(GpsState.orientation(this));
        carMarker.setRotation(vehicle ? 0f : bearing);
    }

    private void changeZoom(float delta) {
        float next = Math.max(10f, Math.min(19f, GpsState.zoom(this) + delta));
        GpsState.prefs(this).edit().putFloat(GpsState.KEY_ZOOM, next).apply();
        if (lastLocation != null) moveCamera(lastLocation, true);
        else if (map != null) map.animateCamera(CameraUpdateFactory.zoomTo(next), 250);
    }

    private View makeBar(int color) {
        View v = new View(this);
        v.setBackgroundColor(color);
        return v;
    }

    private TextView makeText(String text, float sp, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        tv.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tv.setSingleLine(true);
        return tv;
    }

    private Button makeButton(String text, float sp) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(sp);
        b.setTextColor(Color.WHITE);
        b.setPadding(0, 0, 0, 0);
        b.setMinHeight(0);
        b.setMinWidth(0);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xC8161616);
        bg.setCornerRadius(7f);
        bg.setStroke(1, 0xFF555555);
        b.setBackground(bg);
        return b;
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent openPi = PendingIntent.getActivity(this, 10, open,
                PendingIntent.FLAG_UPDATE_CURRENT | immutableFlag());
        Intent hide = new Intent(this, ActionReceiver.class).setAction(ActionReceiver.ACTION_HIDE);
        PendingIntent hidePi = PendingIntent.getBroadcast(this, 11, hide,
                PendingIntent.FLAG_UPDATE_CURRENT | immutableFlag());

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("GTI GPS Widget")
                .setContentText("Carte GPS active · 380 × 350")
                .setOngoing(true)
                .setContentIntent(openPi)
                .addAction(new Notification.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel, "Masquer", hidePi).build())
                .build();
    }

    private int immutableFlag() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "GTI GPS", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Panneau de carte GPS externe");
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(channel);
        }
    }

    private void hidePanel() {
        gpsWatchdog.removeCallbacks(gpsWatchdogTask);
        try {
            if (locationManager != null) locationManager.removeUpdates(this);
        } catch (Exception ignored) {}
        if (mapView != null) {
            try { mapView.onPause(); } catch (Exception ignored) {}
            try { mapView.onStop(); } catch (Exception ignored) {}
            try { mapView.onDestroy(); } catch (Exception ignored) {}
        }
        if (panelAdded && panel != null && windowManager != null) {
            try { windowManager.removeView(panel); } catch (Exception ignored) {}
        }
        panelAdded = false;
        panel = null;
        mapView = null;
        map = null;
        GpsState.prefs(this).edit().putBoolean(GpsState.KEY_PANEL_ENABLED, false).apply();
        GTIGpsWidgetProvider.updateAll(this);
    }

    @Override
    public void onProviderEnabled(String provider) {
        if (statusText != null) statusText.setText("GPS · acquisition…");
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (LocationManager.GPS_PROVIDER.equals(provider) && statusText != null) statusText.setText("GPS · désactivé");
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

    @Override
    public void onDestroy() {
        hidePanel();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
