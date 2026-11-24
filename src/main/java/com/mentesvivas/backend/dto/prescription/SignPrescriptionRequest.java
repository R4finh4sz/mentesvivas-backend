package com.mentesvivas.backend.dto.prescription;

import jakarta.validation.constraints.NotBlank;

public record SignPrescriptionRequest(
        @NotBlank(message = "informe a senha") String password,
        @NotBlank(message = "informe o codigo da segunda etapa") String code
) {
}
