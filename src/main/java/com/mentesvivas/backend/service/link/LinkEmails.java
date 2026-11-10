package com.mentesvivas.backend.service.link;

import com.mentesvivas.backend.email.EmailSender;
import com.mentesvivas.backend.exception.common.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LinkEmails {

    private final EmailSender emailSender;

    public void inviteRegistered(String recipient) {
        send(recipient, "Voce recebeu um convite",
                "<p>Voce recebeu um convite no MentesVivas.</p><p>Abra o aplicativo e confira a aba de notificacoes para aceitar ou recusar.</p>");
    }

    public void inviteToRegister(String recipient) {
        send(recipient, "Convite para o MentesVivas",
                "<p>Alguem convidou voce para o MentesVivas.</p><p>Crie sua conta de paciente com este e-mail e confirme-o. O convite aparecera na aba de notificacoes do aplicativo.</p>");
    }

    private void send(String recipient, String subject, String body) {
        try {
            emailSender.send(recipient, subject, body);
        } catch (ServiceUnavailableException ex) {
            log.warn("Could not send the invitation e-mail; the invitation stays available in the notifications", ex);
        }
    }
}
