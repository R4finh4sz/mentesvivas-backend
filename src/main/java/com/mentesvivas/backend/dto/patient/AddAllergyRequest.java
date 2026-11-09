package com.mentesvivas.backend.dto.patient;

import com.mentesvivas.backend.entity.patient.AllergyCategory;
import com.mentesvivas.backend.entity.patient.AllergySeverity;
import com.mentesvivas.backend.util.Normalization;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddAllergyRequest(
        @NotNull AllergyCategory category,
        @NotBlank @Size(max = 200) String description,
        @NotNull AllergySeverity severity
) {

    public AddAllergyRequest {
        description = Normalization.name(description);
    }
}
