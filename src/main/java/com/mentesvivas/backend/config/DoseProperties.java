package com.mentesvivas.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.dose")
public record DoseProperties(int doubleClickSeconds) {
}
