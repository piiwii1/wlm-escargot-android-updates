package ch.piiwii.suchard;
import android.content.*;
public final class Boot extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){Notify.schedule(c);}
}
