package com.mentesvivas.backend.service.prescription;

import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ServiceUnavailableException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentSignerTest {

    private final User doctor = new User();

    @Test
    void simulatedSignerIsDeterministicAndMarkedAsSimulated() {
        SimulatedDocumentSigner signer = new SimulatedDocumentSigner();
        byte[] content = "documento".getBytes(StandardCharsets.UTF_8);

        SignatureResult first = signer.sign(content, doctor);
        SignatureResult second = signer.sign(content, doctor);

        assertThat(first.simulated()).isTrue();
        assertThat(first.reference()).startsWith("SIMULADA-").isEqualTo(second.reference());
    }

    @Test
    void simulatedSignerChangesTheReferenceWhenTheContentChanges() {
        SimulatedDocumentSigner signer = new SimulatedDocumentSigner();

        assertThat(signer.sign("a".getBytes(StandardCharsets.UTF_8), doctor).reference())
                .isNotEqualTo(signer.sign("b".getBytes(StandardCharsets.UTF_8), doctor).reference());
    }

    @Test
    void unavailableSignerNeverPretendsToSign() {
        assertThatThrownBy(() -> new UnavailableDocumentSigner().sign(new byte[0], doctor))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("nao esta disponivel");
    }
}
