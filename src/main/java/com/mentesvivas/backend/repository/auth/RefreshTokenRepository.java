package com.mentesvivas.backend.repository.auth;

import com.mentesvivas.backend.entity.auth.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshToken r set r.revokedAt = :now where r.user.id = :userId and r.revokedAt is null")
    void revokeAllByUser(UUID userId, Instant now);
}
