package ch.piiwii.gtigps;

import android.app.Activity;
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

    public static boolean hasFreeformFeature(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false;
        try {
            return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT);
        } catch (Throwable ignored) {
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

        // IMPORTANT : on garde la tâche Google Maps existante afin que l'adresse,
        // l'itinéraire et la navigation en cours restent ceux de Google Maps.
        // Le trampoline Activity sert uniquement à donner au système un vrai contexte
        // d'Activity lorsqu'on vient d'un AppWidget / écran d'accueil.
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            i.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
        }
        return i;
    }

    /**
     * Cette méthode doit idéalement être appelée depuis WindowLaunchActivity.
     * Les firmwares Samsung/Topway respectent beaucoup mieux setLaunchBounds depuis
     * un vrai contexte Activity que depuis un BroadcastReceiver/AppWidgetProvider.
     */
    public static LaunchResult launchWindow(Activity host) {
        if (!isInstalled(host)) {
            return new LaunchResult(false, false, "Google Maps non installé");
        }

        Rect bounds = WindowPrefs.getBounds(host);
        Intent i = mapsLaunchIntent(host);
        try {
            ActivityOptions options = ActivityOptions.makeBasic();
            boolean freeformRequested = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                options.setLaunchBounds(bounds);
                freeformRequested = requestFreeformWindowing(options);
            }
            Bundle bundle = options.toBundle();
            host.startActivity(i, bundle);
            host.overridePendingTransition(0, 0);

            String mode = "accueil → Google Maps · zone "
                    + bounds.left + "," + bounds.top + " " + bounds.width() + "×" + bounds.height()
                    + " · freeform=" + (freeformRequested ? "demandé" : "non forcé")
                    + " · feature=" + (hasFreeformFeature(host) ? "oui" : "non");
            WindowPrefs.setLastMode(host, mode);
            return new LaunchResult(true, freeformRequested,
                    "Google Maps lancé sur la zone de test");
        } catch (Throwable first) {
            try {
                host.startActivity(i);
                host.overridePendingTransition(0, 0);
                WindowPrefs.setLastMode(host,
                        "plein écran fallback · " + first.getClass().getSimpleName());
                return new LaunchResult(true, false,
                        "Le firmware a refusé la fenêtre : Google Maps est ouvert en plein écran");
            } catch (Throwable second) {
                WindowPrefs.setLastMode(host,
                        "échec · " + second.getClass().getSimpleName());
                return new LaunchResult(false, false, "Impossible d’ouvrir Google Maps");
            }
        }
    }

    public static LaunchResult launchFullScreen(Activity host) {
        if (!isInstalled(host)) return new LaunchResult(false, false, "Google Maps non installé");
        try {
            Intent i = mapsLaunchIntent(host);
            ActivityOptions options = ActivityOptions.makeBasic();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                options.setLaunchBounds(null);
            }
            host.startActivity(i, options.toBundle());
            host.overridePendingTransition(0, 0);
            WindowPrefs.setLastMode(host, "plein écran");
            return new LaunchResult(true, false, "Google Maps lancé en plein écran");
        } catch (Throwable e) {
            return new LaunchResult(false, false, "Impossible d’ouvrir Google Maps");
        }
    }

    private static boolean requestFreeformWindowing(ActivityOptions options) {
        // API système cachée, utile sur Android 10 / firmwares Topway et certains
        // appareils Samsung. Si elle est refusée, le setLaunchBounds public reste
        // quand même présent et peut être honoré par le gestionnaire multi-fenêtre.
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
