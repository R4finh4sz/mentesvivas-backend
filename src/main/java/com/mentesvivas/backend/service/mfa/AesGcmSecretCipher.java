package com.mentesvivas.backend.service.mfa;

import com.mentesvivas.backend.exception.config.InvalidConfigurationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AesGcmSecretCipher implements SecretCipher {

    private static final int KEY_BYTES = 32;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecureRandom secureRandom = new SecureRandom();
    private final String encodedKey;

    public AesGcmSecretCipher(@Value("${app.mfa.encryption-key:}") String encodedKey) {
        this.encodedKey = encodedKey;
    }

    @Override
    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv).put(encrypted).array());
        } catch (GeneralSecurityException e) {
            throw new InvalidConfigurationException("Could not encrypt the secret", e);
        }
    }

    @Override
    public String decrypt(String encrypted) {
        try {
            byte[] data = Base64.getDecoder().decode(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, data, 0, IV_BYTES));
            return new String(cipher.doFinal(data, IV_BYTES, data.length - IV_BYTES), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new InvalidConfigurationException("Could not decrypt the secret", e);
        }
    }

    private SecretKeySpec key() {
        if (encodedKey == null || encodedKey.isBlank()) {
            throw new InvalidConfigurationException("MFA_ENCRYPTION_KEY is not configured");
        }
        byte[] key;
        try {
            key = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException e) {
            throw new InvalidConfigurationException("MFA_ENCRYPTION_KEY must be Base64", e);
        }
        if (key.length != KEY_BYTES) {
            throw new InvalidConfigurationException("MFA_ENCRYPTION_KEY must have 32 bytes");
        }
        return new SecretKeySpec(key, "AES");
    }
}
