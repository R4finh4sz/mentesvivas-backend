package com.mentesvivas.backend.entity.alert;

import com.mentesvivas.backend.entity.notification.NotificationType;
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
@Table(name = "sos_alerts")
@Getter
@NoArgsConstructor
public class SosAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID patientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private AlertStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationType type;

    private UUID sourceId;

    @Column(nullable = false)
    private UUID createdBy;

    private Instant seenAt;
    private UUID seenBy;
    private Instant acknowledgedAt;
    private UUID acknowledgedBy;
    private Instant resolvedAt;
    private UUID resolvedBy;

    public SosAlert(UUID patientId, Instant createdAt) {
        this(patientId, patientId, NotificationType.SOS_ALERT, null, createdAt);
    }

    public SosAlert(UUID patientId, UUID createdBy, NotificationType type, UUID sourceId, Instant createdAt) {
        this.patientId = patientId;
        this.createdBy = createdBy;
        this.type = type;
        this.sourceId = sourceId;
        this.createdAt = createdAt;
        this.status = AlertStatus.CREATED;
    }

    public boolean advance(AlertStatus target, UUID actorId, Instant now) {
        if (!status.isBefore(target)) {
            return false;
        }
        if (status.isBefore(AlertStatus.SEEN) && target.compareTo(AlertStatus.SEEN) >= 0) {
            seenAt = now;
            seenBy = actorId;
        }
        if (status.isBefore(AlertStatus.ACKNOWLEDGED) && target.compareTo(AlertStatus.ACKNOWLEDGED) >= 0) {
            acknowledgedAt = now;
            acknowledgedBy = actorId;
        }
        if (target == AlertStatus.RESOLVED) {
            resolvedAt = now;
            resolvedBy = actorId;
        }
        status = target;
        return true;
    }
}
