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
@Table(name = "access_events")
@Immutable
@Getter
@NoArgsConstructor
public class AccessEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(nullable = false)
    private UUID accessorUserId;

    @Column(nullable = false)
    private UUID patientUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PatientDataArea area;

    public AccessEvent(Instant occurredAt, UUID accessorUserId, UUID patientUserId, PatientDataArea area) {
        this.occurredAt = occurredAt;
        this.accessorUserId = accessorUserId;
        this.patientUserId = patientUserId;
        this.area = area;
    }
}
