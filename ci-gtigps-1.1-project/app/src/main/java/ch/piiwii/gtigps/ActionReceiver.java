package ch.piiwii.gtigps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ActionReceiver extends BroadcastReceiver {
    public static final String ACTION_SHOW = "ch.piiwii.gtigps.SHOW_MAP";
    public static final String ACTION_HIDE = "ch.piiwii.gtigps.HIDE_MAP";
    public static final String ACTION_REFRESH = "ch.piiwii.gtigps.REFRESH_MAP";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();
        Intent service = new Intent(context, MapPanelService.class);
        service.setAction(action);
        if (ACTION_HIDE.equals(action)) {
            context.stopService(service);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(service);
        } else {
            context.startService(service);
        }
    }
}
