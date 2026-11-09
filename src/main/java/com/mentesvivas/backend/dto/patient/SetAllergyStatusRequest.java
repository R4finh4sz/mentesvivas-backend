package com.mentesvivas.backend.dto.patient;

import com.mentesvivas.backend.entity.patient.AllergyStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record SetAllergyStatusRequest(
        @NotNull AllergyStatus status
) {

    @AssertTrue(message = "o estado 'informada' e definido ao cadastrar alergias")
    public boolean isStatusSettable() {
        return status != AllergyStatus.INFORMED;
    }
}
