package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.dto.auth.LoginRequest;
import com.mentesvivas.backend.dto.auth.TokenResponse;
import com.mentesvivas.backend.entity.usuario.Usuario;
import com.mentesvivas.backend.exception.auth.CredenciaisInvalidasException;
import com.mentesvivas.backend.repository.usuario.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class LoginService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final BloqueioLoginService bloqueioLoginService;
    private final String hashFicticio;

    public LoginService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                        TokenService tokenService, BloqueioLoginService bloqueioLoginService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.bloqueioLoginService = bloqueioLoginService;
        this.hashFicticio = passwordEncoder.encode("senha-ficticia-para-tempo-constante");
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        bloqueioLoginService.exigirDesbloqueado(request.email());
        Optional<Usuario> usuario = usuarioRepository.findByEmail(request.email());

        String hash = usuario.map(Usuario::getSenhaHash).orElse(hashFicticio);
        boolean senhaConfere = passwordEncoder.matches(request.senha(), hash);
        if (usuario.isEmpty() || !senhaConfere) {
            bloqueioLoginService.registrarFalha(request.email());
            throw new CredenciaisInvalidasException();
        }
        bloqueioLoginService.registrarSucesso(request.email());
        return tokenService.emitir(usuario.get());
    }
}
