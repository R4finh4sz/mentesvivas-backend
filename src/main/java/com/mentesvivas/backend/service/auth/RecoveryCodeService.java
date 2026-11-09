package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.cache.TemporaryCounter;
import com.mentesvivas.backend.cache.TemporaryValue;
import com.mentesvivas.backend.config.PasswordRecoveryProperties;
import com.mentesvivas.backend.exception.auth.InvalidRecoveryCodeException;
import com.mentesvivas.backend.security.auth.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecoveryCodeService {

    private static final String CODE_PREFIX = "recovery:code:";
    private static final String ATTEMPTS_PREFIX = "recovery:attempts:";
    private static final String SENDS_PREFIX = "recovery:sends:";

    private final SecureRandom secureRandom = new SecureRandom();
    private final TemporaryCounter counter;
    private final TemporaryValue values;
    private final PasswordRecoveryProperties properties;
    private final TokenHasher hasher;

    public Optional<String> generate(String email) {
        String key = hasher.hash(email);
        long sends = counter.incrementar(SENDS_PREFIX + key, properties.sendsWindow());
        if (sends > properties.maxSends()) {
            return Optional.empty();
        }
        String code = "%06d".formatted(secureRandom.nextInt(1_000_000));
        values.store(CODE_PREFIX + key, codeHash(key, code), properties.codeValidity());
        counter.remove(ATTEMPTS_PREFIX + key);
        return Optional.of(code);
    }

    public void validateAndConsume(String email, String code) {
        String key = hasher.hash(email);
        long attempts = counter.incrementar(ATTEMPTS_PREFIX + key, properties.codeValidity());
        if (attempts > properties.maxAttempts()) {
            values.remove(CODE_PREFIX + key);
            throw new InvalidRecoveryCodeException();
        }
        String expected = values.get(CODE_PREFIX + key).orElseThrow(InvalidRecoveryCodeException::new);
        boolean matches = MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                codeHash(key, code).getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            throw new InvalidRecoveryCodeException();
        }
        values.remove(CODE_PREFIX + key);
        counter.remove(ATTEMPTS_PREFIX + key);
    }

    private String codeHash(String key, String code) {
        return hasher.hash(key + ":" + code);
    }
}
