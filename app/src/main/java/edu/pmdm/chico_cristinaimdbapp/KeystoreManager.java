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
            //obtiene una instancia
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

    //metodo para cifrar
    public String encrypt(String plainText) {
        try {
            // Obtiene la clave almacenada en el Keystore
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            Key key = keyStore.getKey(KEY_ALIAS, null);

            // Configura el cifrador con AES/GCM/NoPadding
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, (SecretKey) key); // Inicia el cifrador en modo ENCRIPTAR

            byte[] iv = cipher.getIV(); // Obtiene el IV generado automáticamente
            byte[] encrypted = cipher.doFinal(plainText.getBytes()); // Cifra el texto

            // Combina el IV y el texto cifrado en un solo array de bytes
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length); // Copia el IV al principio del array
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length); // Agrega el texto cifrado

            return Base64.encodeToString(combined, Base64.DEFAULT); // Retorna el resultado en Base64
        } catch (Exception e) {
            e.printStackTrace(); // Captura errores si ocurren
        }
        return null;
    }

    //metodo para descifrar
    public String decrypt(String encryptedText) {
        try {
            // Obtiene la clave almacenada en el Keystore
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            Key key = keyStore.getKey(KEY_ALIAS, null);

            // Decodifica el texto cifrado de Base64 a un array de bytes
            byte[] combined = Base64.decode(encryptedText, Base64.DEFAULT);
            byte[] iv = new byte[12]; // El IV tiene 12 bytes en GCM
            byte[] encrypted = new byte[combined.length - 12]; // El resto es el texto cifrado

            // Separa el IV del texto cifrado
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);

            // Configura el cifrador con AES/GCM/NoPadding y el IV obtenido
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, (SecretKey) key, new GCMParameterSpec(128, iv));

            return new String(cipher.doFinal(encrypted)); // Retorna el texto descifrado
        } catch (Exception e) {
            e.printStackTrace(); // Captura errores si ocurren
        }
        return null;
    }
}
