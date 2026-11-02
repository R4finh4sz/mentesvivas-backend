package com.mentesvivas.backend.exception.auth;

public class CredenciaisInvalidasException extends AutenticacaoException {

    public CredenciaisInvalidasException() {
        super("E-mail ou senha invalidos");
    }
}
