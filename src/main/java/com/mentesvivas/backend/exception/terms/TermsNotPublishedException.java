package com.mentesvivas.backend.exception.terms;

import com.mentesvivas.backend.exception.common.ResourceNotFoundException;

public class TermsNotPublishedException extends ResourceNotFoundException {

    public TermsNotPublishedException() {
        super("Nenhum termo de uso publicado.");
    }
}
