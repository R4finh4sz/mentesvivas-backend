package com.mentesvivas.backend.dto.auth;

import com.mentesvivas.backend.util.Normalizacao;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitarRecuperacaoRequest(
        @NotBlank @Email @Size(max = 254) String email
) {

    public SolicitarRecuperacaoRequest {
        email = Normalizacao.email(email);
    }
}
