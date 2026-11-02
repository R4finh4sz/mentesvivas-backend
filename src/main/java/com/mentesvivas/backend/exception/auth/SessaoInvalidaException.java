package com.mentesvivas.backend.exception.auth;

public class SessaoInvalidaException extends AutenticacaoException {

    public SessaoInvalidaException() {
        super("Sessao invalida ou expirada. Faca login novamente.");
    }
}
