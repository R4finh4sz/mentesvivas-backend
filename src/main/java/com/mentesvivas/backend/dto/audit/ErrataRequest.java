package com.mentesvivas.backend.dto.audit;

import com.mentesvivas.backend.util.Normalization;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ErrataRequest(
        @NotBlank @Size(max = 500) String justification
) {

    public ErrataRequest {
        justification = Normalization.name(justification);
    }
}
