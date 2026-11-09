package com.mentesvivas.backend.exception.user;

import com.mentesvivas.backend.exception.common.ConflictException;

public class CpfAlreadyRegisteredException extends ConflictException {

    public CpfAlreadyRegisteredException() {
        super("CPF ja cadastrado");
    }
}
