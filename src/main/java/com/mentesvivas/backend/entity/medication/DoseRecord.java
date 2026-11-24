package com.mentesvivas.backend.entity.medication;

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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dose_records")
@Getter
@Setter
@NoArgsConstructor
public class DoseRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID medicationId;

    @Column(nullable = false)
    private UUID patientId;

    @Column(nullable = false)
    private Instant scheduledFor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private DoseStatus status;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal stockDiscounted = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean duplicate;

    @Column(nullable = false)
    private boolean corrected;

    @Column(nullable = false)
    private UUID recordedBy;

    @Column(nullable = false)
    private Instant recordedAt;

    private UUID clientOperationId;
}
