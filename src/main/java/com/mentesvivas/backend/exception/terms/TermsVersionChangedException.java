package com.mentesvivas.backend.exception.terms;

import com.mentesvivas.backend.exception.common.ConflictException;

public class TermsVersionChangedException extends ConflictException {

    public TermsVersionChangedException() {
        super("Os termos foram atualizados. Consulte a versao atual antes de aceitar.");
    }
}
