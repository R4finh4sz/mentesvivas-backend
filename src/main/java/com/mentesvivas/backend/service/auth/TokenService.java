package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.config.JwtProperties;
import com.mentesvivas.backend.dto.auth.TokenResponse;
import com.mentesvivas.backend.entity.usuario.Usuario;
import com.mentesvivas.backend.exception.auth.SessaoInvalidaException;
import com.mentesvivas.backend.mapper.usuario.UsuarioMapper;
import com.mentesvivas.backend.service.termos.AceiteTermosService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TokenService {

    private static final String TOKEN_TYPE = "Bearer";

    private final AccessTokenService accessTokenService;
    private final RefreshTokenService refreshTokenService;
    private final AceiteTermosService aceiteTermosService;
    private final JwtProperties properties;

    @Transactional
    public TokenResponse emitir(Usuario usuario) {
        return new TokenResponse(
                accessTokenService.gerar(usuario),
                refreshTokenService.criar(usuario),
                TOKEN_TYPE,
                properties.accessTokenExpiration().toSeconds(),
                properties.refreshTokenExpiration().toSeconds(),
                usuario.getRole(),
                aceiteTermosService.aceitouVersaoAtual(usuario),
                UsuarioMapper.toResponse(usuario));
    }

    @Transactional(noRollbackFor = SessaoInvalidaException.class)
    public TokenResponse renovar(String refreshToken) {
        return emitir(refreshTokenService.consumir(refreshToken));
    }
}
