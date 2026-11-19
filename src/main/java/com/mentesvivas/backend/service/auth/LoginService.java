package com.mentesvivas.backend.service.auth;

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

    @Transactional
    public LoginResult login(LoginRequest request) {
        User user = credentialAuthenticator.authenticate(request.email(), request.password());
        if (twoFactorRequirement.isRequiredFor(user)) {
            return LoginResult.challenged(twoFactorService.challenge(user));
        }
        return LoginResult.authenticated(tokenService.issue(user));
    }

    @Transactional
    public TokenResponse verifyTwoFactor(VerifyTwoFactorRequest request) {
        return tokenService.issue(twoFactorService.verify(request.email(), request.code()));
    }
}
