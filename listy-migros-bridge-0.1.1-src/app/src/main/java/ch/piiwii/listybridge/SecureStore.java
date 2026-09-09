package ch.piiwii.listybridge;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class SecureStore {
    private static final String ALIAS = "listy_bridge_store";
    private static final String PREFS = "listy_bridge_secure";
    private SecureStore() {}

    private static SecretKey key() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        if (!ks.containsAlias(ALIAS)) {
            KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            kg.init(new KeyGenParameterSpec.Builder(ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build());
            kg.generateKey();
        }
        return ((KeyStore.SecretKeyEntry) ks.getEntry(ALIAS, null)).getSecretKey();
    }

    public static void put(Context c, String name, String value) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key());
            byte[] iv = cipher.getIV();
            byte[] enc = cipher.doFinal((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[1 + iv.length + enc.length];
            out[0] = (byte) iv.length;
            System.arraycopy(iv, 0, out, 1, iv.length);
            System.arraycopy(enc, 0, out, 1 + iv.length, enc.length);
            c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(name, Base64.encodeToString(out, Base64.NO_WRAP)).apply();
        } catch (Exception e) {
            throw new RuntimeException("SecureStore write failed", e);
        }
    }

    public static String get(Context c, String name) {
        String raw = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(name, "");
        if (raw == null || raw.isEmpty()) return "";
        try {
            byte[] all = Base64.decode(raw, Base64.NO_WRAP);
            int ivLen = all[0] & 0xff;
            byte[] iv = new byte[ivLen];
            byte[] enc = new byte[all.length - 1 - ivLen];
            System.arraycopy(all, 1, iv, 0, ivLen);
            System.arraycopy(all, 1 + ivLen, enc, 0, enc.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(enc), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    public static void remove(Context c, String name) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(name).apply();
    }

    public static void putPlain(Context c, String name, String value) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("plain_" + name, value).apply();
    }

    public static String getPlain(Context c, String name) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("plain_" + name, "");
    }
}
