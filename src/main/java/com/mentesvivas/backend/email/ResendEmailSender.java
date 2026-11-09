package com.mentesvivas.backend.email;

import com.mentesvivas.backend.config.EmailProperties;
import com.mentesvivas.backend.exception.common.ServicoIndisponivelException;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("!'${app.email.api-key:}'.isEmpty()")
public class ResendEmailSender implements EmailSender {

    private final Resend resend;
    private final String remetente;

    public ResendEmailSender(EmailProperties properties) {
        this.resend = new Resend(properties.apiKey());
        this.remetente = properties.remetente();
    }

    @Override
    public void enviar(String destinatario, String assunto, String corpoHtml) {
        CreateEmailOptions options = CreateEmailOptions.builder()
                .from(remetente)
                .to(destinatario)
                .subject(assunto)
                .html(corpoHtml)
                .build();
        try {
            resend.emails().send(options);
        } catch (ResendException ex) {
            throw new ServicoIndisponivelException("Nao foi possivel enviar o e-mail agora. Tente novamente em instantes.", ex);
        }
    }
}
