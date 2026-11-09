package com.mentesvivas.backend.dto.patient;

import com.mentesvivas.backend.entity.patient.ChangeType;
import com.mentesvivas.backend.entity.patient.SensitiveField;

import java.time.Instant;
import java.util.UUID;

public record SensitiveChangeResponse(
        SensitiveField field,
        ChangeType changeType,
        String summary,
        UUID changedBy,
        Instant changedAt
) {
}
