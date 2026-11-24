package com.mentesvivas.backend.entity.patient;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;

@Embeddable
public record ClinicalRange(
        @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) ClinicalMetric metric,
        @Column(precision = 14, scale = 6) BigDecimal lowerBound,
        @Enumerated(EnumType.STRING) @Column(nullable = false, length = 6) ClinicalColor color
) {}
