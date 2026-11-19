package com.mentesvivas.backend.exception.auth;

import lombok.Getter;

import java.time.Duration;

@Getter
public class CodeSendLimitException extends AuthException {

    private final Duration remainingTime;

    public CodeSendLimitException(Duration remainingTime) {
        super("Limite de envios do codigo atingido. Aguarde para solicitar um novo codigo.");
        this.remainingTime = remainingTime;
    }
}
