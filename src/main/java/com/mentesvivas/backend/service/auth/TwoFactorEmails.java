package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.config.TwoFactorProperties;
import com.mentesvivas.backend.email.EmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TwoFactorEmails {

    private final EmailSender emailSender;
    private final TwoFactorProperties properties;

    public void sendCode(String recipient, String code) {
        long minutes = properties.codeValidity().toMinutes();
        emailSender.send(recipient, "Seu codigo de acesso",
                "<p>Seu codigo de verificacao em duas etapas e <strong>%s</strong>.</p><p>Ele vale por %d minutos. Se voce nao tentou entrar, altere sua senha.</p>"
                        .formatted(code, minutes));
    }
}
