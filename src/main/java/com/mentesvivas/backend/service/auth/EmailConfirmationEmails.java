package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.config.EmailConfirmationProperties;
import com.mentesvivas.backend.email.EmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailConfirmationEmails {

    private final EmailSender emailSender;
    private final EmailConfirmationProperties properties;

    public void sendCode(String recipient, String code) {
        long minutes = properties.codeValidity().toMinutes();
        emailSender.send(recipient, "Confirme seu e-mail",
                "<p>Seu codigo de confirmacao e <strong>%s</strong>.</p><p>Ele vale por %d minutos. Se voce nao criou uma conta, ignore esta mensagem.</p>"
                        .formatted(code, minutes));
    }
}
