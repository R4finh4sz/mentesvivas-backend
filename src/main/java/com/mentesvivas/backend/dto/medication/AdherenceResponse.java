package com.mentesvivas.backend.dto.medication;

import java.math.BigDecimal;
import java.time.Instant;

public record AdherenceResponse(
        Instant from,
        Instant to,
        int expectedDoses,
        int taken,
        int refused,
        int postponed,
        int notRegistered,
        BigDecimal adherencePercent,
        int thresholdPercent,
        boolean belowThreshold
) {
}
