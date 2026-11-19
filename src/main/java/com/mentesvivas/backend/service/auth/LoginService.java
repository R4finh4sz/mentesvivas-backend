package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.service.audit.AuditTrail;
import com.mentesvivas.backend.dto.auth.LoginRequest;
import com.mentesvivas.backend.dto.auth.LoginResult;
import com.mentesvivas.backend.dto.auth.TokenResponse;
import com.mentesvivas.backend.dto.auth.VerifyTwoFactorRequest;
import com.mentesvivas.backend.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final CredentialAuthenticator credentialAuthenticator;
    private final TokenService tokenService;
    private final TwoFactorRequirement twoFactorRequirement;
    private final TwoFactorService twoFactorService;
    private final AuditTrail auditTrail;

    @Transactional
    public LoginResult login(LoginRequest request) {
        User user = credentialAuthenticator.authenticate(request.email(), request.password());
        if (twoFactorRequirement.isRequiredFor(user)) {
            return LoginResult.challenged(twoFactorService.challenge(user));
        }
        auditTrail.record(AuditRecord.of(AuditEventType.LOGIN_SUCCEEDED, user.getId()));
        return LoginResult.authenticated(tokenService.issue(user));
    }

    @Transactional
    public TokenResponse verifyTwoFactor(VerifyTwoFactorRequest request) {
        return tokenService.issue(twoFactorService.verify(request.email(), request.code()));
    }
}
