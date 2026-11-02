package com.mentesvivas.backend.exception.termos;

import com.mentesvivas.backend.exception.common.ConflitoException;

public class TermosVersaoAlteradaException extends ConflitoException {

    public TermosVersaoAlteradaException() {
        super("Os termos foram atualizados. Consulte a versao atual antes de aceitar.");
    }
}
