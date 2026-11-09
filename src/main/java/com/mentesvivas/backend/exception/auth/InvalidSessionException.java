package com.mentesvivas.backend.exception.auth;

public class InvalidSessionException extends AuthException {

    public InvalidSessionException() {
        super("Sessao invalida ou expirada. Faca login novamente.");
    }
}
