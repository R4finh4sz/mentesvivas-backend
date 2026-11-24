CREATE TABLE caregiver_notification_preferences (
    caregiver_id       UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    sundown_enabled    BOOLEAN NOT NULL,
    medication_enabled BOOLEAN NOT NULL,
    geofence_enabled   BOOLEAN NOT NULL,
    sundown_time       TIME WITHOUT TIME ZONE NOT NULL
);
