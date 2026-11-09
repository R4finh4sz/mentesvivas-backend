package com.mentesvivas.backend.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisArmazenamentoTemporario implements ContadorTemporario, ValorTemporario {

    private final StringRedisTemplate redis;

    @Override
    public long incrementar(String chave, Duration expiracao) {
        Long total = redis.opsForValue().increment(chave);
        if (total != null && total == 1L) {
            redis.expire(chave, expiracao);
        }
        return total == null ? 0L : total;
    }

    @Override
    public long valor(String chave) {
        String valor = redis.opsForValue().get(chave);
        return valor == null ? 0L : Long.parseLong(valor);
    }

    @Override
    public void gravar(String chave, String valor, Duration expiracao) {
        redis.opsForValue().set(chave, valor, expiracao);
    }

    @Override
    public Optional<String> obter(String chave) {
        return Optional.ofNullable(redis.opsForValue().get(chave));
    }

    @Override
    public Optional<Duration> tempoRestante(String chave) {
        Long segundos = redis.getExpire(chave);
        return segundos == null || segundos < 0 ? Optional.empty() : Optional.of(Duration.ofSeconds(segundos));
    }

    @Override
    public void remover(String chave) {
        redis.delete(chave);
    }
}
