package ch.piiwii.foirevalais;

import android.app.*;
import android.content.*;
import android.os.Build;

public class EventReceiver extends BroadcastReceiver {
    public static final String CHANNEL = "foire_events";
    @Override public void onReceive(Context context, Intent intent) {
        NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT>=26) nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Mes événements",NotificationManager.IMPORTANCE_HIGH));
        String title=intent.getStringExtra("title"); String sub=intent.getStringExtra("sub");
        Intent open=new Intent(context,MainActivity.class);
        PendingIntent pi=PendingIntent.getActivity(context,0,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,CHANNEL):new Notification.Builder(context);
        b.setSmallIcon(ch.piiwii.foirevalais.R.drawable.ic_launcher).setContentTitle("Bientôt : "+title).setContentText(sub).setAutoCancel(true).setContentIntent(pi);
        nm.notify((title+sub).hashCode(),b.build());
    }
}
