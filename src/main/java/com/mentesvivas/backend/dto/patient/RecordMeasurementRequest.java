package com.mentesvivas.backend.dto.patient;

import jakarta.validation.constraints.AssertTrue;

import java.math.BigDecimal;

public record RecordMeasurementRequest(
        BigDecimal weightKg,
        BigDecimal heightM,
        Boolean confirmOutOfRange
) {

    public boolean confirmed() {
        return Boolean.TRUE.equals(confirmOutOfRange);
    }

    @AssertTrue(message = "informe o peso, a altura ou ambos")
    public boolean isAnyValueInformed() {
        return weightKg != null || heightM != null;
    }
}
