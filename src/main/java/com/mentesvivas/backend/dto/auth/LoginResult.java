package com.mentesvivas.backend.dto.auth;

public record LoginResult(
        TokenResponse tokens,
        TwoFactorChallengeResponse challenge
) {

    public static LoginResult authenticated(TokenResponse tokens) {
        return new LoginResult(tokens, null);
    }

    public static LoginResult challenged(TwoFactorChallengeResponse challenge) {
        return new LoginResult(null, challenge);
    }

    public boolean requiresTwoFactor() {
        return challenge != null;
    }
}
