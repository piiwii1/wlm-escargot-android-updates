package ch.piiwii.suchard;
import android.app.Activity;import android.content.*;import android.net.Uri;import android.os.Bundle;import android.widget.Toast;import java.net.URLEncoder;
public final class Gps extends Activity {
 public static void start(Context c,int id){
   if(id<0||id>=Route.ALL.length)return;
   Route.Stop s=Route.ALL[id];if(s.destination.isEmpty())return;
   try{
     String q=URLEncoder.encode(s.destination,"UTF-8");
     Intent nav=new Intent(Intent.ACTION_VIEW,Uri.parse("google.navigation:q="+q+"&mode="+(s.mode.equals("walking")?"w":"d")));
     nav.setPackage("com.google.android.apps.maps");nav.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);c.startActivity(nav);
   }catch(Exception e){
     try{
       String q=URLEncoder.encode(s.destination,"UTF-8");
       Intent web=new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/maps/dir/?api=1&destination="+q+"&travelmode="+s.mode));
       web.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);c.startActivity(web);
     }catch(Exception failed){Toast.makeText(c,"Aucun GPS disponible",Toast.LENGTH_LONG).show();}
   }
 }
 @Override protected void onCreate(Bundle b){super.onCreate(b);start(this,getIntent().getIntExtra("id",-1));finish();}
}
