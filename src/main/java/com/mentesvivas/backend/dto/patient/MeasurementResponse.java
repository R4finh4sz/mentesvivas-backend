package com.mentesvivas.backend.dto.patient;

import java.math.BigDecimal;
import java.time.Instant;

public record MeasurementResponse(
        BigDecimal weightKg,
        BigDecimal heightM,
        Instant measuredAt
) {
}
