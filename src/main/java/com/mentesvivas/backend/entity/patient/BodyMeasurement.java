package com.mentesvivas.backend.entity.patient;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "body_measurements")
@Getter
@Setter
@NoArgsConstructor
public class BodyMeasurement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID patientId;

    @Column(name = "weight_kg", precision = 5, scale = 1)
    private BigDecimal weightKg;

    @Column(name = "height_m", precision = 3, scale = 2)
    private BigDecimal heightM;

    @Column(nullable = false)
    private UUID recordedBy;

    @Column(nullable = false)
    private Instant measuredAt;
}
