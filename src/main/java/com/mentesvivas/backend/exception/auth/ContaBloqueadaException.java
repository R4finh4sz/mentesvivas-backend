package com.mentesvivas.backend.exception.auth;

import lombok.Getter;

import java.time.Duration;

@Getter
public class ContaBloqueadaException extends AutenticacaoException {

    private final Duration tempoRestante;

    public ContaBloqueadaException(Duration tempoRestante) {
        super("Acesso temporariamente indisponivel por excesso de tentativas. Aguarde ou recupere sua senha.");
        this.tempoRestante = tempoRestante;
    }
}
