package com.mentesvivas.backend.entity.notification;

import com.mentesvivas.backend.dto.notification.NotificationPreferences;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "caregiver_notification_preferences")
@NoArgsConstructor
public class CaregiverNotificationPreferences {

    @Id
    private UUID caregiverId;

    @Column(nullable = false)
    private boolean sundownEnabled;

    @Column(nullable = false)
    private boolean medicationEnabled;

    @Column(nullable = false)
    private boolean geofenceEnabled;
    @Column(nullable = false)
    private LocalTime sundownTime;

    public CaregiverNotificationPreferences(UUID caregiverId, NotificationPreferences preferences) {
        this.caregiverId = caregiverId;
        this.sundownEnabled = preferences.sundownEnabled();
        this.medicationEnabled = preferences.medicationEnabled();
        this.geofenceEnabled = preferences.geofenceEnabled();
        this.sundownTime = preferences.sundownTime();
    }

    public NotificationPreferences view() {
        return new NotificationPreferences(sundownEnabled, medicationEnabled, geofenceEnabled, sundownTime);
    }
}
