package com.mentesvivas.backend.service.mfa;

import com.mentesvivas.backend.entity.user.MfaBackupCode;
import com.mentesvivas.backend.repository.user.MfaBackupCodeRepository;
import com.mentesvivas.backend.security.auth.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BackupCodeService {

    public static final int CODE_LENGTH = 10;
    public static final int CODES_PER_BATCH = 10;

    private static final long BOUND = 10_000_000_000L;

    private final SecureRandom secureRandom = new SecureRandom();
    private final MfaBackupCodeRepository repository;
    private final TokenHasher hasher;

    @Transactional
    public List<String> regenerate(UUID userId) {
        repository.deleteAllByUserId(userId);
        repository.flush();
        List<String> codes = new ArrayList<>();
        while (codes.size() < CODES_PER_BATCH) {
            String code = "%010d".formatted(secureRandom.nextLong(BOUND));
            if (!codes.contains(code)) {
                codes.add(code);
            }
        }
        Instant now = Instant.now();
        repository.saveAll(codes.stream().map(code -> new MfaBackupCode(userId, hash(userId, code), now)).toList());
        return List.copyOf(codes);
    }

    @Transactional
    public boolean consume(UUID userId, String code) {
        return repository.findByUserIdAndCodeHash(userId, hash(userId, code))
                .filter(stored -> !stored.isUsed())
                .map(stored -> {
                    stored.setUsedAt(Instant.now());
                    return true;
                })
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public long remaining(UUID userId) {
        return repository.countByUserIdAndUsedAtIsNull(userId);
    }

    @Transactional
    public void clear(UUID userId) {
        repository.deleteAllByUserId(userId);
    }

    public static boolean looksLikeBackupCode(String code) {
        return code != null && code.length() == CODE_LENGTH && code.chars().allMatch(Character::isDigit);
    }

    private String hash(UUID userId, String code) {
        return hasher.hash(userId + ":" + code);
    }
}
