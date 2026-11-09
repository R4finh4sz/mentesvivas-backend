package com.mentesvivas.backend.exception.auth;

import lombok.Getter;

import java.time.Duration;

@Getter
public class AccountLockedException extends AuthException {

    private final Duration remainingTime;

    public AccountLockedException(Duration remainingTime) {
        super("Acesso temporariamente indisponivel por excesso de tentativas. Aguarde ou recupere sua senha.");
        this.remainingTime = remainingTime;
    }
}
