package com.mentesvivas.backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(HydrationProperties.class)
public class HydrationConfig {
}
