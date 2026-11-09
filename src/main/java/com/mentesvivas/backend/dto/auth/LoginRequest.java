package com.mentesvivas.backend.dto.auth;

import com.mentesvivas.backend.util.Normalization;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password
) {

    public LoginRequest {
        email = Normalization.email(email);
    }
}
