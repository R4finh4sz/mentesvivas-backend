package com.mentesvivas.backend.cache;

import java.time.Duration;
import java.util.Optional;

public interface TemporaryValue {

    void store(String key, String value, Duration expiration);

    Optional<String> get(String key);

    Optional<Duration> remainingTime(String key);

    void remove(String key);
}
