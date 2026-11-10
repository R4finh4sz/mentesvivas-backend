package com.mentesvivas.backend.entity.notification;

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
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    private UUID referenceId;

    @Column(nullable = false, length = 300)
    private String message;

    @Column(nullable = false)
    private Instant createdAt;

    @Setter
    private Instant readAt;

    public Notification(UUID recipientId, NotificationType type, UUID referenceId, String message) {
        this.recipientId = recipientId;
        this.type = type;
        this.referenceId = referenceId;
        this.message = message;
        this.createdAt = Instant.now();
    }
}
