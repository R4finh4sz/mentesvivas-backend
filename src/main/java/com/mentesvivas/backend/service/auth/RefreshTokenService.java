package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.config.JwtProperties;
import com.mentesvivas.backend.entity.auth.RefreshToken;
import com.mentesvivas.backend.entity.usuario.Usuario;
import com.mentesvivas.backend.exception.auth.SessaoInvalidaException;
import com.mentesvivas.backend.repository.auth.RefreshTokenRepository;
import com.mentesvivas.backend.security.auth.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final SecureRandom secureRandom = new SecureRandom();
    private final JwtProperties properties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHasher tokenHasher;

    @Transactional
    public String criar(Usuario usuario) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String valor = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUsuario(usuario);
        refreshToken.setTokenHash(tokenHasher.hash(valor));
        refreshToken.setExpiraEm(Instant.now().plus(properties.refreshTokenExpiration()));
        refreshTokenRepository.save(refreshToken);
        return valor;
    }

    @Transactional(noRollbackFor = SessaoInvalidaException.class)
    public Usuario consumir(String valor) {
        Instant agora = Instant.now();
        RefreshToken atual = refreshTokenRepository.findByTokenHash(tokenHasher.hash(valor))
                .orElseThrow(SessaoInvalidaException::new);

        if (atual.getRevogadoEm() != null) {
            refreshTokenRepository.revogarTodosDoUsuario(atual.getUsuario().getId(), agora);
            throw new SessaoInvalidaException();
        }
        if (!atual.isAtivo(agora)) {
            throw new SessaoInvalidaException();
        }

        atual.setRevogadoEm(agora);
        return atual.getUsuario();
    }
}
