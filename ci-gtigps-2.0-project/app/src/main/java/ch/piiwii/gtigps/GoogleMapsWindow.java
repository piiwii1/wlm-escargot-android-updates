package ch.piiwii.gtigps;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import java.lang.reflect.Method;

public final class GoogleMapsWindow {
    public static final String MAPS_PACKAGE = "com.google.android.apps.maps";
    private static final int WINDOWING_MODE_FREEFORM = 5;

    private GoogleMapsWindow() {}

    public static boolean isInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo(MAPS_PACKAGE, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static Intent mapsLaunchIntent(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent i = pm.getLaunchIntentForPackage(MAPS_PACKAGE);
        if (i == null) {
            i = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0"));
            i.setPackage(MAPS_PACKAGE);
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            i.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
        }
        return i;
    }

    public static LaunchResult launchWindow(Context context) {
        if (!isInstalled(context)) return new LaunchResult(false, false, "Google Maps non installé");
        Rect bounds = WindowPrefs.getBounds(context);
        Intent i = mapsLaunchIntent(context);
        try {
            ActivityOptions options = ActivityOptions.makeBasic();
            boolean freeformRequested = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                options.setLaunchBounds(bounds);
                freeformRequested = requestFreeformWindowing(options);
            }
            Bundle bundle = options.toBundle();
            context.startActivity(i, bundle);
            WindowPrefs.setLastMode(context,
                    "fenêtre demandée " + bounds.left + "," + bounds.top + " " + bounds.width() + "×" + bounds.height()
                            + (freeformRequested ? " · freeform=oui" : " · freeform=limité"));
            return new LaunchResult(true, freeformRequested, "Google Maps lancé dans la zone calibrée");
        } catch (Throwable first) {
            try {
                context.startActivity(i);
                WindowPrefs.setLastMode(context, "plein écran (fallback : " + first.getClass().getSimpleName() + ")");
                return new LaunchResult(true, false, "Le firmware a refusé le fenêtrage ; Maps a été ouvert en plein écran");
            } catch (Throwable second) {
                WindowPrefs.setLastMode(context, "échec : " + second.getClass().getSimpleName());
                return new LaunchResult(false, false, "Impossible d’ouvrir Google Maps");
            }
        }
    }

    public static LaunchResult launchFullScreen(Context context) {
        if (!isInstalled(context)) return new LaunchResult(false, false, "Google Maps non installé");
        try {
            Intent i = mapsLaunchIntent(context);
            context.startActivity(i);
            WindowPrefs.setLastMode(context, "plein écran");
            return new LaunchResult(true, false, "Google Maps lancé en plein écran");
        } catch (Throwable e) {
            return new LaunchResult(false, false, "Impossible d’ouvrir Google Maps");
        }
    }

    private static boolean requestFreeformWindowing(ActivityOptions options) {
        try {
            Method m = ActivityOptions.class.getDeclaredMethod("setLaunchWindowingMode", int.class);
            m.setAccessible(true);
            m.invoke(options, WINDOWING_MODE_FREEFORM);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static final class LaunchResult {
        public final boolean launched;
        public final boolean freeformRequested;
        public final String message;
        LaunchResult(boolean launched, boolean freeformRequested, String message) {
            this.launched = launched;
            this.freeformRequested = freeformRequested;
            this.message = message;
        }
    }
}
