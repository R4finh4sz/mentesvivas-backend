package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.config.JwtProperties;
import com.mentesvivas.backend.dto.auth.TokenResponse;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.auth.InvalidSessionException;
import com.mentesvivas.backend.mapper.user.UserMapper;
import com.mentesvivas.backend.service.terms.TermsAcceptanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TokenService {

    private static final String TOKEN_TYPE = "Bearer";

    private final AccessTokenService accessTokenService;
    private final RefreshTokenService refreshTokenService;
    private final TermsAcceptanceService termsAcceptanceService;
    private final JwtProperties properties;

    @Transactional
    public TokenResponse issue(User user) {
        return new TokenResponse(
                accessTokenService.generate(user),
                refreshTokenService.create(user),
                TOKEN_TYPE,
                properties.accessTokenExpiration().toSeconds(),
                properties.refreshTokenExpiration().toSeconds(),
                user.getRole(),
                termsAcceptanceService.acceptedCurrentVersion(user),
                UserMapper.toResponse(user));
    }

    @Transactional
    public void end(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    @Transactional(noRollbackFor = InvalidSessionException.class)
    public TokenResponse renew(String refreshToken) {
        return issue(refreshTokenService.consume(refreshToken));
    }
}
