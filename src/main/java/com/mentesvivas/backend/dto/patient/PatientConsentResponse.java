package com.mentesvivas.backend.dto.patient;

import com.mentesvivas.backend.entity.patient.ConsentScope;

import java.time.Instant;

public record PatientConsentResponse(ConsentScope scope, boolean granted, boolean decidedByPatient, Instant decidedAt) {}
