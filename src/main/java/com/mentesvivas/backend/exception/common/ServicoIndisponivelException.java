package com.mentesvivas.backend.exception.common;

public class ServicoIndisponivelException extends RuntimeException {

    public ServicoIndisponivelException(String message, Throwable cause) {
        super(message, cause);
    }
}
