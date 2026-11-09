package com.mentesvivas.backend.support;

import com.mentesvivas.backend.cache.ContadorTemporario;
import com.mentesvivas.backend.cache.ValorTemporario;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Primary
public class MemoriaArmazenamentoTemporario implements ContadorTemporario, ValorTemporario {

    private record Entrada(String valor, Instant expiraEm) {
    }

    private final Map<String, Entrada> entradas = new ConcurrentHashMap<>();

    @Override
    public synchronized long incrementar(String chave, Duration expiracao) {
        Entrada atual = vivo(chave);
        long total = atual == null ? 1L : Long.parseLong(atual.valor()) + 1;
        Instant expiraEm = atual == null ? Instant.now().plus(expiracao) : atual.expiraEm();
        entradas.put(chave, new Entrada(Long.toString(total), expiraEm));
        return total;
    }

    @Override
    public synchronized long valor(String chave) {
        Entrada atual = vivo(chave);
        return atual == null ? 0L : Long.parseLong(atual.valor());
    }

    @Override
    public synchronized void gravar(String chave, String valor, Duration expiracao) {
        entradas.put(chave, new Entrada(valor, Instant.now().plus(expiracao)));
    }

    @Override
    public synchronized Optional<String> obter(String chave) {
        return Optional.ofNullable(vivo(chave)).map(Entrada::valor);
    }

    @Override
    public synchronized Optional<Duration> tempoRestante(String chave) {
        return Optional.ofNullable(vivo(chave)).map(e -> Duration.between(Instant.now(), e.expiraEm()));
    }

    @Override
    public synchronized void remover(String chave) {
        entradas.remove(chave);
    }

    public synchronized java.util.List<String> todosOsValores() {
        return entradas.values().stream().map(Entrada::valor).toList();
    }

    public synchronized void limpar() {
        entradas.clear();
    }

    private Entrada vivo(String chave) {
        Entrada entrada = entradas.get(chave);
        if (entrada != null && !entrada.expiraEm().isAfter(Instant.now())) {
            entradas.remove(chave);
            return null;
        }
        return entrada;
    }
}
