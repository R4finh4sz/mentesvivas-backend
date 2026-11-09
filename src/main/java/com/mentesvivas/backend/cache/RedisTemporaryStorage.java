package com.mentesvivas.backend.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisTemporaryStorage implements TemporaryCounter, TemporaryValue {

    private final StringRedisTemplate redis;

    @Override
    public long increment(String key, Duration expiration) {
        Long total = redis.opsForValue().increment(key);
        if (total != null && total == 1L) {
            redis.expire(key, expiration);
        }
        return total == null ? 0L : total;
    }

    @Override
    public long value(String key) {
        String value = redis.opsForValue().get(key);
        return value == null ? 0L : Long.parseLong(value);
    }

    @Override
    public void store(String key, String value, Duration expiration) {
        redis.opsForValue().set(key, value, expiration);
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(redis.opsForValue().get(key));
    }

    @Override
    public Optional<Duration> remainingTime(String key) {
        Long seconds = redis.getExpire(key);
        return seconds == null || seconds < 0 ? Optional.empty() : Optional.of(Duration.ofSeconds(seconds));
    }

    @Override
    public void remove(String key) {
        redis.delete(key);
    }
}
