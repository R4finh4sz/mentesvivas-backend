package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.service.audit.AuditTrail;
import com.mentesvivas.backend.config.JwtProperties;
import com.mentesvivas.backend.entity.auth.RefreshToken;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.auth.InvalidSessionException;
import com.mentesvivas.backend.repository.auth.RefreshTokenRepository;
import com.mentesvivas.backend.security.auth.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final SecureRandom secureRandom = new SecureRandom();
    private final JwtProperties properties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHasher tokenHasher;
    private final AuditTrail auditTrail;

    @Transactional
    public String create(User user) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(tokenHasher.hash(value));
        refreshToken.setExpiresAt(Instant.now().plus(properties.refreshTokenExpiration()));
        refreshTokenRepository.save(refreshToken);
        return value;
    }

    @Transactional
    public void revoke(String value) {
        refreshTokenRepository.findByTokenHash(tokenHasher.hash(value))
                .filter(token -> token.getRevokedAt() == null)
                .ifPresent(token -> {
                    token.setRevokedAt(Instant.now());
                    auditTrail.record(AuditRecord.of(AuditEventType.LOGOUT, token.getUser().getId()));
                });
    }

    @Transactional
    public void revokeAll(User user) {
        refreshTokenRepository.revokeAllByUser(user.getId(), Instant.now());
    }

    @Transactional(noRollbackFor = InvalidSessionException.class)
    public User consume(String value) {
        Instant now = Instant.now();
        RefreshToken current = refreshTokenRepository.findByTokenHash(tokenHasher.hash(value))
                .orElseThrow(InvalidSessionException::new);

        if (current.getRevokedAt() != null) {
            refreshTokenRepository.revokeAllByUser(current.getUser().getId(), now);
            auditTrail.record(AuditRecord.of(AuditEventType.REFRESH_TOKEN_REUSED, current.getUser().getId()));
            throw new InvalidSessionException();
        }
        if (!current.isActive(now)) {
            throw new InvalidSessionException();
        }

        current.setRevokedAt(now);
        auditTrail.record(AuditRecord.of(AuditEventType.TOKEN_REFRESHED, current.getUser().getId()));
        return current.getUser();
    }
}
