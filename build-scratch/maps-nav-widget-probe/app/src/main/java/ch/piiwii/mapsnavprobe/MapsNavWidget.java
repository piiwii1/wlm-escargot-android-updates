package ch.piiwii.mapsnavprobe;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.widget.RemoteViews;

public class MapsNavWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) { for(int id:ids) updateOne(context,manager,id); }
    public static void updateAll(Context context){ AppWidgetManager m=AppWidgetManager.getInstance(context); int[] ids=m.getAppWidgetIds(new ComponentName(context,MapsNavWidget.class)); for(int id:ids) updateOne(context,m,id); }
    private static void updateOne(Context context,AppWidgetManager manager,int id){
        SharedPreferences p=context.getSharedPreferences(MapsNotificationListener.PREFS,Context.MODE_PRIVATE);
        String primary=p.getString("primary",""); String secondary=p.getString("secondary",""); String stamp=p.getString("timestamp",""); boolean simulated=p.getBoolean("simulated",false);
        if(TextUtils.isEmpty(primary)) primary="Aucune consigne Google Maps";
        String detail=TextUtils.isEmpty(secondary)?"Lance un trajet dans Google Maps":secondary;
        if(simulated) detail="TEST · "+detail; if(!TextUtils.isEmpty(stamp)) detail+="  ·  "+stamp;
        RemoteViews views=new RemoteViews(context.getPackageName(),R.layout.widget_navigation); views.setTextViewText(R.id.widget_instruction,primary); views.setTextViewText(R.id.widget_detail,detail);
        Intent open=new Intent(context,MainActivity.class); PendingIntent pi=PendingIntent.getActivity(context,100,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE); views.setOnClickPendingIntent(R.id.widget_root,pi); manager.updateAppWidget(id,views);
    }
}
