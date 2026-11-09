package com.mentesvivas.backend.cache;

import java.time.Duration;
import java.util.Optional;

public interface ValorTemporario {

    void gravar(String chave, String valor, Duration expiracao);

    Optional<String> obter(String chave);

    Optional<Duration> tempoRestante(String chave);

    void remover(String chave);
}
