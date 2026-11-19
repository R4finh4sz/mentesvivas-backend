package com.mentesvivas.backend.service.mfa;

import com.mentesvivas.backend.cache.TemporaryCounter;
import com.mentesvivas.backend.config.TwoFactorProperties;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.auth.InvalidCodeException;
import com.mentesvivas.backend.security.auth.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticatorCredentialVerifier {

    private final AuthenticatorCodeVerifier totpVerifier;
    private final BackupCodeService backupCodes;
    private final SecretCipher cipher;
    private final TemporaryCounter counter;
    private final TokenHasher hasher;
    private final TwoFactorProperties properties;

    public void verify(User user, String code) {
        if (user.getTotpSecret() == null) {
            throw new InvalidCodeException();
        }
        if (BackupCodeService.looksLikeBackupCode(code)) {
            verifyBackupCode(user, code);
            return;
        }
        totpVerifier.verify(user.getId(), cipher.decrypt(user.getTotpSecret()), code);
    }

    private void verifyBackupCode(User user, String code) {
        String attemptsKey = "backup-code:attempts:" + hasher.hash(user.getId().toString());
        if (counter.increment(attemptsKey, properties.codeValidity()) > properties.maxAttempts()) {
            throw new InvalidCodeException();
        }
        if (!backupCodes.consume(user.getId(), code)) {
            throw new InvalidCodeException();
        }
        counter.remove(attemptsKey);
    }
}
