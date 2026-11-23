package com.mentesvivas.backend.dto.medication;

import com.mentesvivas.backend.entity.medication.DoseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CorrectDoseRequest(
        @NotNull(message = "informe o novo status da dose") DoseStatus status,
        @NotBlank(message = "informe a justificativa da correcao")
        @Size(max = 300, message = "a justificativa aceita ate 300 caracteres") String justification
) {
}
