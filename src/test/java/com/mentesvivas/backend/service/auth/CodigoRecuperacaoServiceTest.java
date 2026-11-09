package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.config.RecuperacaoSenhaProperties;
import com.mentesvivas.backend.exception.auth.CodigoRecuperacaoInvalidoException;
import com.mentesvivas.backend.security.auth.TokenHasher;
import com.mentesvivas.backend.support.MemoriaArmazenamentoTemporario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodigoRecuperacaoServiceTest {

    private static final String EMAIL = "ana@exemplo.com";

    private MemoriaArmazenamentoTemporario memoria;
    private CodigoRecuperacaoService service;

    @BeforeEach
    void preparar() {
        memoria = new MemoriaArmazenamentoTemporario();
        RecuperacaoSenhaProperties properties = new RecuperacaoSenhaProperties(Duration.ofMinutes(30), 3, 2, Duration.ofHours(1));
        service = new CodigoRecuperacaoService(memoria, memoria, properties, new TokenHasher());
    }

    @Test
    void geraCodigoDeSeisDigitos() {
        String codigo = service.gerar(EMAIL).orElseThrow();

        assertThat(codigo).matches("\\d{6}");
    }

    @Test
    void naoGuardaOCodigoEmTextoAberto() {
        String codigo = service.gerar(EMAIL).orElseThrow();

        assertThat(memoria.todosOsValores()).noneMatch(valor -> valor.contains(codigo));
    }

    @Test
    void codigoCorretoEhAceitoUmaUnicaVez() {
        String codigo = service.gerar(EMAIL).orElseThrow();

        assertThatCode(() -> service.validarEConsumir(EMAIL, codigo)).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.validarEConsumir(EMAIL, codigo))
                .isInstanceOf(CodigoRecuperacaoInvalidoException.class);
    }

    @Test
    void codigoDeOutroEmailEhRecusado() {
        String codigo = service.gerar(EMAIL).orElseThrow();

        assertThatThrownBy(() -> service.validarEConsumir("outra@exemplo.com", codigo))
                .isInstanceOf(CodigoRecuperacaoInvalidoException.class);
    }

    @Test
    void semCodigoGeradoEhRecusado() {
        assertThatThrownBy(() -> service.validarEConsumir(EMAIL, "123456"))
                .isInstanceOf(CodigoRecuperacaoInvalidoException.class);
    }

    @Test
    void estourarTentativasInvalidaOCodigoCorreto() {
        String codigo = service.gerar(EMAIL).orElseThrow();
        String errado = codigo.equals("000000") ? "000001" : "000000";
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> service.validarEConsumir(EMAIL, errado))
                    .isInstanceOf(CodigoRecuperacaoInvalidoException.class);
        }

        assertThatThrownBy(() -> service.validarEConsumir(EMAIL, codigo))
                .isInstanceOf(CodigoRecuperacaoInvalidoException.class);
    }

    @Test
    void limitaOsEnviosNaJanela() {
        assertThat(service.gerar(EMAIL)).isPresent();
        assertThat(service.gerar(EMAIL)).isPresent();
        assertThat(service.gerar(EMAIL)).isEmpty();
    }

    @Test
    void novoCodigoSubstituiOAnterior() {
        String primeiro = service.gerar(EMAIL).orElseThrow();
        String segundo = service.gerar(EMAIL).orElseThrow();

        if (!primeiro.equals(segundo)) {
            assertThatThrownBy(() -> service.validarEConsumir(EMAIL, primeiro))
                    .isInstanceOf(CodigoRecuperacaoInvalidoException.class);
        }
        assertThatCode(() -> service.validarEConsumir(EMAIL, segundo)).doesNotThrowAnyException();
    }
}
