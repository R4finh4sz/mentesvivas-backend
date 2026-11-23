package com.mentesvivas.backend.dto.patient;

import com.mentesvivas.backend.entity.patient.AppointmentType;

import java.time.Instant;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        AppointmentType type,
        String title,
        String location,
        String notes,
        Instant scheduledAt,
        Instant remindAt,
        UUID createdBy
) {
}
