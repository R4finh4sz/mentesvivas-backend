package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.config.RecuperacaoSenhaProperties;
import com.mentesvivas.backend.support.EmailCapturadoSender;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RecuperacaoSenhaEmailsTest {

    private final EmailCapturadoSender sender = new EmailCapturadoSender();
    private final RecuperacaoSenhaEmails emails = new RecuperacaoSenhaEmails(sender,
            new RecuperacaoSenhaProperties(Duration.ofMinutes(30), 5, 3, Duration.ofHours(1)));

    @Test
    void emailDoCodigoTrazCodigoEValidade() {
        emails.enviarCodigo("ana@exemplo.com", "123456");

        assertThat(sender.enviados()).singleElement().satisfies(enviado -> {
            assertThat(enviado.destinatario()).isEqualTo("ana@exemplo.com");
            assertThat(enviado.corpo()).contains("123456").contains("30 minutos");
        });
        assertThat(sender.ultimoCodigo()).isEqualTo("123456");
    }

    @Test
    void avisoDeSenhaAlteradaNaoTrazCodigoNemDadosDeSaude() {
        emails.avisarSenhaAlterada("ana@exemplo.com");

        assertThat(sender.enviados()).singleElement().satisfies(enviado -> {
            assertThat(enviado.assunto()).isEqualTo("Sua senha foi alterada");
            assertThat(enviado.corpo()).doesNotContainPattern("\\d{6}");
        });
    }
}
