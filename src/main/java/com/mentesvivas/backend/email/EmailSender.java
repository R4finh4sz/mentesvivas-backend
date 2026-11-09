package com.mentesvivas.backend.email;

public interface EmailSender {

    void enviar(String destinatario, String assunto, String corpoHtml);
}
