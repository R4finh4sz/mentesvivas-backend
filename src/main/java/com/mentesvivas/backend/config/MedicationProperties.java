package com.mentesvivas.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.medication")
public record MedicationProperties(int criticalStockDays) {
}
