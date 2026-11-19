package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.dto.auth.LoginRequest;
import com.mentesvivas.backend.dto.auth.TwoFactorChallengeResponse;
import com.mentesvivas.backend.entity.user.MfaMethod;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.service.mfa.BackupCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TwoFactorRecoveryService {

    private final CredentialAuthenticator credentialAuthenticator;
    private final TwoFactorRequirement requirement;
    private final TwoFactorService twoFactorService;
    private final BackupCodeService backupCodes;
    private final TwoFactorEmails emails;

    @Transactional
    public TwoFactorChallengeResponse recoverWithEmail(LoginRequest request) {
        User user = credentialAuthenticator.authenticate(request.email(), request.password());
        if (!requirement.isRequiredFor(user) || user.getMfaMethod() != MfaMethod.AUTHENTICATOR) {
            throw new ConflictException("O aplicativo autenticador nao esta ativo nesta conta.");
        }
        user.setMfaMethod(MfaMethod.EMAIL);
        user.setTotpSecret(null);
        backupCodes.clear(user.getId());
        emails.sendMethodChangedNotice(user.getEmail());
        return twoFactorService.challenge(user);
    }
}
