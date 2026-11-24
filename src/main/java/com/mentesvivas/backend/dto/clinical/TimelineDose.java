package com.mentesvivas.backend.dto.clinical;

import com.mentesvivas.backend.entity.medication.DoseStatus;

import java.time.Instant;

public record TimelineDose(String medication, DoseStatus status, Instant scheduledFor, Instant recordedAt) {}
