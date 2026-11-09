package com.mentesvivas.backend.cache;

import java.time.Duration;
import java.util.Optional;

public interface TemporaryCounter {

    long increment(String key, Duration expiration);

    long value(String key);

    Optional<Duration> remainingTime(String key);

    void remove(String key);
}
