package com.mentesvivas.backend.exception.auth;

public class InvalidCodeException extends RuntimeException {

    public InvalidCodeException() {
        super("Codigo invalido ou expirado. Solicite um novo codigo.");
    }
}
