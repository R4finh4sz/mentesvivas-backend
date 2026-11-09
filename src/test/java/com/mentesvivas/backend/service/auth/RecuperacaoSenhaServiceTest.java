package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.config.RecuperacaoSenhaProperties;
import com.mentesvivas.backend.dto.auth.RedefinirSenhaRequest;
import com.mentesvivas.backend.entity.usuario.Usuario;
import com.mentesvivas.backend.exception.auth.CodigoRecuperacaoInvalidoException;
import com.mentesvivas.backend.repository.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecuperacaoSenhaServiceTest {

    private static final String EMAIL = "ana@exemplo.com";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CodigoRecuperacaoService codigoService;
    @Mock
    private RecuperacaoSenhaEmails emails;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private BloqueioLoginService bloqueioLoginService;

    private RecuperacaoSenhaService service;

    @BeforeEach
    void preparar() {
        RecuperacaoSenhaProperties properties = new RecuperacaoSenhaProperties(Duration.ofMinutes(30), 5, 3, Duration.ofHours(1));
        service = new RecuperacaoSenhaService(usuarioRepository, codigoService, emails, passwordEncoder,
                refreshTokenService, bloqueioLoginService, properties);
    }

    @Test
    void solicitarParaEmailInexistenteNaoGeraCodigoNemEnviaEmail() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        var resposta = service.solicitar(EMAIL);

        assertThat(resposta.codigoExpiraEmSegundos()).isEqualTo(1800);
        verifyNoInteractions(codigoService, emails);
    }

    @Test
    void solicitarParaEmailCadastradoEnviaOCodigo() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(new Usuario()));
        when(codigoService.gerar(EMAIL)).thenReturn(Optional.of("123456"));

        service.solicitar(EMAIL);

        verify(emails).enviarCodigo(EMAIL, "123456");
    }

    @Test
    void solicitarAcimaDoLimiteDeEnviosNaoEnviaEmail() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(new Usuario()));
        when(codigoService.gerar(EMAIL)).thenReturn(Optional.empty());

        service.solicitar(EMAIL);

        verify(emails, never()).enviarCodigo(anyString(), anyString());
    }

    @Test
    void redefinirTrocaSenhaRevogaSessoesLiberaBloqueioEAvisa() {
        Usuario usuario = new Usuario();
        usuario.setEmail(EMAIL);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("novaSenha99")).thenReturn("hash-novo");

        service.redefinir(new RedefinirSenhaRequest(EMAIL, "123456", "novaSenha99", "novaSenha99"));

        assertThat(usuario.getSenhaHash()).isEqualTo("hash-novo");
        verify(codigoService).validarEConsumir(EMAIL, "123456");
        verify(refreshTokenService).revogarTodos(usuario);
        verify(bloqueioLoginService).liberar(EMAIL);
        verify(emails).avisarSenhaAlterada(EMAIL);
    }

    @Test
    void redefinirComCodigoInvalidoNaoAlteraNada() {
        doThrow(new CodigoRecuperacaoInvalidoException()).when(codigoService).validarEConsumir(anyString(), anyString());

        assertThatThrownBy(() -> service.redefinir(new RedefinirSenhaRequest(EMAIL, "123456", "novaSenha99", "novaSenha99")))
                .isInstanceOf(CodigoRecuperacaoInvalidoException.class);

        verifyNoInteractions(passwordEncoder, refreshTokenService, bloqueioLoginService, emails);
        verify(usuarioRepository, never()).findByEmail(any());
    }
}
