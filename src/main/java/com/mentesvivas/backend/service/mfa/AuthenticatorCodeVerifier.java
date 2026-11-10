package com.mentesvivas.backend.service.mfa;

import com.mentesvivas.backend.cache.TemporaryCounter;
import com.mentesvivas.backend.cache.TemporaryValue;
import com.mentesvivas.backend.config.TwoFactorProperties;
import com.mentesvivas.backend.exception.auth.InvalidCodeException;
import com.mentesvivas.backend.security.auth.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.OptionalLong;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthenticatorCodeVerifier {

    private static final Duration USED_CODE_MEMORY = Duration.ofMinutes(2);

    private final TotpService totpService;
    private final TemporaryCounter counter;
    private final TemporaryValue values;
    private final TokenHasher hasher;
    private final TwoFactorProperties properties;

    public void verify(UUID userId, String secret, String code) {
        String key = hasher.hash(userId.toString());
        String attemptsKey = "totp:attempts:" + key;
        if (counter.increment(attemptsKey, properties.codeValidity()) > properties.maxAttempts()) {
            throw new InvalidCodeException();
        }
        OptionalLong step = totpService.matchingStep(secret, code, Instant.now());
        if (step.isEmpty()) {
            throw new InvalidCodeException();
        }
        String usedKey = "totp:used:" + key + ":" + step.getAsLong();
        if (values.get(usedKey).isPresent()) {
            throw new InvalidCodeException();
        }
        values.store(usedKey, "1", USED_CODE_MEMORY);
        counter.remove(attemptsKey);
    }
}
