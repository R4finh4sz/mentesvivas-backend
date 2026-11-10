package com.mentesvivas.backend.service.mfa;

import com.mentesvivas.backend.config.TwoFactorProperties;
import com.mentesvivas.backend.exception.auth.InvalidCodeException;
import com.mentesvivas.backend.security.auth.TokenHasher;
import com.mentesvivas.backend.support.InMemoryTemporaryStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticatorCodeVerifierTest {

    private final TotpService totp = new TotpService("MentesVivas");
    private final UUID userId = UUID.randomUUID();
    private String secret;
    private AuthenticatorCodeVerifier verifier;

    @BeforeEach
    void setUp() {
        InMemoryTemporaryStorage storage = new InMemoryTemporaryStorage();
        TwoFactorProperties properties = new TwoFactorProperties(Duration.ofMinutes(10), 3, 5, Duration.ofHours(1));
        verifier = new AuthenticatorCodeVerifier(totp, storage, storage, new TokenHasher(), properties);
        secret = totp.generateSecret();
    }

    private String wrongCode() {
        String right = totp.codeAt(secret, Instant.now());
        return right.equals("000000") ? "000001" : "000000";
    }

    @Test
    void acceptsTheCurrentCode() {
        assertThatCode(() -> verifier.verify(userId, secret, totp.codeAt(secret, Instant.now()))).doesNotThrowAnyException();
    }

    @Test
    void aCodeCannotBeUsedTwice() {
        String code = totp.codeAt(secret, Instant.now());
        verifier.verify(userId, secret, code);

        assertThatThrownBy(() -> verifier.verify(userId, secret, code)).isInstanceOf(InvalidCodeException.class);
    }

    @Test
    void blocksGuessingAfterTheAttemptLimit() {
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> verifier.verify(userId, secret, wrongCode())).isInstanceOf(InvalidCodeException.class);
        }

        String right = totp.codeAt(secret, Instant.now());
        assertThatThrownBy(() -> verifier.verify(userId, secret, right)).isInstanceOf(InvalidCodeException.class);
    }

    @Test
    void successResetsTheAttemptCounter() {
        assertThatThrownBy(() -> verifier.verify(userId, secret, wrongCode())).isInstanceOf(InvalidCodeException.class);
        assertThatThrownBy(() -> verifier.verify(userId, secret, wrongCode())).isInstanceOf(InvalidCodeException.class);
        verifier.verify(userId, secret, totp.codeAt(secret, Instant.now()));

        assertThatThrownBy(() -> verifier.verify(userId, secret, wrongCode())).isInstanceOf(InvalidCodeException.class);
        assertThatThrownBy(() -> verifier.verify(userId, secret, wrongCode())).isInstanceOf(InvalidCodeException.class);
    }
}
