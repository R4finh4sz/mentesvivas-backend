package com.mentesvivas.backend.exception.auth;

public abstract class AutenticacaoException extends RuntimeException {

    protected AutenticacaoException(String message) {
        super(message);
    }
}
