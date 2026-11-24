package com.mentesvivas.backend.dto.notification;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record NotificationPreferences(
        @NotNull Boolean sundownEnabled,
        @NotNull Boolean medicationEnabled,
        @NotNull Boolean geofenceEnabled,
        LocalTime sundownTime
) {
    private static final LocalTime DEFAULT_SUNDOWN = LocalTime.of(18, 0);

    public NotificationPreferences {
        if (sundownTime == null) {
            sundownTime = DEFAULT_SUNDOWN;
        }
    }

    public static NotificationPreferences defaults() {
        return new NotificationPreferences(true, true, true, DEFAULT_SUNDOWN);
    }
}
