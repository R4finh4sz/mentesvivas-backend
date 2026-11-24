package com.mentesvivas.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.signature")
public record SignatureProperties(Mode mode) {

    public enum Mode {
        UNAVAILABLE,
        SIMULATED
    }
}
