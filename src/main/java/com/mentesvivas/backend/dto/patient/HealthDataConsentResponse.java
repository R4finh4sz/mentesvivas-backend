package com.mentesvivas.backend.dto.patient;

import java.time.Instant;

public record HealthDataConsentResponse(boolean granted, Instant acceptedAt, Instant revokedAt) {}
