package com.mentesvivas.backend.dto.medication;

import com.mentesvivas.backend.entity.medication.DoseRecord;
import com.mentesvivas.backend.entity.medication.DoseStatus;

import java.time.Instant;
import java.util.UUID;

public record DoseResponse(
        UUID id,
        UUID medicationId,
        Instant scheduledFor,
        DoseStatus status,
        boolean duplicate,
        boolean corrected,
        UUID recordedBy,
        Instant recordedAt
) {

    public static DoseResponse from(DoseRecord record) {
        return new DoseResponse(record.getId(), record.getMedicationId(), record.getScheduledFor(),
                record.getStatus(), record.isDuplicate(), record.isCorrected(), record.getRecordedBy(),
                record.getRecordedAt());
    }
}
