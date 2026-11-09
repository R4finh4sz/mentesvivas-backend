package com.mentesvivas.backend.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnExpression("'${app.email.api-key:}'.isEmpty()")
public class EmailDesabilitadoSender implements EmailSender {

    @Override
    public void enviar(String destinatario, String assunto, String corpoHtml) {
        log.warn("Envio de e-mail desabilitado (RESEND_API_KEY ausente). Assunto ignorado: {}", assunto);
    }
}
