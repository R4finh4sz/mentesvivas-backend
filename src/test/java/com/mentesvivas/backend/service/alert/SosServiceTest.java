package com.mentesvivas.backend.service.alert;

import com.mentesvivas.backend.dto.alert.AlertDetailResponse;
import com.mentesvivas.backend.entity.alert.AlertStatus;
import com.mentesvivas.backend.entity.alert.SosAlert;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.entity.notification.NotificationType;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.repository.alert.SosAlertRepository;
import com.mentesvivas.backend.security.CurrentPatient;
import com.mentesvivas.backend.service.audit.AuditTrail;
import com.mentesvivas.backend.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SosServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-23T12:00:00Z");

    @Mock
    private CurrentPatient currentPatient;
    @Mock
    private SosRecipients recipients;
    @Mock
    private SosAlertRepository repository;
    @Mock
    private NotificationService notifications;
    @Mock
    private AlertDetailLoader detailLoader;
    @Mock
    private AuditTrail auditTrail;

    private SosService service;
    private User patient;

    @BeforeEach
    void setUp() {
        service = new SosService(currentPatient, recipients, repository, notifications, detailLoader, auditTrail,
                Clock.fixed(NOW, ZoneOffset.UTC));
        patient = new User();
        patient.setId(UUID.randomUUID());
        patient.setFullName("Maria Souza");
        when(currentPatient.get()).thenReturn(patient);
    }

    @Test
    void triggerCreatesTheAlertAndNotifiesEveryLinkedCaregiver() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(recipients.requireCaregiversOrContacts(patient.getId())).thenReturn(List.of(first, second));
        when(repository.save(any(SosAlert.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AlertDetailResponse detail = new AlertDetailResponse(null, List.of(), List.of());
        when(detailLoader.load(any(SosAlert.class))).thenReturn(detail);

        assertThat(service.trigger()).isSameAs(detail);

        ArgumentCaptor<SosAlert> saved = ArgumentCaptor.forClass(SosAlert.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getPatientId()).isEqualTo(patient.getId());
        assertThat(saved.getValue().getStatus()).isEqualTo(AlertStatus.CREATED);
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
        verify(notifications).notify(eq(first), eq(NotificationType.SOS_ALERT), any(), eq("Maria Souza acionou o SOS."));
        verify(notifications).notify(eq(second), eq(NotificationType.SOS_ALERT), any(), eq("Maria Souza acionou o SOS."));
    }

    @Test
    void triggerWithOnlyEmergencyContactsStillCreatesTheAlertWithoutNotifications() {
        when(recipients.requireCaregiversOrContacts(patient.getId())).thenReturn(List.of());
        when(repository.save(any(SosAlert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.trigger();

        verify(repository).save(any(SosAlert.class));
        verify(notifications, never()).notify(any(), any(), any(), any());
    }

    @Test
    void triggerIsRecordedInTheAuditTrail() {
        when(recipients.requireCaregiversOrContacts(patient.getId())).thenReturn(List.of());
        when(repository.save(any(SosAlert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.trigger();

        ArgumentCaptor<AuditRecord> record = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditTrail).record(record.capture());
        assertThat(record.getValue().type()).isEqualTo(AuditEventType.SOS_TRIGGERED);
        assertThat(record.getValue().actorUserId()).isEqualTo(patient.getId());
        assertThat(record.getValue().patientUserId()).isEqualTo(patient.getId());
    }

    @Test
    void triggerWithoutRecipientsIsRefusedAndCreatesNothing() {
        when(recipients.requireCaregiversOrContacts(patient.getId())).thenThrow(new ConflictException("sem destinatario"));

        assertThatThrownBy(() -> service.trigger()).isInstanceOf(ConflictException.class);

        verify(repository, never()).save(any());
        verify(auditTrail, never()).record(any());
    }
}
