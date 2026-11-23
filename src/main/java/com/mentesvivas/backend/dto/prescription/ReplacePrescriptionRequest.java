package com.mentesvivas.backend.dto.prescription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReplacePrescriptionRequest(
        @NotBlank(message = "informe a justificativa da substituicao")
        @Size(max = 500, message = "a justificativa aceita ate 500 caracteres") String justification
) {

    public ReplacePrescriptionRequest {
        justification = justification == null ? null : justification.strip();
    }
}
