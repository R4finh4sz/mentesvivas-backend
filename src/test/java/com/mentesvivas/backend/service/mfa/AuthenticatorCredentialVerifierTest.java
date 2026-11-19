package com.mentesvivas.backend.service.mfa;

import com.mentesvivas.backend.config.TwoFactorProperties;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.auth.InvalidCodeException;
import com.mentesvivas.backend.security.auth.TokenHasher;
import com.mentesvivas.backend.support.InMemoryTemporaryStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticatorCredentialVerifierTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String ENCRYPTED = "encrypted-secret";
    private static final String SECRET = "JBSWY3DPEHPK3PXP";

    @Mock
    private AuthenticatorCodeVerifier totpVerifier;
    @Mock
    private BackupCodeService backupCodes;
    @Mock
    private SecretCipher cipher;

    private InMemoryTemporaryStorage memory;
    private AuthenticatorCredentialVerifier verifier;
    private User user;

    @BeforeEach
    void setUp() {
        memory = new InMemoryTemporaryStorage();
        TwoFactorProperties properties = new TwoFactorProperties(Duration.ofMinutes(10), 3, 3, Duration.ofMinutes(15));
        verifier = new AuthenticatorCredentialVerifier(totpVerifier, backupCodes, cipher, memory, new TokenHasher(),
                properties);
        user = new User() {
            @Override
            public UUID getId() {
                return USER_ID;
            }
        };
        user.setTotpSecret(ENCRYPTED);
    }

    @Test
    void sixDigitCodeGoesToTheTotpVerifier() {
        when(cipher.decrypt(ENCRYPTED)).thenReturn(SECRET);

        verifier.verify(user, "123456");

        verify(totpVerifier).verify(USER_ID, SECRET, "123456");
        verify(backupCodes, never()).consume(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void tenDigitCodeIsCheckedAsABackupCode() {
        when(backupCodes.consume(USER_ID, "1234567890")).thenReturn(true);

        assertThatCode(() -> verifier.verify(user, "1234567890")).doesNotThrowAnyException();

        verify(totpVerifier, never()).verify(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void wrongBackupCodeIsRejected() {
        when(backupCodes.consume(USER_ID, "1234567890")).thenReturn(false);

        assertThatThrownBy(() -> verifier.verify(user, "1234567890")).isInstanceOf(InvalidCodeException.class);
    }

    @Test
    void backupCodeAttemptsAreLimited() {
        when(backupCodes.consume(org.mockito.ArgumentMatchers.eq(USER_ID), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(false);

        for (int i = 0; i < 3; i++) {
            String code = "00000000%02d".formatted(i);
            assertThatThrownBy(() -> verifier.verify(user, code)).isInstanceOf(InvalidCodeException.class);
        }

        assertThatThrownBy(() -> verifier.verify(user, "1234567890")).isInstanceOf(InvalidCodeException.class);
        verify(backupCodes, org.mockito.Mockito.times(3)).consume(org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void userWithoutAnAuthenticatorIsRejected() {
        user.setTotpSecret(null);

        assertThatThrownBy(() -> verifier.verify(user, "123456")).isInstanceOf(InvalidCodeException.class);
        assertThatThrownBy(() -> verifier.verify(user, "1234567890")).isInstanceOf(InvalidCodeException.class);
    }
}
