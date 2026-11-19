package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.service.audit.AuditTrail;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.auth.AccountLockedException;
import com.mentesvivas.backend.exception.auth.InvalidCredentialsException;
import com.mentesvivas.backend.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialAuthenticatorTest {

    private static final String EMAIL = "ana@exemplo.com";
    private static final String PASSWORD = "SenhaForte1";

    @Mock
    private AuditTrail auditTrail;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LoginLockoutService lockoutService;

    private CredentialAuthenticator authenticator;
    private User user;

    @BeforeEach
    void setUp() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
        authenticator = new CredentialAuthenticator(userRepository, encoder, lockoutService, auditTrail);
        user = new User();
        user.setEmail(EMAIL);
        user.setPasswordHash(encoder.encode(PASSWORD));
    }

    @Test
    void correctCredentialsReturnTheUserAndClearTheFailures() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThat(authenticator.authenticate(EMAIL, PASSWORD)).isSameAs(user);

        verify(lockoutService).requireUnlocked(EMAIL);
        verify(lockoutService).recordSuccess(EMAIL);
        verify(lockoutService, never()).recordFailure(EMAIL);
    }

    @Test
    void wrongPasswordRecordsAFailure() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authenticator.authenticate(EMAIL, "outraSenha1"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(lockoutService).recordFailure(EMAIL);
        verify(lockoutService, never()).recordSuccess(EMAIL);
    }

    @Test
    void unknownEmailIsTreatedAsAWrongPassword() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticator.authenticate(EMAIL, PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(lockoutService).recordFailure(EMAIL);
    }

    @Test
    void lockedAccountIsRejectedBeforeLookingUpTheUser() {
        doThrow(new AccountLockedException(Duration.ofMinutes(5))).when(lockoutService).requireUnlocked(EMAIL);

        assertThatThrownBy(() -> authenticator.authenticate(EMAIL, PASSWORD))
                .isInstanceOf(AccountLockedException.class);

        verify(userRepository, never()).findByEmail(EMAIL);
    }

    @Test
    void inactiveAccountIsRejectedEvenWithTheCorrectPassword() {
        user.setActive(false);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authenticator.authenticate(EMAIL, PASSWORD))
                .isInstanceOf(com.mentesvivas.backend.exception.auth.AccountInactiveException.class);
    }

    @Test
    void inactiveAccountWithAWrongPasswordIsStillTreatedAsInvalidCredentials() {
        user.setActive(false);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authenticator.authenticate(EMAIL, "outraSenha1"))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
