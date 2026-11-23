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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vital_signs")
@Getter
@Setter
@NoArgsConstructor
public class VitalSign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID patientId;

    private Integer systolic;

    private Integer diastolic;

    private Integer heartRate;

    private Integer glucose;

    @Enumerated(EnumType.STRING)
    private GlucoseType glucoseType;

    @Column(precision = 3, scale = 1)
    private BigDecimal temperature;

    private Integer oxygenSaturation;

    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeasurementSource source;

    @Column(nullable = false)
    private UUID recordedBy;

    @Column(nullable = false)
    private Instant measuredAt;
}
