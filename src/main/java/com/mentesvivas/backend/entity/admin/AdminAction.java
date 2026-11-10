package com.mentesvivas.backend.entity.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin_actions")
@Immutable
@Getter
@NoArgsConstructor
public class AdminAction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID adminId;

    @Column(nullable = false)
    private UUID targetUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AdminActionType action;

    @Column(nullable = false)
    private Instant performedAt;

    public AdminAction(UUID adminId, UUID targetUserId, AdminActionType action, Instant performedAt) {
        this.adminId = adminId;
        this.targetUserId = targetUserId;
        this.action = action;
        this.performedAt = performedAt;
    }
}
