package ch.piiwii.suchard;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class FinishStage extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent intent){
  int index=intent.getIntExtra("id",-1);
  if(index<0||index>=Route.ALL.length)return;
  Notify.markDone(c,index,true);
 }
}
