package com.mentesvivas.backend.support;

import com.mentesvivas.backend.cache.TemporaryCounter;
import com.mentesvivas.backend.cache.TemporaryValue;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Primary
public class InMemoryTemporaryStorage implements TemporaryCounter, TemporaryValue {

    private record Entry(String value, Instant expiresAt) {
    }

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    @Override
    public synchronized long increment(String key, Duration expiration) {
        Entry current = liveEntry(key);
        long total = current == null ? 1L : Long.parseLong(current.value()) + 1;
        Instant expiresAt = current == null ? Instant.now().plus(expiration) : current.expiresAt();
        entries.put(key, new Entry(Long.toString(total), expiresAt));
        return total;
    }

    @Override
    public synchronized long value(String key) {
        Entry current = liveEntry(key);
        return current == null ? 0L : Long.parseLong(current.value());
    }

    @Override
    public synchronized void store(String key, String value, Duration expiration) {
        entries.put(key, new Entry(value, Instant.now().plus(expiration)));
    }

    @Override
    public synchronized Optional<String> get(String key) {
        return Optional.ofNullable(liveEntry(key)).map(Entry::value);
    }

    @Override
    public synchronized Optional<Duration> remainingTime(String key) {
        return Optional.ofNullable(liveEntry(key)).map(e -> Duration.between(Instant.now(), e.expiresAt()));
    }

    @Override
    public synchronized void remove(String key) {
        entries.remove(key);
    }

    public synchronized java.util.List<String> allValues() {
        return entries.values().stream().map(Entry::value).toList();
    }

    public synchronized void clear() {
        entries.clear();
    }

    private Entry liveEntry(String key) {
        Entry entry = entries.get(key);
        if (entry != null && !entry.expiresAt().isAfter(Instant.now())) {
            entries.remove(key);
            return null;
        }
        return entry;
    }
}
