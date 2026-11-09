package com.mentesvivas.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.cep")
public record CepProperties(
        String baseUrl,
        Duration timeout
) {
}
