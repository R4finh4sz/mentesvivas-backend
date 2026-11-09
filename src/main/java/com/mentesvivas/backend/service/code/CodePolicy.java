package com.mentesvivas.backend.service.code;

import java.time.Duration;

public record CodePolicy(
        String name,
        Duration validity,
        int maxAttempts,
        int maxSends,
        Duration sendsWindow
) {
}
