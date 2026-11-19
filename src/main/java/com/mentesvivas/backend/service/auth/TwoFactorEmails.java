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

    public void sendMethodChangedNotice(String recipient) {
        emailSender.send(recipient, "Seu segundo fator foi alterado",
                "<p>O segundo fator da sua conta voltou a ser o codigo enviado por e-mail, depois de uma recuperacao com e-mail e senha.</p>"
                        + "<p>Se nao foi voce, altere sua senha imediatamente.</p>");
    }

    public void sendCode(String recipient, String code) {
        long minutes = properties.codeValidity().toMinutes();
        emailSender.send(recipient, "Seu codigo de acesso",
                "<p>Seu codigo de verificacao em duas etapas e <strong>%s</strong>.</p><p>Ele vale por %d minutos. Se voce nao tentou entrar, altere sua senha.</p>"
                        .formatted(code, minutes));
    }
}
