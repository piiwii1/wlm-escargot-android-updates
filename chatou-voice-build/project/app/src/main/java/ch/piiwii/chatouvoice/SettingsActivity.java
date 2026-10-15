package ch.piiwii.chatouvoice;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        SharedPreferences p=getSharedPreferences("chatou",MODE_PRIVATE);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(42,48,42,32); root.setBackgroundColor(Color.rgb(245,247,250));
        TextView t=new TextView(this); t.setText("Paramètres"); t.setTextSize(28); root.addView(t);

        EditText wake=field("Mot de réveil", p.getString("wake_word","ok chatou")); root.addView(wake);
        EditText model=field("Modèle OpenAI", p.getString("model","gpt-5.6-luna")); root.addView(model);
        EditText key=field("Clé API OpenAI", SecurePrefs.getApiKey(this)); key.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD); root.addView(key);
        CheckBox auto=new CheckBox(this); auto.setText("Réactiver automatiquement après redémarrage"); auto.setChecked(p.getBoolean("auto_start",true)); root.addView(auto);
        CheckBox sound=new CheckBox(this); sound.setText("Réponse vocale automatique"); sound.setChecked(p.getBoolean("speak",true)); root.addView(sound);
        TextView note=new TextView(this); note.setText("La clé API est chiffrée avec Android Keystore. Elle n'est pas intégrée dans l'APK. L'utilisation de l'API OpenAI est distincte de l'abonnement ChatGPT."); note.setPadding(0,20,0,20); root.addView(note);
        Button save=new Button(this); save.setText("Enregistrer"); root.addView(save);
        save.setOnClickListener(v->{
            try { SecurePrefs.saveApiKey(this,key.getText().toString().trim()); }
            catch(Exception e){ Toast.makeText(this,"Impossible de sécuriser la clé",Toast.LENGTH_LONG).show(); return; }
            p.edit().putString("wake_word",wake.getText().toString().trim().toLowerCase())
                    .putString("model",model.getText().toString().trim())
                    .putBoolean("auto_start",auto.isChecked()).putBoolean("speak",sound.isChecked()).apply();
            Toast.makeText(this,"Paramètres enregistrés",Toast.LENGTH_SHORT).show(); finish();
        });
        setContentView(root);
    }
    private EditText field(String hint,String value){ EditText e=new EditText(this); e.setHint(hint); e.setText(value); e.setSingleLine(true); e.setPadding(12,18,12,18); return e; }
}
