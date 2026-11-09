package com.mentesvivas.backend.exception.auth;

public class CodigoRecuperacaoInvalidoException extends RuntimeException {

    public CodigoRecuperacaoInvalidoException() {
        super("Codigo invalido ou expirado. Solicite um novo codigo.");
    }
}
