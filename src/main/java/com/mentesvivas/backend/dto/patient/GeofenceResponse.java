package com.mentesvivas.backend.dto.patient;

public record GeofenceResponse(double latitude, double longitude, int radiusMeters, int readingIntervalSeconds,
                               String notice) {}
