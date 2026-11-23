package com.mentesvivas.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties("app.exam")
public record ExamProperties(DataSize maxFileSize) {
}
