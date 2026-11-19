package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.dto.auth.LoginRequest;
import com.mentesvivas.backend.dto.auth.TwoFactorChallengeResponse;
import com.mentesvivas.backend.entity.user.MfaMethod;
import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.auth.InvalidCredentialsException;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.service.mfa.BackupCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TwoFactorRecoveryServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "dr.paulo@clinica.com";

    @Mock
    private CredentialAuthenticator credentialAuthenticator;
    @Mock
    private TwoFactorService twoFactorService;
    @Mock
    private BackupCodeService backupCodes;
    @Mock
    private TwoFactorEmails emails;

    private final TwoFactorRequirement requirement = new TwoFactorRequirement();
    private TwoFactorRecoveryService service;
    private User user;
    private final LoginRequest request = new LoginRequest(EMAIL, "SenhaForte1");

    @BeforeEach
    void setUp() {
        service = new TwoFactorRecoveryService(credentialAuthenticator, requirement, twoFactorService, backupCodes, emails);
        user = new User() {
            @Override
            public UUID getId() {
                return USER_ID;
            }
        };
        user.setEmail(EMAIL);
        user.setRole(Role.DOCTOR);
        user.setMfaMethod(MfaMethod.AUTHENTICATOR);
        user.setTotpSecret("encrypted");
    }

    @Test
    void switchesToEmailClearsTheAuthenticatorNotifiesAndSendsTheChallenge() {
        var challenge = new TwoFactorChallengeResponse(true, "msg", 600, MfaMethod.EMAIL);
        when(credentialAuthenticator.authenticate(EMAIL, "SenhaForte1")).thenReturn(user);
        when(twoFactorService.challenge(user)).thenReturn(challenge);

        var response = service.recoverWithEmail(request);

        assertThat(response).isSameAs(challenge);
        assertThat(user.getMfaMethod()).isEqualTo(MfaMethod.EMAIL);
        assertThat(user.getTotpSecret()).isNull();
        InOrder order = inOrder(backupCodes, emails, twoFactorService);
        order.verify(backupCodes).clear(USER_ID);
        order.verify(emails).sendMethodChangedNotice(EMAIL);
        order.verify(twoFactorService).challenge(user);
    }

    @Test
    void administratorCanRecoverToo() {
        user.setRole(Role.ADMINISTRATOR);
        when(credentialAuthenticator.authenticate(EMAIL, "SenhaForte1")).thenReturn(user);

        service.recoverWithEmail(request);

        assertThat(user.getMfaMethod()).isEqualTo(MfaMethod.EMAIL);
    }

    @Test
    void accountOnEmailAlreadyIsAConflict() {
        user.setMfaMethod(MfaMethod.EMAIL);
        user.setTotpSecret(null);
        when(credentialAuthenticator.authenticate(EMAIL, "SenhaForte1")).thenReturn(user);

        assertThatThrownBy(() -> service.recoverWithEmail(request)).isInstanceOf(ConflictException.class);

        verify(emails, never()).sendMethodChangedNotice(EMAIL);
        verifyNoInteractions(twoFactorService);
    }

    @Test
    void profileWithoutSecondFactorIsAConflict() {
        user.setRole(Role.PATIENT);
        when(credentialAuthenticator.authenticate(EMAIL, "SenhaForte1")).thenReturn(user);

        assertThatThrownBy(() -> service.recoverWithEmail(request)).isInstanceOf(ConflictException.class);

        assertThat(user.getMfaMethod()).isEqualTo(MfaMethod.AUTHENTICATOR);
    }

    @Test
    void wrongPasswordChangesNothing() {
        when(credentialAuthenticator.authenticate(EMAIL, "SenhaForte1")).thenThrow(new InvalidCredentialsException());

        assertThatThrownBy(() -> service.recoverWithEmail(request)).isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(backupCodes, emails, twoFactorService);
    }
}
