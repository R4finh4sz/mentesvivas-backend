package com.mentesvivas.backend.dto.alert;

import com.mentesvivas.backend.entity.alert.AlertStatus;
import com.mentesvivas.backend.entity.notification.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record AlertResponse(
        UUID id,
        UUID patientId,
        AlertStatus status,
        Instant createdAt,
        Instant seenAt,
        UUID seenBy,
        Instant acknowledgedAt,
        UUID acknowledgedBy,
        Instant resolvedAt,
        UUID resolvedBy,
        String notice,
        NotificationType type,
        UUID sourceId,
        UUID createdBy
) {
}
