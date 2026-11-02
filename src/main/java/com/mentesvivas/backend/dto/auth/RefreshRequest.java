package com.mentesvivas.backend.dto.auth;

import com.mentesvivas.backend.util.Normalizacao;
import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank String refreshToken
) {

    public RefreshRequest {
        refreshToken = Normalizacao.token(refreshToken);
    }
}
