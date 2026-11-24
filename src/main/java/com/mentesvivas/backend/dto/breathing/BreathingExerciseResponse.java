package com.mentesvivas.backend.dto.breathing;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BreathingExerciseResponse(
        UUID id, String title, int inhaleSeconds, int holdSeconds, int exhaleSeconds, int cycles
) {
    public record Validated(BreathingExerciseResponse exercise, UUID validatedBy, Instant validatedAt,
                            boolean published) {}

    public record Catalog(List<BreathingExerciseResponse> exercises, String startNotice) {}

    public record Start(BreathingExerciseResponse exercise, String notice) {}
}
