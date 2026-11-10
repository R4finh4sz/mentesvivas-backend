package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.cache.TemporaryValue;
import com.mentesvivas.backend.config.TwoFactorProperties;
import com.mentesvivas.backend.dto.auth.TwoFactorChallengeResponse;
import com.mentesvivas.backend.entity.user.MfaMethod;
import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.auth.InvalidCodeException;
import com.mentesvivas.backend.repository.user.UserRepository;
import com.mentesvivas.backend.security.auth.TokenHasher;
import com.mentesvivas.backend.service.mfa.SecondFactor;
import com.mentesvivas.backend.support.InMemoryTemporaryStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TwoFactorServiceTest {

    private static final String EMAIL = "dr@clinica.com";

    @Mock
    private UserRepository userRepository;
    @Mock
    private SecondFactor emailFactor;
    @Mock
    private SecondFactor authenticatorFactor;

    private TwoFactorService service;

    @BeforeEach
    void setUp() {
        lenient().when(emailFactor.method()).thenReturn(MfaMethod.EMAIL);
        lenient().when(authenticatorFactor.method()).thenReturn(MfaMethod.AUTHENTICATOR);
        TemporaryValue storage = new InMemoryTemporaryStorage();
        TwoFactorProperties properties = new TwoFactorProperties(Duration.ofMinutes(10), 5, 5, Duration.ofHours(1));
        service = new TwoFactorService(userRepository, new TwoFactorRequirement(), storage, new TokenHasher(),
                properties, List.of(emailFactor, authenticatorFactor));
    }

    private User user(Role role, MfaMethod method) {
        User user = new User();
        user.setEmail(EMAIL);
        user.setRole(role);
        user.setMfaMethod(method);
        return user;
    }

    @Test
    void challengeUsesTheMethodChosenByTheUser() {
        User byEmail = user(Role.DOCTOR, MfaMethod.EMAIL);
        User byApp = user(Role.DOCTOR, MfaMethod.AUTHENTICATOR);
        var emailChallenge = new TwoFactorChallengeResponse(true, "email", 600, MfaMethod.EMAIL);
        var appChallenge = new TwoFactorChallengeResponse(true, "app", 600, MfaMethod.AUTHENTICATOR);
        when(emailFactor.challenge(byEmail)).thenReturn(emailChallenge);
        when(authenticatorFactor.challenge(byApp)).thenReturn(appChallenge);

        assertThat(service.challenge(byEmail)).isSameAs(emailChallenge);
        assertThat(service.challenge(byApp)).isSameAs(appChallenge);
    }

    @Test
    void verifyDelegatesToTheUsersFactorAfterAChallenge() {
        User doctor = user(Role.DOCTOR, MfaMethod.AUTHENTICATOR);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(doctor));
        service.challenge(doctor);

        assertThat(service.verify(EMAIL, "123456")).isSameAs(doctor);
        verify(authenticatorFactor).verify(doctor, "123456");
        verify(emailFactor, never()).verify(any(), any());
    }

    @Test
    void verifyWithoutAPriorChallengeIsRejected() {
        User doctor = user(Role.DOCTOR, MfaMethod.AUTHENTICATOR);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(doctor));

        assertThatThrownBy(() -> service.verify(EMAIL, "123456")).isInstanceOf(InvalidCodeException.class);
        verify(authenticatorFactor, never()).verify(any(), any());
    }

    @Test
    void verifyRejectsInvalidCodesAndKeepsTheChallengeOpen() {
        User doctor = user(Role.DOCTOR, MfaMethod.EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(doctor));
        doThrow(new InvalidCodeException()).when(emailFactor).verify(doctor, "000000");
        service.challenge(doctor);

        assertThatThrownBy(() -> service.verify(EMAIL, "000000")).isInstanceOf(InvalidCodeException.class);
        assertThat(service.verify(EMAIL, "123456")).isSameAs(doctor);
    }

    @Test
    void aChallengeCanBeCompletedOnlyOnce() {
        User doctor = user(Role.DOCTOR, MfaMethod.EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(doctor));
        service.challenge(doctor);
        service.verify(EMAIL, "123456");

        assertThatThrownBy(() -> service.verify(EMAIL, "123456")).isInstanceOf(InvalidCodeException.class);
    }

    @Test
    void verifyRejectsUnknownEmailsAndProfilesWithoutTheRequirement() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.verify(EMAIL, "123456")).isInstanceOf(InvalidCodeException.class);

        when(userRepository.findByEmail("c@x.com")).thenReturn(Optional.of(user(Role.CAREGIVER, MfaMethod.EMAIL)));
        assertThatThrownBy(() -> service.verify("c@x.com", "123456")).isInstanceOf(InvalidCodeException.class);
    }
}
