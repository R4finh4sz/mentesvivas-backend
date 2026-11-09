package com.mentesvivas.backend.config;

import com.mentesvivas.backend.service.code.CodePolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CodePolicyConfig {

    public static final String PASSWORD_RECOVERY = "passwordRecoveryPolicy";
    public static final String EMAIL_CONFIRMATION = "emailConfirmationPolicy";

    @Bean(PASSWORD_RECOVERY)
    CodePolicy passwordRecoveryPolicy(PasswordRecoveryProperties properties) {
        return new CodePolicy("recovery", properties.codeValidity(), properties.maxAttempts(),
                properties.maxSends(), properties.sendsWindow());
    }

    @Bean(EMAIL_CONFIRMATION)
    CodePolicy emailConfirmationPolicy(EmailConfirmationProperties properties) {
        return new CodePolicy("email-confirmation", properties.codeValidity(), properties.maxAttempts(),
                properties.maxSends(), properties.sendsWindow());
    }
}
