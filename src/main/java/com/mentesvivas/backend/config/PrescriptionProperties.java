package com.mentesvivas.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.prescription")
public record PrescriptionProperties(
        int validityDays,
        int expiryNoticeDays,
        boolean allergyOverrideAllowed,
        String verificationBaseUrl
) {
}
