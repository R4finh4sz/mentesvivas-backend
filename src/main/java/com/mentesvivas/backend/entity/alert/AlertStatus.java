package com.mentesvivas.backend.entity.alert;

public enum AlertStatus {
    CREATED,
    SEEN,
    ACKNOWLEDGED,
    RESOLVED;

    public boolean isBefore(AlertStatus other) {
        return ordinal() < other.ordinal();
    }

    public boolean isPending() {
        return this != RESOLVED;
    }
}
