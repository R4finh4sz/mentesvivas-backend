package com.mentesvivas.backend.service.code;

import com.mentesvivas.backend.cache.TemporaryCounter;
import com.mentesvivas.backend.cache.TemporaryValue;
import com.mentesvivas.backend.exception.auth.InvalidCodeException;
import com.mentesvivas.backend.security.auth.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OneTimeCodeService {

    private final SecureRandom secureRandom = new SecureRandom();
    private final TemporaryCounter counter;
    private final TemporaryValue values;
    private final TokenHasher hasher;

    public Optional<String> generate(CodePolicy policy, String email) {
        String key = hasher.hash(email);
        long sends = counter.increment(sendsKey(policy, key), policy.sendsWindow());
        if (sends > policy.maxSends()) {
            return Optional.empty();
        }
        String code = "%06d".formatted(secureRandom.nextInt(1_000_000));
        values.store(codeKey(policy, key), codeHash(key, code), policy.validity());
        counter.remove(attemptsKey(policy, key));
        return Optional.of(code);
    }

    public void validateAndConsume(CodePolicy policy, String email, String code) {
        String key = hasher.hash(email);
        long attempts = counter.increment(attemptsKey(policy, key), policy.validity());
        if (attempts > policy.maxAttempts()) {
            values.remove(codeKey(policy, key));
            throw new InvalidCodeException();
        }
        String expected = values.get(codeKey(policy, key)).orElseThrow(InvalidCodeException::new);
        boolean matches = MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                codeHash(key, code).getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            throw new InvalidCodeException();
        }
        values.remove(codeKey(policy, key));
        counter.remove(attemptsKey(policy, key));
    }

    private String codeKey(CodePolicy policy, String key) {
        return policy.name() + ":code:" + key;
    }

    private String attemptsKey(CodePolicy policy, String key) {
        return policy.name() + ":attempts:" + key;
    }

    private String sendsKey(CodePolicy policy, String key) {
        return policy.name() + ":sends:" + key;
    }

    private String codeHash(String key, String code) {
        return hasher.hash(key + ":" + code);
    }
}
