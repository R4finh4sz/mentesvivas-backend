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
@Table(name = "routine_records")
@Getter
@Setter
@NoArgsConstructor
public class RoutineRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID patientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private RoutineType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private Mood mood;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private SleepQuality sleepQuality;

    @Column(precision = 3, scale = 1)
    private BigDecimal sleepHours;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private MealPeriod mealPeriod;

    private Boolean mealAccepted;

    private Integer amountMl;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private BathCooperation bathCooperation;

    private Integer urineFrequency;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private UrineAspect urineAspect;

    private Integer stoolFrequency;

    private Integer bristolScale;

    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MeasurementSource source;

    @Column(nullable = false)
    private UUID recordedBy;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(nullable = false)
    private Instant recordedAt;
}
