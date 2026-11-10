package com.mentesvivas.backend.entity.link;

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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pending_invitations")
@Getter
@NoArgsConstructor
public class PendingInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false)
    private UUID inviterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InvitationKind kind;

    @Column(nullable = false)
    private Instant createdAt;

    public PendingInvitation(String email, UUID inviterId, InvitationKind kind) {
        this.email = email;
        this.inviterId = inviterId;
        this.kind = kind;
        this.createdAt = Instant.now();
    }
}
