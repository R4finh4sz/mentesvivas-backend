package com.mentesvivas.backend.dto.notification;

import com.mentesvivas.backend.entity.notification.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        UUID referenceId,
        String message,
        Instant createdAt,
        boolean read
) {
}
