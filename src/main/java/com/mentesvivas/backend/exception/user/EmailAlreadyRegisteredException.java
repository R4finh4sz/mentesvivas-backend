package com.mentesvivas.backend.exception.user;

import com.mentesvivas.backend.exception.common.ConflictException;

public class EmailAlreadyRegisteredException extends ConflictException {

    public EmailAlreadyRegisteredException() {
        super("E-mail ja cadastrado");
    }
}
