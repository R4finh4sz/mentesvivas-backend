package com.mentesvivas.backend.dto.auth;

import com.mentesvivas.backend.util.Normalizacao;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String email,
        @NotBlank String senha
) {

    public LoginRequest {
        email = Normalizacao.email(email);
    }
}
