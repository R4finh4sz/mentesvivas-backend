package com.mentesvivas.backend.exception.auth;

public class AccountInactiveException extends AuthException {

    public AccountInactiveException() {
        super("Esta conta esta inativa. Entre em contato com o suporte.");
    }
}
