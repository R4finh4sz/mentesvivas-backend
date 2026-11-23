package com.mentesvivas.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;

@ConfigurationProperties("app.hydration")
public record HydrationProperties(
        BigDecimal mlPerKgMin,
        BigDecimal mlPerKgMax,
        ZoneId zone,
        List<String> renalKeywords,
        List<String> cardiacKeywords,
        List<String> consultKeywords
) {
}
