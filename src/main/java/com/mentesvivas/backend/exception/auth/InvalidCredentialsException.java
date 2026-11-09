package com.mentesvivas.backend.exception.auth;

public class InvalidCredentialsException extends AuthException {

    public InvalidCredentialsException() {
        super("E-mail ou senha invalidos");
    }
}
