package ch.piiwii.gtigps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        GTIGpsWidgetProvider.updateAll(context);
        if (!WindowPrefs.autoBoot(context)) return;
        new Handler(Looper.getMainLooper()).postDelayed(() -> GoogleMapsWindow.launchWindow(context), 4500L);
    }
}
