package ch.piiwii.waarchive;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Entrée 0.4.5 : intercepte Partager/Ouvrir avec avant la couche 0.4.3. */
public class MainActivityV045Entry extends MainActivityV045 {
    @Override
    protected void onCreate(Bundle state) {
        Uri incoming = extractIncoming(getIntent());
        if (incoming != null) {
            blockAutomaticRecovery(true);
            setIntent(new Intent());
        }
        super.onCreate(state);
        if (incoming != null) {
            blockAutomaticRecovery(false);
            startMediaImport(incoming);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Uri incoming = extractIncoming(intent);
        setIntent(new Intent());
        super.onNewIntent(new Intent());
        if (incoming != null) startMediaImport(incoming);
    }

    private Uri extractIncoming(Intent intent) {
        if (intent == null) return null;
        try {
            if (Intent.ACTION_SEND.equals(intent.getAction())) {
                Object value = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (value instanceof Uri) return (Uri) value;
            }
            if (Intent.ACTION_VIEW.equals(intent.getAction())) return intent.getData();
        } catch (Exception ignored) {
        }
        return null;
    }

    private void blockAutomaticRecovery(boolean value) {
        try {
            Field f = MainActivityV045.class.getDeclaredField("extracting");
            f.setAccessible(true);
            f.setBoolean(this, value);
        } catch (Exception ignored) {
        }
    }

    private void startMediaImport(Uri uri) {
        try {
            Method m = MainActivityV045.class.getDeclaredMethod("importWithMedia", Uri.class, String.class);
            m.setAccessible(true);
            m.invoke(this, uri, "Export WhatsApp");
        } catch (Exception e) {
            android.widget.Toast.makeText(this, "Impossible d’ouvrir le fichier partagé.", android.widget.Toast.LENGTH_LONG).show();
        }
    }
}
