package com.mentesvivas.backend.service.usuario;

import com.mentesvivas.backend.exception.usuario.CpfJaCadastradoException;
import com.mentesvivas.backend.exception.usuario.EmailJaCadastradoException;
import com.mentesvivas.backend.repository.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UsuarioUnicidadeValidator {

    private final UsuarioRepository usuarioRepository;

    public void validar(String email, String cpf) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new EmailJaCadastradoException();
        }
        if (usuarioRepository.existsByCpf(cpf)) {
            throw new CpfJaCadastradoException();
        }
    }
}
