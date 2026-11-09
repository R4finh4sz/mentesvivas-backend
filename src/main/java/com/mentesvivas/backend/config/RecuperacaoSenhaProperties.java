package com.mentesvivas.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.recuperacao")
public record RecuperacaoSenhaProperties(
        Duration validadeCodigo,
        int maxTentativas,
        int maxEnvios,
        Duration janelaEnvios
) {
}
