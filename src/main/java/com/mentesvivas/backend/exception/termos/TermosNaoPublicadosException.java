package com.mentesvivas.backend.exception.termos;

import com.mentesvivas.backend.exception.common.RecursoNaoEncontradoException;

public class TermosNaoPublicadosException extends RecursoNaoEncontradoException {

    public TermosNaoPublicadosException() {
        super("Nenhum termo de uso publicado.");
    }
}
