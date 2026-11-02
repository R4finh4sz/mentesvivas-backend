package com.mentesvivas.backend.exception.usuario;

import com.mentesvivas.backend.exception.common.ConflitoException;

public class EmailJaCadastradoException extends ConflitoException {

    public EmailJaCadastradoException() {
        super("E-mail ja cadastrado");
    }
}
