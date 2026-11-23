package com.mentesvivas.backend.service.prescription;

import com.mentesvivas.backend.dto.prescription.VerificationStatus;
import com.mentesvivas.backend.entity.prescription.Prescription;
import com.mentesvivas.backend.entity.prescription.PrescriptionStatus;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.prescription.PrescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionVerificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-23T12:00:00Z");

    @Mock
    private PrescriptionRepository repository;

    private PrescriptionVerificationService service;

    @BeforeEach
    void setUp() {
        service = new PrescriptionVerificationService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private Prescription signed(Instant validUntil, UUID replacedBy) {
        Prescription prescription = new Prescription();
        prescription.setStatus(PrescriptionStatus.SIGNED);
        prescription.setValidUntil(validUntil);
        prescription.setReplacedById(replacedBy);
        when(repository.findByVerificationCodeAndStatus("code", PrescriptionStatus.SIGNED))
                .thenReturn(Optional.of(prescription));
        return prescription;
    }

    @Test
    void currentDocumentIsValid() {
        signed(NOW.plusSeconds(3600), null);

        assertThat(service.verify("code").status()).isEqualTo(VerificationStatus.VALID);
    }

    @Test
    void pastValidityIsExpired() {
        signed(NOW.minusSeconds(1), null);

        assertThat(service.verify("code").status()).isEqualTo(VerificationStatus.EXPIRED);
    }

    @Test
    void replacedDocumentIsReportedAsReplacedEvenWhenExpired() {
        signed(NOW.minusSeconds(1), UUID.randomUUID());

        assertThat(service.verify("code").status()).isEqualTo(VerificationStatus.REPLACED);
    }

    @Test
    void unknownCodeIsNotFound() {
        when(repository.findByVerificationCodeAndStatus("x", PrescriptionStatus.SIGNED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify("x")).isInstanceOf(ResourceNotFoundException.class);
    }
}
