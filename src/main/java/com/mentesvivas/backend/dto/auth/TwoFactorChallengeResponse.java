package com.mentesvivas.backend.dto.auth;

public record TwoFactorChallengeResponse(
        boolean twoFactorRequired,
        String message,
        long codeExpiresInSeconds
) {
}
