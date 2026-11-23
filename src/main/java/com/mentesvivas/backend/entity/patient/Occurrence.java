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
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "occurrences")
@Getter
@Setter
@NoArgsConstructor
public class Occurrence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID patientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private OccurrenceType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OccurrenceSeverity severity;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false)
    private boolean needsHospital;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MeasurementSource source;

    @Column(nullable = false)
    private UUID recordedBy;

    @Column(nullable = false)
    private Instant occurredAt;
}
