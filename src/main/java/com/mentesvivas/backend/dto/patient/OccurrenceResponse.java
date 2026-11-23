package com.mentesvivas.backend.dto.patient;

import com.mentesvivas.backend.entity.patient.MeasurementSource;
import com.mentesvivas.backend.entity.patient.OccurrenceSeverity;
import com.mentesvivas.backend.entity.patient.OccurrenceType;

import java.time.Instant;
import java.util.UUID;

public record OccurrenceResponse(
        UUID id,
        OccurrenceType type,
        OccurrenceSeverity severity,
        String description,
        boolean needsHospital,
        MeasurementSource source,
        UUID recordedBy,
        Instant occurredAt
) {
}
