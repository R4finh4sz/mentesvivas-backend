package com.mentesvivas.backend.dto.patient;

import com.mentesvivas.backend.entity.patient.OccurrenceSeverity;

import java.time.Instant;
import java.util.UUID;

public record AgitationProtocolResponse(UUID id, OccurrenceSeverity severity, String instructions, Instant approvedAt) {}
