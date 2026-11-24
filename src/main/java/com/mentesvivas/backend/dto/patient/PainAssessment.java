package com.mentesvivas.backend.dto.patient;

import com.mentesvivas.backend.entity.patient.PainScale;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class PainAssessment {

    private PainAssessment() {}

    public record Selection(PainScale scale, List<Integer> allowedScores, Instant selectedAt) {}

    public record Recorded(UUID id, PainScale scale, int score, UUID recordedBy, Instant recordedAt) {}
}
