package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.config.RecuperacaoSenhaProperties;
import com.mentesvivas.backend.dto.auth.RecuperacaoSolicitadaResponse;
import com.mentesvivas.backend.dto.auth.RedefinirSenhaRequest;
import com.mentesvivas.backend.exception.auth.CodigoRecuperacaoInvalidoException;
import com.mentesvivas.backend.repository.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecuperacaoSenhaService {

    private static final String MENSAGEM_SOLICITACAO =
            "Se o e-mail estiver cadastrado, enviaremos um codigo de verificacao.";

    private final UsuarioRepository usuarioRepository;
    private final CodigoRecuperacaoService codigoService;
    private final RecuperacaoSenhaEmails emails;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final BloqueioLoginService bloqueioLoginService;
    private final RecuperacaoSenhaProperties properties;

    public RecuperacaoSolicitadaResponse solicitar(String email) {
        usuarioRepository.findByEmail(email).ifPresent(usuario ->
                codigoService.gerar(email).ifPresent(codigo -> emails.enviarCodigo(email, codigo)));
        return new RecuperacaoSolicitadaResponse(MENSAGEM_SOLICITACAO, properties.validadeCodigo().toSeconds());
    }

    @Transactional
    public void redefinir(RedefinirSenhaRequest request) {
        codigoService.validarEConsumir(request.email(), request.codigo());
        var usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(CodigoRecuperacaoInvalidoException::new);

        usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        refreshTokenService.revogarTodos(usuario);
        bloqueioLoginService.liberar(request.email());
        emails.avisarSenhaAlterada(usuario.getEmail());
    }
}
