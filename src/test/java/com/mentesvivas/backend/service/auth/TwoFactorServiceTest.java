package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.config.TwoFactorProperties;
import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.auth.InvalidCodeException;
import com.mentesvivas.backend.repository.user.UserRepository;
import com.mentesvivas.backend.service.code.CodePolicy;
import com.mentesvivas.backend.service.code.OneTimeCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TwoFactorServiceTest {

    private static final String EMAIL = "dr@clinica.com";

    private final CodePolicy policy = new CodePolicy("two-factor", Duration.ofMinutes(10), 5, 5, Duration.ofHours(1));

    @Mock
    private UserRepository userRepository;
    @Mock
    private OneTimeCodeService codeService;
    @Mock
    private TwoFactorEmails emails;

    private TwoFactorService service;

    @BeforeEach
    void setUp() {
        TwoFactorProperties properties = new TwoFactorProperties(Duration.ofMinutes(10), 5, 5, Duration.ofHours(1));
        service = new TwoFactorService(userRepository, codeService, policy, emails, new TwoFactorRequirement(), properties);
    }

    private User user(Role role) {
        User user = new User();
        user.setEmail(EMAIL);
        user.setRole(role);
        return user;
    }

    @Test
    void challengeSendsTheGeneratedCodeAndReportsValidity() {
        when(codeService.generate(policy, EMAIL)).thenReturn(Optional.of("123456"));

        var challenge = service.challenge(user(Role.DOCTOR));

        assertThat(challenge.twoFactorRequired()).isTrue();
        assertThat(challenge.codeExpiresInSeconds()).isEqualTo(600);
        verify(emails).sendCode(EMAIL, "123456");
    }

    @Test
    void challengeAboveTheSendLimitStillAnswersWithoutSendingAnotherEmail() {
        when(codeService.generate(policy, EMAIL)).thenReturn(Optional.empty());

        var challenge = service.challenge(user(Role.DOCTOR));

        assertThat(challenge.twoFactorRequired()).isTrue();
        verifyNoInteractions(emails);
    }

    @Test
    void verifyReturnsTheDoctorWhenTheCodeIsValid() {
        User doctor = user(Role.DOCTOR);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(doctor));

        assertThat(service.verify(EMAIL, "123456")).isSameAs(doctor);
        verify(codeService).validateAndConsume(policy, EMAIL, "123456");
    }

    @Test
    void verifyRejectsInvalidCodes() {
        doThrow(new InvalidCodeException()).when(codeService).validateAndConsume(policy, EMAIL, "000000");

        assertThatThrownBy(() -> service.verify(EMAIL, "000000")).isInstanceOf(InvalidCodeException.class);
        verifyNoInteractions(userRepository);
    }

    @Test
    void verifyRejectsProfilesThatDoNotRequireTheSecondFactor() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user(Role.CAREGIVER)));

        assertThatThrownBy(() -> service.verify(EMAIL, "123456")).isInstanceOf(InvalidCodeException.class);
    }
}
