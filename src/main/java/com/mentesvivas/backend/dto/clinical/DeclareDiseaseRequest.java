package com.mentesvivas.backend.dto.clinical;

import com.mentesvivas.backend.util.Normalization;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeclareDiseaseRequest(
        @NotBlank @Size(max = 300) String disease
) {

    public DeclareDiseaseRequest {
        disease = Normalization.name(disease);
    }
}
