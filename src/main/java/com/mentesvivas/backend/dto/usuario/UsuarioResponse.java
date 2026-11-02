package com.mentesvivas.backend.dto.usuario;

import com.mentesvivas.backend.entity.usuario.Role;

import java.util.UUID;

public record UsuarioResponse(
        UUID id,
        String nomeCompleto,
        String email,
        Role role
) {
}
