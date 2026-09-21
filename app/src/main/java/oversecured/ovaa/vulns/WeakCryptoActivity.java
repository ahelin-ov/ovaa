package oversecured.ovaa.vulns;

import android.app.Activity;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class WeakCryptoActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String seed = getIntent().getStringExtra("seed");

        keyFromPredictableRandom();
        keyFromAttackerData(seed);
        weakAlgorithms(seed);
        weakHashes(seed);
        weakKeyStoreKey();
        seededSecureRandom(seed);
        authenticateWithoutCryptoObject();
        finish();
    }

    private void keyFromPredictableRandom() {
        Random random = new Random(System.currentTimeMillis());
        byte[] key = new byte[16];
        random.nextBytes(key);
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new IvParameterSpec(new byte[16]));
            cipher.doFinal("secret".getBytes());
        } catch (Exception ignored) {
        }
    }

    private void keyFromAttackerData(String seed) {
        if (seed == null) {
            return;
        }
        try {
            byte[] key = MessageDigest.getInstance("MD5").digest(seed.getBytes());
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
            cipher.doFinal("secret".getBytes());
        } catch (Exception ignored) {
        }
    }

    private void weakAlgorithms(String data) {
        try {
            Cipher des = Cipher.getInstance("DES/ECB/PKCS5Padding");
            des.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(new byte[8], "DES"));
            des.doFinal(String.valueOf(data).getBytes());

            Cipher rc4 = Cipher.getInstance("RC4");
            rc4.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(new byte[16], "RC4"));
            rc4.doFinal(String.valueOf(data).getBytes());

            Cipher ecb = Cipher.getInstance("AES/ECB/NoPadding");
            ecb.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(new byte[16], "AES"));
            ecb.doFinal(new byte[16]);
        } catch (Exception ignored) {
        }
    }

    private void weakHashes(String data) {
        try {
            MessageDigest.getInstance("MD5").digest(String.valueOf(data).getBytes());
            MessageDigest.getInstance("SHA-1").digest(String.valueOf(data).getBytes());
        } catch (Exception ignored) {
        }
    }

    private void weakKeyStoreKey() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder("ovaa_key",
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(128)
                    .setRandomizedEncryptionRequired(false)
                    .setUserAuthenticationRequired(false)
                    .build());
            SecretKey key = generator.generateKey();
            Base64.encodeToString(key.getEncoded() == null ? new byte[0] : key.getEncoded(), 0);
        } catch (Exception ignored) {
        }
    }

    private void seededSecureRandom(String seed) {
        SecureRandom random = new SecureRandom();
        if (seed != null) {
            random.setSeed(seed.getBytes());
        }
        byte[] token = new byte[16];
        random.nextBytes(token);
    }

    private void authenticateWithoutCryptoObject() {
        BiometricPrompt prompt = new BiometricPrompt.Builder(this)
                .setTitle("Confirm")
                .setNegativeButton("Cancel", getMainExecutor(), (dialog, which) -> { })
                .build();
        prompt.authenticate(new CancellationSignal(), getMainExecutor(),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        startActivity(new android.content.Intent(WeakCryptoActivity.this,
                                oversecured.ovaa.activities.MainActivity.class));
                    }
                });
    }
}
