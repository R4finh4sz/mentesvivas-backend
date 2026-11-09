package com.mentesvivas.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties("app.measurements")
public record MeasurementProperties(Limits weight, Limits height) {

    public record Limits(
            BigDecimal expectedMin,
            BigDecimal expectedMax,
            BigDecimal possibleMin,
            BigDecimal possibleMax
    ) {
    }
}
