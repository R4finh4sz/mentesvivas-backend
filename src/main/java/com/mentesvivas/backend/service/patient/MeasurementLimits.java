package com.mentesvivas.backend.service.patient;

import java.math.BigDecimal;

public record MeasurementLimits(
        BigDecimal expectedMin,
        BigDecimal expectedMax,
        BigDecimal possibleMin,
        BigDecimal possibleMax
) {

    public static final MeasurementLimits WEIGHT_KG = new MeasurementLimits(
            new BigDecimal("30"), new BigDecimal("300"), new BigDecimal("10"), new BigDecimal("500"));

    public static final MeasurementLimits HEIGHT_M = new MeasurementLimits(
            new BigDecimal("1.20"), new BigDecimal("2.30"), new BigDecimal("0.50"), new BigDecimal("2.80"));

    public boolean isImpossible(BigDecimal value) {
        return value.compareTo(possibleMin) < 0 || value.compareTo(possibleMax) > 0;
    }

    public boolean isOutsideExpected(BigDecimal value) {
        return value.compareTo(expectedMin) < 0 || value.compareTo(expectedMax) > 0;
    }
}
