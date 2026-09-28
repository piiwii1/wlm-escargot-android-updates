package ch.piiwii.chatouvoice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i){
        if(!c.getSharedPreferences("chatou",Context.MODE_PRIVATE).getBoolean("auto_start",true)) return;
        try { Intent s=new Intent(c,WakeWordService.class); if(Build.VERSION.SDK_INT>=26)c.startForegroundService(s); else c.startService(s); }
        catch(Exception ignored) {}
    }
}
