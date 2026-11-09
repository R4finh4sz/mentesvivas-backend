package com.mentesvivas.backend.cache;

import java.time.Duration;
import java.util.Optional;

public interface ContadorTemporario {

    long incrementar(String chave, Duration expiracao);

    long valor(String chave);

    Optional<Duration> tempoRestante(String chave);

    void remover(String chave);
}
