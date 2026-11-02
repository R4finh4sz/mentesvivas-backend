package com.mentesvivas.backend.dto.auth;

import com.mentesvivas.backend.dto.usuario.UsuarioResponse;
import com.mentesvivas.backend.entity.usuario.Role;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        long refreshExpiresIn,
        Role role,
        boolean termosAceitos,
        UsuarioResponse usuario
) {
}
