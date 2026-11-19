package com.mentesvivas.backend.service.audit;

import com.mentesvivas.backend.entity.audit.AccessEvent;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.repository.audit.AccessEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DatabaseAccessLogTest {

    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");

    @Mock
    private AccessEventRepository repository;

    private DatabaseAccessLog log;

    @BeforeEach
    void setUp() {
        log = new DatabaseAccessLog(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void storesTheAccessorThePatientTheAreaAndTheTime() {
        UUID accessor = UUID.randomUUID();
        UUID patient = UUID.randomUUID();

        log.record(accessor, patient, PatientDataArea.ALLERGIES);

        ArgumentCaptor<AccessEvent> captor = ArgumentCaptor.forClass(AccessEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAccessorUserId()).isEqualTo(accessor);
        assertThat(captor.getValue().getPatientUserId()).isEqualTo(patient);
        assertThat(captor.getValue().getArea()).isEqualTo(PatientDataArea.ALLERGIES);
        assertThat(captor.getValue().getOccurredAt()).isEqualTo(NOW);
    }

    @Test
    void thePatientReadingTheirOwnDataIsNotRecorded() {
        UUID patient = UUID.randomUUID();

        log.record(patient, patient, PatientDataArea.PROFILE);

        verify(repository, never()).save(any());
    }
}
