package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.config.LoginLockoutProperties;
import com.mentesvivas.backend.exception.auth.AccountLockedException;
import com.mentesvivas.backend.security.auth.TokenHasher;
import com.mentesvivas.backend.support.InMemoryTemporaryStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginLockoutServiceTest {

    private static final String EMAIL = "ana@exemplo.com";

    private LoginLockoutService service;

    @BeforeEach
    void setUp() {
        InMemoryTemporaryStorage memory = new InMemoryTemporaryStorage();
        LoginLockoutProperties properties = new LoginLockoutProperties(3, Duration.ofMinutes(15), Duration.ofMinutes(10));
        service = new LoginLockoutService(memory, memory, properties, new TokenHasher());
    }

    @Test
    void doesNotLockBelowLimit() {
        service.recordFailure(EMAIL);
        service.recordFailure(EMAIL);

        assertThatCode(() -> service.requireUnlocked(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void locksAtLimitAndReportsRemainingTime() {
        for (int i = 0; i < 3; i++) {
            service.recordFailure(EMAIL);
        }

        assertThatThrownBy(() -> service.requireUnlocked(EMAIL))
                .isInstanceOfSatisfying(AccountLockedException.class, ex ->
                        assertThat(ex.getRemainingTime()).isPositive().isLessThanOrEqualTo(Duration.ofMinutes(10)));
    }

    @Test
    void lockoutIsPerEmail() {
        for (int i = 0; i < 3; i++) {
            service.recordFailure(EMAIL);
        }

        assertThatCode(() -> service.requireUnlocked("outra@exemplo.com")).doesNotThrowAnyException();
    }

    @Test
    void successResetsCount() {
        service.recordFailure(EMAIL);
        service.recordFailure(EMAIL);
        service.recordSuccess(EMAIL);
        service.recordFailure(EMAIL);
        service.recordFailure(EMAIL);

        assertThatCode(() -> service.requireUnlocked(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void releaseRemovesActiveLockout() {
        for (int i = 0; i < 3; i++) {
            service.recordFailure(EMAIL);
        }

        service.release(EMAIL);

        assertThatCode(() -> service.requireUnlocked(EMAIL)).doesNotThrowAnyException();
    }
}
