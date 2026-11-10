package com.mentesvivas.backend.dto.mfa;

public record AuthenticatorSetupResponse(
        String secret,
        String otpauthUri
) {
}
