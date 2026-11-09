package com.mentesvivas.backend.dto.patient;

import com.mentesvivas.backend.entity.patient.AllergyCategory;
import com.mentesvivas.backend.entity.patient.AllergySeverity;

import java.time.Instant;
import java.util.UUID;

public record AllergyResponse(
        UUID id,
        AllergyCategory category,
        String description,
        AllergySeverity severity,
        Instant createdAt
) {
}
