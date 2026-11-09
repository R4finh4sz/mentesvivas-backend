package com.mentesvivas.backend.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnExpression("'${app.email.api-key:}'.isEmpty()")
public class DisabledEmailSender implements EmailSender {

    @Override
    public void send(String recipient, String subject, String htmlBody) {
        log.warn("E-mail sending disabled (RESEND_API_KEY missing). Ignored subject: {}", subject);
    }
}
