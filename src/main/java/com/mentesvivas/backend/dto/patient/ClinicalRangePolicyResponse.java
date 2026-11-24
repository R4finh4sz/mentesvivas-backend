package com.mentesvivas.backend.dto.patient;

import com.mentesvivas.backend.entity.patient.ClinicalRange;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ClinicalRangePolicyResponse(UUID id, Instant approvedAt, List<ClinicalRange> ranges, String notice) {}
