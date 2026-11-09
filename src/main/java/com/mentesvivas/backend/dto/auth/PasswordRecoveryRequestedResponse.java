package com.mentesvivas.backend.dto.auth;

public record PasswordRecoveryRequestedResponse(
        String message,
        long codeExpiresInSeconds
) {
}
