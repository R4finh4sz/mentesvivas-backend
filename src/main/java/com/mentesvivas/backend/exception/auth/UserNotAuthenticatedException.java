package com.mentesvivas.backend.exception.auth;

public class UserNotAuthenticatedException extends AuthException {

    public UserNotAuthenticatedException() {
        super("E necessario estar autenticado.");
    }
}
