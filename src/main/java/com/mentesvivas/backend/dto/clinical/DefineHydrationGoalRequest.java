package com.mentesvivas.backend.dto.clinical;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DefineHydrationGoalRequest(
        @NotNull(message = "informe a meta em ml")
        @Min(value = 100, message = "a meta minima e 100 ml")
        @Max(value = 10000, message = "a meta maxima e 10000 ml") Integer goalMl
) {
}
