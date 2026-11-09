package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.cache.TemporaryCounter;
import com.mentesvivas.backend.cache.TemporaryValue;
import com.mentesvivas.backend.config.LoginLockoutProperties;
import com.mentesvivas.backend.exception.auth.AccountLockedException;
import com.mentesvivas.backend.security.auth.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginLockoutService {

    private static final String FAILURES_PREFIX = "login:failures:";
    private static final String LOCKOUT_PREFIX = "login:lockout:";

    private final TemporaryCounter counter;
    private final TemporaryValue values;
    private final LoginLockoutProperties properties;
    private final TokenHasher hasher;

    public void requireUnlocked(String email) {
        values.remainingTime(LOCKOUT_PREFIX + hasher.hash(email))
                .ifPresent(remaining -> {
                    throw new AccountLockedException(remaining);
                });
    }

    public void recordFailure(String email) {
        String key = hasher.hash(email);
        long failures = counter.incrementar(FAILURES_PREFIX + key, properties.attemptsWindow());
        if (failures >= properties.maxAttempts()) {
            values.store(LOCKOUT_PREFIX + key, "1", properties.lockoutDuration());
            counter.remove(FAILURES_PREFIX + key);
        }
    }

    public void release(String email) {
        String key = hasher.hash(email);
        counter.remove(FAILURES_PREFIX + key);
        values.remove(LOCKOUT_PREFIX + key);
    }

    public void recordSuccess(String email) {
        counter.remove(FAILURES_PREFIX + hasher.hash(email));
    }
}
