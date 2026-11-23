package com.mentesvivas.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.timeline")
public record TimelineProperties(int defaultDays, int maxDays) {
}
