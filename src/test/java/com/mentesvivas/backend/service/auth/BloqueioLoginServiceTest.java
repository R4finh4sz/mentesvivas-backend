package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.config.LoginBloqueioProperties;
import com.mentesvivas.backend.exception.auth.ContaBloqueadaException;
import com.mentesvivas.backend.security.auth.TokenHasher;
import com.mentesvivas.backend.support.MemoriaArmazenamentoTemporario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BloqueioLoginServiceTest {

    private static final String EMAIL = "ana@exemplo.com";

    private BloqueioLoginService service;

    @BeforeEach
    void preparar() {
        MemoriaArmazenamentoTemporario memoria = new MemoriaArmazenamentoTemporario();
        LoginBloqueioProperties properties = new LoginBloqueioProperties(3, Duration.ofMinutes(15), Duration.ofMinutes(10));
        service = new BloqueioLoginService(memoria, memoria, properties, new TokenHasher());
    }

    @Test
    void naoBloqueiaAbaixoDoLimite() {
        service.registrarFalha(EMAIL);
        service.registrarFalha(EMAIL);

        assertThatCode(() -> service.exigirDesbloqueado(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void bloqueiaAoAtingirOLimiteEInformaTempoRestante() {
        for (int i = 0; i < 3; i++) {
            service.registrarFalha(EMAIL);
        }

        assertThatThrownBy(() -> service.exigirDesbloqueado(EMAIL))
                .isInstanceOfSatisfying(ContaBloqueadaException.class, ex ->
                        assertThat(ex.getTempoRestante()).isPositive().isLessThanOrEqualTo(Duration.ofMinutes(10)));
    }

    @Test
    void bloqueioEhPorEmail() {
        for (int i = 0; i < 3; i++) {
            service.registrarFalha(EMAIL);
        }

        assertThatCode(() -> service.exigirDesbloqueado("outra@exemplo.com")).doesNotThrowAnyException();
    }

    @Test
    void sucessoZeraAContagem() {
        service.registrarFalha(EMAIL);
        service.registrarFalha(EMAIL);
        service.registrarSucesso(EMAIL);
        service.registrarFalha(EMAIL);
        service.registrarFalha(EMAIL);

        assertThatCode(() -> service.exigirDesbloqueado(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void liberarRemoveBloqueioAtivo() {
        for (int i = 0; i < 3; i++) {
            service.registrarFalha(EMAIL);
        }

        service.liberar(EMAIL);

        assertThatCode(() -> service.exigirDesbloqueado(EMAIL)).doesNotThrowAnyException();
    }
}
