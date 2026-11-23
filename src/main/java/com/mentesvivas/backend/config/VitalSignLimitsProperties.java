package com.mentesvivas.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties("app.vital-signs")
public record VitalSignLimitsProperties(
        Range systolic,
        Range diastolic,
        Range heartRate,
        Range glucose,
        Range temperature,
        Range oxygenSaturation
) {

    public record Range(BigDecimal min, BigDecimal max) {

        public boolean contains(BigDecimal value) {
            return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
        }
    }
}
