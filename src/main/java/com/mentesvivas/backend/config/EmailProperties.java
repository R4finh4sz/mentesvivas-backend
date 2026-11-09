package com.mentesvivas.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.email")
public record EmailProperties(
        String apiKey,
        String fromAddress
) {
}
