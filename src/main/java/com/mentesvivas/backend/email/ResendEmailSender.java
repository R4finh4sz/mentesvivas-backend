package com.mentesvivas.backend.email;

import com.mentesvivas.backend.config.EmailProperties;
import com.mentesvivas.backend.exception.common.ServiceUnavailableException;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("!'${app.email.api-key:}'.isEmpty()")
public class ResendEmailSender implements EmailSender {

    private final Resend resend;
    private final String fromAddress;

    public ResendEmailSender(EmailProperties properties) {
        this.resend = new Resend(properties.apiKey());
        this.fromAddress = properties.fromAddress();
    }

    @Override
    public void send(String recipient, String subject, String htmlBody) {
        CreateEmailOptions options = CreateEmailOptions.builder()
                .from(fromAddress)
                .to(recipient)
                .subject(subject)
                .html(htmlBody)
                .build();
        try {
            resend.emails().send(options);
        } catch (ResendException ex) {
            throw new ServiceUnavailableException("Nao foi possivel enviar o e-mail agora. Tente novamente em instantes.", ex);
        }
    }
}
