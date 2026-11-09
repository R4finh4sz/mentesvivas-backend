package com.mentesvivas.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.login")
public record LoginBloqueioProperties(
        int maxTentativas,
        Duration janelaTentativas,
        Duration duracaoBloqueio
) {
}
