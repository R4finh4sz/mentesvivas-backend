package com.mentesvivas.backend.service.audit;

import com.mentesvivas.backend.entity.audit.AuditEvent;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.repository.audit.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DatabaseAuditTrailTest {

    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");

    @Mock
    private AuditEventRepository repository;

    private DatabaseAuditTrail trail;

    @BeforeEach
    void setUp() {
        trail = new DatabaseAuditTrail(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private AuditEvent savedEvent() {
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void recordStoresEveryFieldOfTheRecordWithTheClockTime() {
        UUID actor = UUID.randomUUID();
        UUID patient = UUID.randomUUID();
        UUID represented = UUID.randomUUID();

        trail.record(AuditRecord.of(AuditEventType.ALLERGY_UPDATED, actor).forPatient(patient)
                .onBehalfOf(represented).withValues("antes", "depois"));

        AuditEvent event = savedEvent();
        assertThat(event.getOccurredAt()).isEqualTo(NOW);
        assertThat(event.getEventType()).isEqualTo(AuditEventType.ALLERGY_UPDATED);
        assertThat(event.getActorUserId()).isEqualTo(actor);
        assertThat(event.getPatientUserId()).isEqualTo(patient);
        assertThat(event.getOnBehalfOfUserId()).isEqualTo(represented);
        assertThat(event.getBeforeValue()).isEqualTo("antes");
        assertThat(event.getAfterValue()).isEqualTo("depois");
    }

    @Test
    void recordIndependentlyStoresTheSameWay() {
        trail.recordIndependently(AuditRecord.of(AuditEventType.LOGIN_FAILED, null));

        AuditEvent event = savedEvent();
        assertThat(event.getEventType()).isEqualTo(AuditEventType.LOGIN_FAILED);
        assertThat(event.getActorUserId()).isNull();
    }

    @Test
    void recordJoinsTheCallerTransactionAndIndependentRecordOpensItsOwn() throws NoSuchMethodException {
        Transactional joined = DatabaseAuditTrail.class.getMethod("record", AuditRecord.class)
                .getAnnotation(Transactional.class);
        Transactional separate = DatabaseAuditTrail.class.getMethod("recordIndependently", AuditRecord.class)
                .getAnnotation(Transactional.class);

        assertThat(joined.propagation()).isEqualTo(Propagation.REQUIRED);
        assertThat(separate.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }
}
