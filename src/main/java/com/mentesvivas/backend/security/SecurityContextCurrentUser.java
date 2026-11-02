package com.mentesvivas.backend.security;

import com.mentesvivas.backend.entity.usuario.Usuario;
import com.mentesvivas.backend.exception.auth.SessaoInvalidaException;
import com.mentesvivas.backend.exception.auth.UsuarioNaoAutenticadoException;
import com.mentesvivas.backend.repository.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SecurityContextCurrentUser implements CurrentUser {

    private final UsuarioRepository usuarioRepository;

    @Override
    public Usuario get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UsuarioNaoAutenticadoException();
        }
        return usuarioRepository.findById(idDe(authentication))
                .orElseThrow(SessaoInvalidaException::new);
    }

    private UUID idDe(Authentication authentication) {
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ex) {
            throw new SessaoInvalidaException();
        }
    }
}
