package com.mentesvivas.backend.entity.patient;

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
@Table(name = "sensitive_data_changes")
@Immutable
@Getter
@NoArgsConstructor
public class SensitiveDataChange {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID patientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SensitiveField field;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ChangeType changeType;

    @Column(nullable = false, length = 300)
    private String summary;

    @Column(nullable = false)
    private UUID changedBy;

    @Column(nullable = false)
    private Instant changedAt;

    public SensitiveDataChange(UUID patientId, SensitiveField field, ChangeType changeType,
                               String summary, UUID changedBy, Instant changedAt) {
        this.patientId = patientId;
        this.field = field;
        this.changeType = changeType;
        this.summary = summary;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
    }
}
