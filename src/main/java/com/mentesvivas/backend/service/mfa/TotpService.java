package com.mentesvivas.backend.service.mfa;

import com.mentesvivas.backend.exception.config.InvalidConfigurationException;
import com.mentesvivas.backend.util.Base32;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.OptionalLong;

@Component
public class TotpService {

    private static final int SECRET_BYTES = 20;
    private static final int STEP_SECONDS = 30;
    private static final int DIGITS = 6;
    private static final int ALLOWED_DRIFT_STEPS = 1;

    private final SecureRandom secureRandom = new SecureRandom();
    private final String issuer;

    public TotpService(@Value("${app.mfa.issuer:MentesVivas}") String issuer) {
        this.issuer = issuer;
    }

    public String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        secureRandom.nextBytes(bytes);
        return Base32.encode(bytes);
    }

    public String provisioningUri(String accountEmail, String secret) {
        String label = encode(issuer) + ":" + encode(accountEmail);
        return "otpauth://totp/%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d"
                .formatted(label, secret, encode(issuer), DIGITS, STEP_SECONDS);
    }

    public String codeAt(String secret, Instant instant) {
        return code(secret, stepOf(instant));
    }

    public OptionalLong matchingStep(String secret, String code, Instant now) {
        long current = stepOf(now);
        for (long step = current - ALLOWED_DRIFT_STEPS; step <= current + ALLOWED_DRIFT_STEPS; step++) {
            if (constantTimeEquals(code(secret, step), code)) {
                return OptionalLong.of(step);
            }
        }
        return OptionalLong.empty();
    }

    private long stepOf(Instant instant) {
        return instant.getEpochSecond() / STEP_SECONDS;
    }

    private String code(String secret, long step) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(Base32.decode(secret), "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24) | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8) | (hash[offset + 3] & 0xFF);
            return "%06d".formatted(binary % 1_000_000);
        } catch (GeneralSecurityException e) {
            throw new InvalidConfigurationException("HmacSHA1 unavailable on this JVM", e);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return java.security.MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
