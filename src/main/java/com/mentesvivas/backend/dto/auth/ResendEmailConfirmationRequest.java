package com.mentesvivas.backend.dto.auth;

import com.mentesvivas.backend.util.Normalization;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResendEmailConfirmationRequest(
        @NotBlank @Email @Size(max = 254) String email
) {

    public ResendEmailConfirmationRequest {
        email = Normalization.email(email);
    }
}
