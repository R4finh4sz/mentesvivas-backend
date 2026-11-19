package com.mentesvivas.backend.service.mfa;

import com.mentesvivas.backend.config.TwoFactorProperties;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.auth.CodeSendLimitException;
import com.mentesvivas.backend.security.auth.TokenHasher;
import com.mentesvivas.backend.service.auth.TwoFactorEmails;
import com.mentesvivas.backend.service.code.CodePolicy;
import com.mentesvivas.backend.service.code.OneTimeCodeService;
import com.mentesvivas.backend.support.InMemoryTemporaryStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailSecondFactorTest {

    private static final String EMAIL = "dr.paulo@clinica.com";
    private static final Duration WINDOW = Duration.ofMinutes(15);

    @Mock
    private TwoFactorEmails emails;

    private EmailSecondFactor factor;
    private User user;

    @BeforeEach
    void setUp() {
        var memory = new InMemoryTemporaryStorage();
        var codeService = new OneTimeCodeService(memory, memory, new TokenHasher());
        var policy = new CodePolicy("two-factor", Duration.ofMinutes(10), 5, 3, WINDOW);
        var properties = new TwoFactorProperties(Duration.ofMinutes(10), 5, 3, WINDOW);
        factor = new EmailSecondFactor(codeService, policy, emails, properties);
        user = new User();
        user.setEmail(EMAIL);
    }

    @Test
    void sendsTheCodeForTheFirstThreeChallenges() {
        for (int i = 0; i < 3; i++) {
            assertThat(factor.challenge(user).twoFactorRequired()).isTrue();
        }

        verify(emails, times(3)).sendCode(org.mockito.ArgumentMatchers.eq(EMAIL), anyString());
    }

    @Test
    void fourthChallengeInTheWindowIsBlockedWithTheRemainingTime() {
        for (int i = 0; i < 3; i++) {
            factor.challenge(user);
        }

        assertThatThrownBy(() -> factor.challenge(user))
                .isInstanceOfSatisfying(CodeSendLimitException.class, ex -> {
                    assertThat(ex.getRemainingTime()).isPositive();
                    assertThat(ex.getRemainingTime()).isLessThanOrEqualTo(WINDOW);
                });

        verify(emails, times(3)).sendCode(org.mockito.ArgumentMatchers.eq(EMAIL), anyString());
    }
}
