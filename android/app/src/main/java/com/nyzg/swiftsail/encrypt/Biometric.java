package com.nyzg.swiftsail.encrypt;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.IpSecManager;
import android.provider.Settings;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;

import androidx.biometric.BiometricManager;

import com.nyzg.swiftsail.bean.GlobalToast;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class Biometric {
    private volatile static String deviceId;

    public static String getLastData(Cipher cipher, String PREFS_NAME, String KEY_NAME, Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE
        );
        String data = sharedPreferences.getString(KEY_NAME, null);
        if (data == null || !data.contains(":")) {
            return null;
        }
        String[] parts = data.split(":", 2);
        byte[] iv = Base64.getDecoder().decode(parts[0]);
        byte[] encryptData = Base64.getDecoder().decode(parts[1]);

        SecretKey secretKey = Biometric.getSecretKey(KEY_NAME);
        if (secretKey == null) {
            return null;
        }
        try {
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            byte[] decryptData = cipher.doFinal(encryptData);
            return new String(decryptData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean putData(String plainText, String PREFS_NAME, String KEY_NAME, Context context) {
        try {
            SecretKey secretKey = getSecretKey(KEY_NAME);
            if (secretKey == null) {
                GlobalToast.COMMON_TOAST.accept("此设备不存在生物密钥");
                return false;
            }

            Cipher cipher = getCipher();
            if (cipher == null) {
                return false;
            }
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            String combined = Base64.getEncoder().encodeToString(iv) + ":" +
                    Base64.getEncoder().encodeToString(encrypted);

            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_NAME, combined).apply();
            return true;
        } catch (Exception e) {
            GlobalToast.COMMON_TOAST.accept("保存加密数据失败：" + e.getMessage());
            return false;
        }
    }

    public static boolean generateSecretKey(KeyGenParameterSpec keyGenParameterSpec) {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            keyGenerator.init(keyGenParameterSpec);
            keyGenerator.generateKey();
            return true;
        } catch (Exception e) {
            GlobalToast.COMMON_TOAST.accept("您的设备似乎对加密的支持不太够？");
            return false;
        }
    }

    public static SecretKey getSecretKey(String KEY_NAME) {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");

            // Before the keystore can be accessed, it must be loaded.
            keyStore.load(null);
            return ((SecretKey) keyStore.getKey(KEY_NAME, null));
        } catch (Exception e) {
            return null;
        }
    }

    public static Cipher getCipher() {
        try {
            return Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getDeviceIdHash(Context context) {
        if (deviceId != null) {
            return deviceId;
        }
        synchronized (Biometric.class) {
            if (deviceId != null) {
                return deviceId;
            }
            try {
                @SuppressLint("HardwareIds") String androidId = Settings.Secure.getString(
                        context.getContentResolver(), Settings.Secure.ANDROID_ID
                );
                // 防御性检查：某些模拟器或异常设备可能返回 null 或全零
                if (TextUtils.isEmpty(androidId)) {
                    return null; // 无效 ID
                }
                deviceId = hash(androidId);
                return deviceId;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private static String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.substring(0, 32); //取前32位足够唯一
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported", e);
        }
    }

    /**
     * 检查是否支持生物识别
     */
    public static boolean isBiometricAvailable(Context context) {
        BiometricManager biometricManager = BiometricManager.from(context);
        switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                return true;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                GlobalToast.COMMON_TOAST.accept("您的硬件当前不可用");
            default:
                return false;
        }
    }
}
