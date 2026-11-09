package com.mentesvivas.backend.dto.auth;

public record CodeRequestedResponse(
        String message,
        long codeExpiresInSeconds
) {
}
