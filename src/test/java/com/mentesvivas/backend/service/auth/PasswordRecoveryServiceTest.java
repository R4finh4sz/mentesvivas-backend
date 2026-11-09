package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.config.PasswordRecoveryProperties;
import com.mentesvivas.backend.dto.auth.ResetPasswordRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryServiceTest {

    private static final String EMAIL = "ana@exemplo.com";

    private final CodePolicy policy = new CodePolicy("recovery", Duration.ofMinutes(30), 5, 3, Duration.ofHours(1));

    @Mock
    private UserRepository userRepository;
    @Mock
    private OneTimeCodeService codeService;
    @Mock
    private PasswordRecoveryEmails emails;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private LoginLockoutService loginLockoutService;

    private PasswordRecoveryService service;

    @BeforeEach
    void setUp() {
        PasswordRecoveryProperties properties = new PasswordRecoveryProperties(Duration.ofMinutes(30), 5, 3, Duration.ofHours(1));
        service = new PasswordRecoveryService(userRepository, codeService, policy, emails, passwordEncoder,
                refreshTokenService, loginLockoutService, properties);
    }

    @Test
    void requestForUnknownEmailGeneratesNoCodeNorEmail() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        var response = service.requestRecovery(EMAIL);

        assertThat(response.codeExpiresInSeconds()).isEqualTo(1800);
        verifyNoInteractions(codeService, emails);
    }

    @Test
    void requestForRegisteredEmailSendsCode() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(new User()));
        when(codeService.generate(policy, EMAIL)).thenReturn(Optional.of("123456"));

        service.requestRecovery(EMAIL);

        verify(emails).sendCode(EMAIL, "123456");
    }

    @Test
    void requestAboveSendLimitDoesNotSendEmail() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(new User()));
        when(codeService.generate(policy, EMAIL)).thenReturn(Optional.empty());

        service.requestRecovery(EMAIL);

        verify(emails, never()).sendCode(anyString(), anyString());
    }

    @Test
    void resetChangesPasswordRevokesSessionsReleasesLockoutAndNotifies() {
        User user = new User();
        user.setEmail(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NovaSenha99")).thenReturn("hash-novo");

        service.reset(new ResetPasswordRequest(EMAIL, "123456", "NovaSenha99", "NovaSenha99"));

        assertThat(user.getPasswordHash()).isEqualTo("hash-novo");
        verify(codeService).validateAndConsume(policy, EMAIL, "123456");
        verify(refreshTokenService).revokeAll(user);
        verify(loginLockoutService).release(EMAIL);
        verify(emails).notifyPasswordChanged(EMAIL);
    }

    @Test
    void resetWithInvalidCodeChangesNothing() {
        doThrow(new InvalidCodeException()).when(codeService).validateAndConsume(any(), anyString(), anyString());

        assertThatThrownBy(() -> service.reset(new ResetPasswordRequest(EMAIL, "123456", "NovaSenha99", "NovaSenha99")))
                .isInstanceOf(InvalidCodeException.class);

        verifyNoInteractions(passwordEncoder, refreshTokenService, loginLockoutService, emails);
        verify(userRepository, never()).findByEmail(any());
    }
}
