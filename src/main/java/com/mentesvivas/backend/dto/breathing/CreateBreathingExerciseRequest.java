package com.mentesvivas.backend.dto.breathing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateBreathingExerciseRequest(
        @NotBlank @Size(max = 100) String title,
        @Positive int inhaleSeconds,
        @PositiveOrZero int holdSeconds,
        @Positive int exhaleSeconds,
        @Positive int cycles
) {}
