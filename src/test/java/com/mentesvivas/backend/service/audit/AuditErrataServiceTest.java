package com.mentesvivas.backend.service.audit;

import com.mentesvivas.backend.entity.audit.AuditEvent;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.audit.AuditEventRepository;
import com.mentesvivas.backend.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditErrataServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");
    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID PATIENT = UUID.randomUUID();

    @Mock
    private AuditEventRepository repository;
    @Mock
    private CurrentUser currentUser;

    private AuditErrataService service;
    private User admin;

    @BeforeEach
    void setUp() {
        service = new AuditErrataService(repository, currentUser, Clock.fixed(NOW, ZoneOffset.UTC));
        admin = new User() {
            @Override
            public UUID getId() {
                return UUID.fromString("00000000-0000-0000-0000-000000000001");
            }
        };
    }

    private AuditEvent event(AuditEventType type) {
        return new AuditEvent(NOW, AuditRecord.of(type, UUID.randomUUID()).forPatient(PATIENT)) {
            @Override
            public UUID getId() {
                return EVENT_ID;
            }
        };
    }

    @Test
    void errataIsStoredAsANewEventLinkedToTheOriginalWithTheJustification() {
        when(repository.findById(EVENT_ID)).thenReturn(Optional.of(event(AuditEventType.ALLERGY_UPDATED)));
        when(repository.existsByCorrectsEventId(EVENT_ID)).thenReturn(false);
        when(currentUser.get()).thenReturn(admin);
        when(repository.save(any(AuditEvent.class))).thenAnswer(call -> call.getArgument(0));

        var response = service.correct(EVENT_ID, "Valor digitado errado");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repository).save(captor.capture());
        AuditEvent errata = captor.getValue();
        assertThat(errata.getEventType()).isEqualTo(AuditEventType.ERRATA);
        assertThat(errata.getCorrectsEventId()).isEqualTo(EVENT_ID);
        assertThat(errata.getJustification()).isEqualTo("Valor digitado errado");
        assertThat(errata.getActorUserId()).isEqualTo(admin.getId());
        assertThat(errata.getPatientUserId()).isEqualTo(PATIENT);
        assertThat(response.correctsEventId()).isEqualTo(EVENT_ID);
    }

    @Test
    void unknownEventIsNotFound() {
        when(repository.findById(EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.correct(EVENT_ID, "x")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void anErrataCannotBeCorrected() {
        when(repository.findById(EVENT_ID)).thenReturn(Optional.of(event(AuditEventType.ERRATA)));

        assertThatThrownBy(() -> service.correct(EVENT_ID, "x")).isInstanceOf(ConflictException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void eventThatAlreadyHasAnErrataIsAConflict() {
        when(repository.findById(EVENT_ID)).thenReturn(Optional.of(event(AuditEventType.ALLERGY_UPDATED)));
        when(repository.existsByCorrectsEventId(EVENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.correct(EVENT_ID, "x")).isInstanceOf(ConflictException.class);

        verify(repository, never()).save(any());
    }
}
