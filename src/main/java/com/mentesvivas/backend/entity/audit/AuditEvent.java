package com.mentesvivas.backend.entity.audit;

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
@Table(name = "audit_events")
@Immutable
@Getter
@NoArgsConstructor
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditEventType eventType;

    private UUID actorUserId;

    private UUID patientUserId;

    private UUID onBehalfOfUserId;

    private UUID correctsEventId;

    @Column(length = 500)
    private String justification;

    @Column(length = 1000)
    private String beforeValue;

    @Column(length = 1000)
    private String afterValue;

    public AuditEvent(Instant occurredAt, AuditRecord record) {
        this.occurredAt = occurredAt;
        this.eventType = record.type();
        this.actorUserId = record.actorUserId();
        this.patientUserId = record.patientUserId();
        this.onBehalfOfUserId = record.onBehalfOfUserId();
        this.correctsEventId = record.correctsEventId();
        this.justification = record.justification();
        this.beforeValue = record.beforeValue();
        this.afterValue = record.afterValue();
    }
}
