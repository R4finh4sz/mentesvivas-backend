package com.mentesvivas.backend.dto.admin;

import com.mentesvivas.backend.util.Normalization;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountStatusRequest(
        @NotBlank @Size(max = 500) String justification
) {

    public AccountStatusRequest {
        justification = Normalization.name(justification);
    }
}
