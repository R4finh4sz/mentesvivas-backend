package com.mentesvivas.backend.dto.auth;

import com.mentesvivas.backend.entity.user.MfaMethod;

public record TwoFactorChallengeResponse(
        boolean twoFactorRequired,
        String message,
        long codeExpiresInSeconds,
        MfaMethod method
) {
}
