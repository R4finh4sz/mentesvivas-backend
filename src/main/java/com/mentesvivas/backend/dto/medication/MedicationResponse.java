package com.mentesvivas.backend.dto.medication;

import com.mentesvivas.backend.entity.medication.MedicationOrigin;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record MedicationResponse(
        UUID id,
        String name,
        String strength,
        BigDecimal unitsPerDose,
        List<LocalTime> times,
        BigDecimal stockQuantity,
        int stockDaysLeft,
        boolean stockCritical,
        MedicationOrigin origin,
        boolean active,
        UUID createdBy,
        Instant createdAt,
        List<MedicationWarning> warnings
) {
}
