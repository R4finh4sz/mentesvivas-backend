package com.mentesvivas.backend.exception.auth;

public class UsuarioNaoAutenticadoException extends AutenticacaoException {

    public UsuarioNaoAutenticadoException() {
        super("E necessario estar autenticado.");
    }
}
