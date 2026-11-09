package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.config.PasswordRecoveryProperties;
import com.mentesvivas.backend.support.CapturedEmailSender;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordRecoveryEmailsTest {

    private final CapturedEmailSender sender = new CapturedEmailSender();
    private final PasswordRecoveryEmails emails = new PasswordRecoveryEmails(sender,
            new PasswordRecoveryProperties(Duration.ofMinutes(30), 5, 3, Duration.ofHours(1)));

    @Test
    void codeEmailHasCodeAndValidity() {
        emails.sendCode("ana@exemplo.com", "123456");

        assertThat(sender.sentEmails()).singleElement().satisfies(sent -> {
            assertThat(sent.recipient()).isEqualTo("ana@exemplo.com");
            assertThat(sent.body()).contains("123456").contains("30 minutos");
        });
        assertThat(sender.lastCode()).isEqualTo("123456");
    }

    @Test
    void passwordChangedNoticeHasNoCodeNorHealthData() {
        emails.notifyPasswordChanged("ana@exemplo.com");

        assertThat(sender.sentEmails()).singleElement().satisfies(sent -> {
            assertThat(sent.subject()).isEqualTo("Sua senha foi alterada");
            assertThat(sent.body()).doesNotContainPattern("\\d{6}");
        });
    }
}
