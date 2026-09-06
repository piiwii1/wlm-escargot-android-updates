package ch.piiwii.gtigps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        boolean resume = GpsState.prefs(context).getBoolean(GpsState.KEY_AUTO_RESUME, false);
        if (!resume) return;
        Intent service = new Intent(context, MapPanelService.class);
        service.setAction(ActionReceiver.ACTION_SHOW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(service);
        else context.startService(service);
    }
}
