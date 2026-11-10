package com.mentesvivas.backend.service.mfa;

import com.mentesvivas.backend.util.Base32;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TotpServiceTest {

    private final TotpService service = new TotpService("MentesVivas");
    private final String rfcSecret = Base32.encode("12345678901234567890".getBytes(StandardCharsets.US_ASCII));

    @Test
    void matchesTheRfc6238TestVectors() {
        assertThat(service.codeAt(rfcSecret, Instant.ofEpochSecond(59))).isEqualTo("287082");
        assertThat(service.codeAt(rfcSecret, Instant.ofEpochSecond(1111111109))).isEqualTo("081804");
        assertThat(service.codeAt(rfcSecret, Instant.ofEpochSecond(2000000000))).isEqualTo("279037");
    }

    @Test
    void acceptsTheNeighbourStepsAndRejectsTheRest() {
        Instant now = Instant.ofEpochSecond(1_700_000_000L);

        assertThat(service.matchingStep(rfcSecret, service.codeAt(rfcSecret, now), now)).isPresent();
        assertThat(service.matchingStep(rfcSecret, service.codeAt(rfcSecret, now.minusSeconds(30)), now)).isPresent();
        assertThat(service.matchingStep(rfcSecret, service.codeAt(rfcSecret, now.plusSeconds(30)), now)).isPresent();
        assertThat(service.matchingStep(rfcSecret, service.codeAt(rfcSecret, now.minusSeconds(120)), now)).isEmpty();
        assertThat(service.matchingStep(rfcSecret, "abcdef", now)).isEmpty();
    }

    @Test
    void generatesDistinctBase32Secrets() {
        String first = service.generateSecret();

        assertThat(first).matches("^[A-Z2-7]{32}$").isNotEqualTo(service.generateSecret());
    }

    @Test
    void buildsAnOtpauthUriForAuthenticatorApps() {
        String uri = service.provisioningUri("dr.paulo@clinica.com", "ABCDEFGH");

        assertThat(uri).startsWith("otpauth://totp/MentesVivas:dr.paulo%40clinica.com?secret=ABCDEFGH")
                .contains("issuer=MentesVivas", "digits=6", "period=30");
    }
}
