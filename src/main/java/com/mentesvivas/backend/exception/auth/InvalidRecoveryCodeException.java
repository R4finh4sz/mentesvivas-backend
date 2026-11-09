package com.mentesvivas.backend.exception.auth;

public class InvalidRecoveryCodeException extends RuntimeException {

    public InvalidRecoveryCodeException() {
        super("Codigo invalido ou expirado. Solicite um novo codigo.");
    }
}
