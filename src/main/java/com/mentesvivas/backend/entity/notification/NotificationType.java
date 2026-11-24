package com.mentesvivas.backend.entity.notification;

public enum NotificationType {
    CAREGIVER_INVITATION,
    DOCTOR_LINK_REQUEST,
    DOCTOR_INVITATION,
    LINK_ACCEPTED,
    LINK_DECLINED,
    LINK_ENDED,
    SOS_ALERT,
    PRESCRIPTION_AVAILABLE,
    PRESCRIPTION_EXPIRING,
    BREATHING_STARTED,
    SUNDOWN_REMINDER,
    MEDICATION_REMINDER,
    GEOFENCE_NOTICE,
    CRITICAL_MEASUREMENT,
    GEOFENCE_EXIT,
    AUTONOMY_CHANGED,
    FALL_ALERT;

    public boolean isCritical() {
        return this == SOS_ALERT || this == CRITICAL_MEASUREMENT || this == GEOFENCE_EXIT || this == FALL_ALERT;
    }
}
