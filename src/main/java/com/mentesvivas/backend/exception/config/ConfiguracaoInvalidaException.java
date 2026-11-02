package com.mentesvivas.backend.exception.config;

public class ConfiguracaoInvalidaException extends RuntimeException {

    public ConfiguracaoInvalidaException(String message) {
        super(message);
    }

    public ConfiguracaoInvalidaException(String message, Throwable cause) {
        super(message, cause);
    }
}
