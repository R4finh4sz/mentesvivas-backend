package com.mentesvivas.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.recovery")
public record PasswordRecoveryProperties(
        Duration codeValidity,
        int maxAttempts,
        int maxSends,
        Duration sendsWindow
) {
}
