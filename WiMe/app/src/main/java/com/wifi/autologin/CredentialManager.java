package com.wifi.autologin;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class CredentialManager {
    private static final String PREFS_NAME = "SecurePrefs";
    private static final String KEY_ALIAS = "WifiLoginKey";
    private static final String ENCRYPTED_USERNAME = "encrypted_username";
    private static final String ENCRYPTED_PASSWORD = "encrypted_password";
    
    private SharedPreferences prefs;
    private KeyStore keyStore;

    public CredentialManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveCredentials(String username, String password) {
        try {
            SecretKey secretKey = getOrCreateSecretKey();
            String encryptedUsername = encryptData(username, secretKey);
            String encryptedPassword = encryptData(password, secretKey);
            
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(ENCRYPTED_USERNAME, encryptedUsername);
            editor.putString(ENCRYPTED_PASSWORD, encryptedPassword);
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String[] getCredentials() {
        try {
            String encryptedUsername = prefs.getString(ENCRYPTED_USERNAME, null);
            String encryptedPassword = prefs.getString(ENCRYPTED_PASSWORD, null);
            
            if (encryptedUsername == null || encryptedPassword == null) {
                return new String[]{"", ""};
            }
            
            SecretKey secretKey = getOrCreateSecretKey();
            String username = decryptData(encryptedUsername, secretKey);
            String password = decryptData(encryptedPassword, secretKey);
            
            return new String[]{username, password};
        } catch (Exception e) {
            e.printStackTrace();
            return new String[]{"", ""};
        }
    }

    private SecretKey getOrCreateSecretKey() throws Exception {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            
            KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build();
                
            keyGenerator.init(keyGenParameterSpec);
            return keyGenerator.generateKey();
        } else {
            return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
        }
    }

    private String encryptData(String data, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        
        byte[] encryptedData = cipher.doFinal(data.getBytes());
        byte[] iv = cipher.getIV();
        
        // Combine IV and encrypted data
        byte[] result = new byte[4 + iv.length + encryptedData.length];
        System.arraycopy(intToByteArray(iv.length), 0, result, 0, 4);
        System.arraycopy(iv, 0, result, 4, iv.length);
        System.arraycopy(encryptedData, 0, result, 4 + iv.length, encryptedData.length);
        
        return android.util.Base64.encodeToString(result, android.util.Base64.DEFAULT);
    }

    private String decryptData(String encryptedData, SecretKey secretKey) throws Exception {
        byte[] data = android.util.Base64.decode(encryptedData, android.util.Base64.DEFAULT);
        
        int ivLength = byteArrayToInt(java.util.Arrays.copyOfRange(data, 0, 4));
        byte[] iv = java.util.Arrays.copyOfRange(data, 4, 4 + ivLength);
        byte[] encryptedBytes = java.util.Arrays.copyOfRange(data, 4 + ivLength, data.length);
        
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
        
        byte[] decryptedData = cipher.doFinal(encryptedBytes);
        return new String(decryptedData);
    }

    private byte[] intToByteArray(int value) {
        return new byte[]{
            (byte) (value >>> 24),
            (byte) (value >>> 16),
            (byte) (value >>> 8),
            (byte) value
        };
    }

    private int byteArrayToInt(byte[] bytes) {
        return (bytes[0] << 24) & 0xFF0000 |
               (bytes[1] << 16) & 0x00FF0000 |
               (bytes[2] << 8)  & 0x0000FF00 |
               (bytes[3] << 0)  & 0x000000FF;
    }
}
