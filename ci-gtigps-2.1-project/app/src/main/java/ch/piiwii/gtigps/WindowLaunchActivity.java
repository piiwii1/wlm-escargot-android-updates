package ch.piiwii.gtigps;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;

/**
 * Trampoline transparent utilisé par le widget.
 *
 * Le point important de 2.1.0 : le widget ne démarre plus Google Maps depuis un
 * BroadcastReceiver. Il ouvre d'abord cette minuscule Activity transparente, puis
 * celle-ci demande la fenêtre Google Maps. Ainsi, le système reçoit exactement le
 * même type de lancement que lorsque le test fonctionne depuis l'application.
 */
public class WindowLaunchActivity extends Activity {
    private boolean started;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setDimAmount(0f);
        new Handler(Looper.getMainLooper()).post(this::launch);
    }

    private void launch() {
        if (started) return;
        started = true;
        GoogleMapsWindow.launchWindow(this);

        // Le trampoline disparaît : si le fenêtrage est accepté, le Launcher / Home
        // redevient immédiatement le fond sous la vraie fenêtre Google Maps.
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                finish();
                overridePendingTransition(0, 0);
            } catch (Throwable ignored) {}
        }, 350L);
    }
}
