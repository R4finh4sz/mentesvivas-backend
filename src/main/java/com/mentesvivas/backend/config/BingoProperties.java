package com.mentesvivas.backend.config;

import com.mentesvivas.backend.exception.config.InvalidConfigurationException;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.bingo")
public record BingoProperties(int roundsPerSession, int soundSeconds, int minimumSounds) {

    public BingoProperties {
        if (roundsPerSession < 1 || soundSeconds < 1 || minimumSounds < 1) {
            throw new InvalidConfigurationException("Os valores de app.bingo devem ser maiores que zero.");
        }
    }
}
