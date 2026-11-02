package com.mentesvivas.backend.exception.usuario;

import com.mentesvivas.backend.exception.common.ConflitoException;

public class CpfJaCadastradoException extends ConflitoException {

    public CpfJaCadastradoException() {
        super("CPF ja cadastrado");
    }
}
