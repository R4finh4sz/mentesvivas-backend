package com.mentesvivas.backend.mapper.usuario;

import com.mentesvivas.backend.dto.usuario.UsuarioResponse;
import com.mentesvivas.backend.entity.usuario.Usuario;

public final class UsuarioMapper {

    private UsuarioMapper() {
    }

    public static UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNomeCompleto(), usuario.getEmail(), usuario.getRole());
    }
}
