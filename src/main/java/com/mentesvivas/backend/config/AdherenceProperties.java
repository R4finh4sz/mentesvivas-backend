package com.mentesvivas.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.adherence")
public record AdherenceProperties(int periodDays, int thresholdPercent) {
}
