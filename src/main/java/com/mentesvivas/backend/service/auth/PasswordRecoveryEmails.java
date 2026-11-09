package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.config.PasswordRecoveryProperties;
import com.mentesvivas.backend.email.EmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordRecoveryEmails {

    private final EmailSender emailSender;
    private final PasswordRecoveryProperties properties;

    public void sendCode(String recipient, String code) {
        long minutes = properties.codeValidity().toMinutes();
        emailSender.send(recipient, "Seu codigo de recuperacao de senha",
                "<p>Seu codigo de verificacao e <strong>%s</strong>.</p><p>Ele vale por %d minutos. Se voce nao pediu a recuperacao, ignore esta mensagem.</p>"
                        .formatted(code, minutes));
    }

    public void notifyPasswordChanged(String recipient) {
        emailSender.send(recipient, "Sua senha foi alterada",
                "<p>A senha da sua conta foi alterada e as demais sessoes foram encerradas.</p><p>Se nao foi voce, solicite a recuperacao de senha imediatamente.</p>");
    }
}
