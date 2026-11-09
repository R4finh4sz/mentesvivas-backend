package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.config.RecuperacaoSenhaProperties;
import com.mentesvivas.backend.email.EmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecuperacaoSenhaEmails {

    private final EmailSender emailSender;
    private final RecuperacaoSenhaProperties properties;

    public void enviarCodigo(String destinatario, String codigo) {
        long minutos = properties.validadeCodigo().toMinutes();
        emailSender.enviar(destinatario, "Seu codigo de recuperacao de senha",
                "<p>Seu codigo de verificacao e <strong>%s</strong>.</p><p>Ele vale por %d minutos. Se voce nao pediu a recuperacao, ignore esta mensagem.</p>"
                        .formatted(codigo, minutos));
    }

    public void avisarSenhaAlterada(String destinatario) {
        emailSender.enviar(destinatario, "Sua senha foi alterada",
                "<p>A senha da sua conta foi alterada e as demais sessoes foram encerradas.</p><p>Se nao foi voce, solicite a recuperacao de senha imediatamente.</p>");
    }
}
