package com.mentesvivas.backend.email;

public interface EmailSender {

    void send(String recipient, String subject, String htmlBody);
}
