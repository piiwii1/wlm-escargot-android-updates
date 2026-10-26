package ch.piiwii.notificationwidget;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

/** Opens the original action if possible and falls back to the originating app. */
public final class WidgetClickActivity extends Activity {
  private static final String TARGET="target_package",KEY="notification_key",
    PENDING="original_pending_intent",CATEGORY="category_id";
  static PendingIntent create(Context c,String category,String pkg,String key,PendingIntent action){
    Intent i=new Intent(c,WidgetClickActivity.class);
    i.putExtra(TARGET,pkg==null?"":pkg);
    i.putExtra(KEY,key==null?"":key);
    i.putExtra(CATEGORY,category==null?"":category);
    if(action!=null)i.putExtra(PENDING,action);
    String identity=(category==null?"":category)+":"+(pkg==null?"":pkg)+":"+(key==null?"":key);
    i.setData(Uri.parse("piiwii-widget://tap/"+Uri.encode(identity)));
    i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION|Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
    return PendingIntent.getActivity(c,identity.hashCode(),i,
      PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  }
  @Override protected void onCreate(Bundle state){
    super.onCreate(state);
    try{route(getIntent());}finally{finish();overridePendingTransition(0,0);}
  }
  private PendingIntent originalFrom(Intent i){
    try{
      if(Build.VERSION.SDK_INT>=33)return i.getParcelableExtra(PENDING,PendingIntent.class);
      return i.getParcelableExtra(PENDING);
    }catch(RuntimeException ignored){return null;}
  }
  private void route(Intent source){
    if(source==null){openSettings();return;}
    String pkg=source.getStringExtra(TARGET);
    String key=source.getStringExtra(KEY);
    PendingIntent pending=(key==null||key.isEmpty())?null:NotificationMonitor.intentForKey(key);
    if(pending==null)pending=originalFrom(source);
    if(ClickPolicy.NOTIFICATION.equals(ClickPolicy.choose(pending!=null,pkg!=null&&!pkg.isEmpty()))){
      try{
        if(Build.VERSION.SDK_INT>=34){
          ActivityOptions options=ActivityOptions.makeBasic();
          options.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
          pending.send(this,0,null,null,null,null,options.toBundle());
        }else pending.send();
        record("notification");
        return;
      }catch(PendingIntent.CanceledException | RuntimeException ignored){}
    }
    if(ClickPolicy.APPLICATION.equals(ClickPolicy.afterCancelled(pkg!=null&&!pkg.isEmpty()))){
      try{
        Intent app=getPackageManager().getLaunchIntentForPackage(pkg);
        if(app!=null){
          app.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
          startActivity(app);
          record("application");
          return;
        }
      }catch(RuntimeException ignored){}
    }
    openSettings();
  }
  private void openSettings(){
    try{startActivity(new Intent(this,MainActivity.class)
      .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));}
    catch(RuntimeException ignored){}
    record("settings");
  }
  private void record(String outcome){
    android.content.SharedPreferences p=Categories.prefs(this);
    p.edit().putLong("widget_taps",p.getLong("widget_taps",0)+1)
      .putString("last_tap_result",outcome)
      .putLong("last_tap_time",System.currentTimeMillis()).apply();
  }
  static String diagnostic(Context c){
    android.content.SharedPreferences p=Categories.prefs(c);
    String outcome=p.getString("last_tap_result","");
    String label="notification".equals(outcome)?"Notification ouverte":
      ("application".equals(outcome)?"Application ouverte":
      ("settings".equals(outcome)?"Réglages ouverts":"Aucun essai"));
    return "Touchers du widget : "+p.getLong("widget_taps",0)+
      "\nDernier résultat : "+label+
      "\nAucun texte de notification n'est enregistré.";
  }
}