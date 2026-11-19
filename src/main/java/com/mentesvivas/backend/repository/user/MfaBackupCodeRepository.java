package com.mentesvivas.backend.repository.user;

import com.mentesvivas.backend.entity.user.MfaBackupCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MfaBackupCodeRepository extends JpaRepository<MfaBackupCode, UUID> {

    Optional<MfaBackupCode> findByUserIdAndCodeHash(UUID userId, String codeHash);

    long countByUserIdAndUsedAtIsNull(UUID userId);

    void deleteAllByUserId(UUID userId);
}
