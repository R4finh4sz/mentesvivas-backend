package com.mentesvivas.backend.support;

import com.mentesvivas.backend.email.EmailSender;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Primary
public class CapturedEmailSender implements EmailSender {

    public record SentEmail(String recipient, String subject, String body) {
    }

    private static final Pattern CODE = Pattern.compile("<strong>(\\d{6})</strong>");

    private final List<SentEmail> sentEmails = new CopyOnWriteArrayList<>();

    @Override
    public void send(String recipient, String subject, String htmlBody) {
        sentEmails.add(new SentEmail(recipient, subject, htmlBody));
    }

    public List<SentEmail> sentEmails() {
        return sentEmails;
    }

    public String lastCode() {
        Matcher matcher = CODE.matcher(sentEmails.get(sentEmails.size() - 1).body());
        if (!matcher.find()) {
            throw new IllegalStateException("Last e-mail does not contain a code");
        }
        return matcher.group(1);
    }

    public void clear() {
        sentEmails.clear();
    }
}
