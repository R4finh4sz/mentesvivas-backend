package com.mentesvivas.backend.entity.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mfa_backup_codes")
@Getter
@Setter
@NoArgsConstructor
public class MfaBackupCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 64)
    private String codeHash;

    private Instant usedAt;

    @Column(nullable = false)
    private Instant createdAt;

    public MfaBackupCode(UUID userId, String codeHash, Instant createdAt) {
        this.userId = userId;
        this.codeHash = codeHash;
        this.createdAt = createdAt;
    }

    public boolean isUsed() {
        return usedAt != null;
    }
}
