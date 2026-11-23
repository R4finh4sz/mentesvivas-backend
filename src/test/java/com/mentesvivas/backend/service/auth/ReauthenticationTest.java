package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.entity.user.MfaMethod;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.auth.InvalidCodeException;
import com.mentesvivas.backend.service.audit.AuditTrail;
import com.mentesvivas.backend.service.mfa.SecondFactor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReauthenticationTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditTrail auditTrail;
    @Mock
    private SecondFactor emailFactor;

    private Reauthentication reauthentication;
    private User user;

    @BeforeEach
    void setUp() {
        when(emailFactor.method()).thenReturn(MfaMethod.EMAIL);
        reauthentication = new Reauthentication(passwordEncoder, auditTrail, List.of(emailFactor));
        user = new User();
        user.setId(UUID.randomUUID());
        user.setPasswordHash("hash");
        user.setMfaMethod(MfaMethod.EMAIL);
    }

    @Test
    void acceptsTheCorrectPasswordAndCode() {
        when(passwordEncoder.matches("senha", "hash")).thenReturn(true);

        assertThatCode(() -> reauthentication.verify(user, "senha", "123456")).doesNotThrowAnyException();
        verify(emailFactor).verify(user, "123456");
    }

    @Test
    void wrongPasswordIsDeniedWithoutTouchingTheSecondFactor() {
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        assertThatThrownBy(() -> reauthentication.verify(user, "errada", "123456"))
                .isInstanceOf(AccessDeniedException.class);

        verify(emailFactor, never()).verify(user, "123456");
        ArgumentCaptor<AuditRecord> record = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditTrail).recordIndependently(record.capture());
        assertThat(record.getValue().type()).isEqualTo(AuditEventType.TWO_FACTOR_FAILED);
    }

    @Test
    void wrongCodeIsAuditedAndRethrown() {
        when(passwordEncoder.matches("senha", "hash")).thenReturn(true);
        doThrow(new InvalidCodeException()).when(emailFactor).verify(user, "000000");

        assertThatThrownBy(() -> reauthentication.verify(user, "senha", "000000"))
                .isInstanceOf(InvalidCodeException.class);

        verify(auditTrail).recordIndependently(org.mockito.ArgumentMatchers.any(AuditRecord.class));
    }

    @Test
    void challengeUsesTheMethodOfTheUser() {
        reauthentication.challenge(user);

        verify(emailFactor).challenge(user);
    }
}
