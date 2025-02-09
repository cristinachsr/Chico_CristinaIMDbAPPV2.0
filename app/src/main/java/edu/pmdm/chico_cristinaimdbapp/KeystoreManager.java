package edu.pmdm.chico_cristinaimdbapp;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.security.KeyStore;
import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import android.util.Base64;

public class KeystoreManager {
    private static final String KEY_ALIAS = "MyAppKeyAlias";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";

    private Context context;

    // Constructor que acepta el contexto
    public KeystoreManager(Context context) {
        this.context = context;

        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
                keyGenerator.init(new KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build());
                keyGenerator.generateKey();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String encrypt(String plainText) {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            Key key = keyStore.getKey(KEY_ALIAS, null);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, (SecretKey) key);

            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(plainText.getBytes());

            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.encodeToString(combined, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String decrypt(String encryptedText) {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            Key key = keyStore.getKey(KEY_ALIAS, null);

            byte[] combined = Base64.decode(encryptedText, Base64.DEFAULT);
            byte[] iv = new byte[12];
            byte[] encrypted = new byte[combined.length - 12];

            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, (SecretKey) key, new GCMParameterSpec(128, iv));

            return new String(cipher.doFinal(encrypted));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
