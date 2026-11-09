package com.mentesvivas.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.email-confirmation")
public record EmailConfirmationProperties(
        Duration codeValidity,
        int maxAttempts,
        int maxSends,
        Duration sendsWindow
) {
}
