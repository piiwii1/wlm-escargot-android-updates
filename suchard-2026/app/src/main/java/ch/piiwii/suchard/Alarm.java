package ch.piiwii.suchard;
import android.content.*;
public final class Alarm extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){
   if(c.getSharedPreferences("suchard",Context.MODE_PRIVATE).getBoolean("on",true))
     Notify.show(c,i.getIntExtra("id",-1),false);
 }
}
