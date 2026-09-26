package ch.piiwii.chatouvoice;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQ = 44;
    private TextView status;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(buildUi());
        requestNeededPermissions();
        refresh();
    }

    @Override protected void onResume() { super.onResume(); refresh(); }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 54, 48, 36);
        root.setBackgroundColor(Color.rgb(245,247,250));

        TextView title = new TextView(this); title.setText("Chatou Voice"); title.setTextSize(30); title.setTextColor(Color.rgb(18,20,23));
        TextView sub = new TextView(this); sub.setText("Assistant vocal personnel Android"); sub.setTextSize(16); sub.setPadding(0,6,0,30);
        status = new TextView(this); status.setTextSize(18); status.setPadding(24,24,24,24); status.setBackgroundColor(Color.WHITE);

        Button start = btn("Activer l'écoute « OK Chatou »");
        start.setOnClickListener(v -> startWakeService());
        Button stop = btn("Désactiver l'écoute");
        stop.setOnClickListener(v -> { stopService(new Intent(this, WakeWordService.class)); Toast.makeText(this,"Écoute arrêtée",Toast.LENGTH_SHORT).show(); refresh(); });
        Button talk = btn("Parler maintenant");
        talk.setOnClickListener(v -> startActivity(new Intent(this, VoiceConversationActivity.class)));
        Button settings = btn("Paramètres et clé API");
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        Button open = btn("Ouvrir l'application ChatGPT");
        open.setOnClickListener(v -> openChatGPT());
        Button battery = btn("Autoriser en arrière-plan");
        battery.setOnClickListener(v -> { try { startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:"+getPackageName()))); } catch(Exception e){ startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:"+getPackageName()))); } });

        root.addView(title); root.addView(sub); root.addView(status);
        root.addView(start); root.addView(stop); root.addView(talk); root.addView(settings); root.addView(open); root.addView(battery);
        return root;
    }

    private Button btn(String s) { Button b = new Button(this); b.setText(s); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.topMargin=18; b.setLayoutParams(p); return b; }

    private void requestNeededPermissions() {
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS}, REQ);
        else requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ);
    }

    private void startWakeService() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { requestNeededPermissions(); return; }
        Intent i = new Intent(this, WakeWordService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        Toast.makeText(this,"Écoute activée",Toast.LENGTH_SHORT).show();
        status.postDelayed(this::refresh, 500);
    }

    private void refresh() {
        String wake = getSharedPreferences("chatou",MODE_PRIVATE).getString("wake_word","ok chatou");
        String api = SecurePrefs.getApiKey(this).isEmpty() ? "non configurée" : "configurée";
        status.setText("● Assistant prêt\n\nMot de réveil : « "+wake+" »\nClé OpenAI : "+api+"\nVersion : 0.1.0 (1)");
    }

    private void openChatGPT() {
        Intent i = getPackageManager().getLaunchIntentForPackage("com.openai.chatgpt");
        if (i != null) startActivity(i); else {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://chatgpt.com"))); } catch(Exception ignored) {}
        }
    }
}
