package com.mentesvivas.backend.service.alert;

import com.mentesvivas.backend.entity.alert.AlertStatus;
import com.mentesvivas.backend.entity.alert.SosAlert;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.alert.SosAlertRepository;
import com.mentesvivas.backend.service.audit.AccessLog;
import com.mentesvivas.backend.service.audit.AuditTrail;
import com.mentesvivas.backend.service.link.CaregiverPatientAccess;
import com.mentesvivas.backend.service.patient.PatientCareAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    private static final UUID PATIENT = UUID.randomUUID();
    private static final Instant CREATED = Instant.parse("2026-11-23T11:00:00Z");
    private static final Instant NOW = Instant.parse("2026-11-23T12:00:00Z");

    @Mock
    private PatientCareAccess access;
    @Mock
    private CaregiverPatientAccess caregiverAccess;
    @Mock
    private SosAlertRepository repository;
    @Mock
    private AlertDetailLoader detailLoader;
    @Mock
    private AccessLog accessLog;
    @Mock
    private AuditTrail auditTrail;

    private AlertService service;
    private User actor;
    private SosAlert alert;
    private UUID alertId;

    @BeforeEach
    void setUp() {
        service = new AlertService(access, caregiverAccess, repository, detailLoader, accessLog, auditTrail,
                Clock.fixed(NOW, ZoneOffset.UTC));
        actor = new User();
        actor.setId(UUID.randomUUID());
        alert = new SosAlert(PATIENT, CREATED);
        alertId = UUID.randomUUID();
    }

    private void alertExists() {
        when(repository.findByIdAndPatientId(alertId, PATIENT)).thenReturn(Optional.of(alert));
        when(repository.save(alert)).thenReturn(alert);
    }

    @Test
    void seenRecordsAuthorAndTime() {
        when(caregiverAccess.requireRepresentativeOf(PATIENT)).thenReturn(actor);
        alertExists();

        var response = service.markSeen(PATIENT, alertId);

        assertThat(response.status()).isEqualTo(AlertStatus.SEEN);
        assertThat(response.seenBy()).isEqualTo(actor.getId());
        assertThat(response.seenAt()).isEqualTo(NOW);
        assertThat(response.acknowledgedAt()).isNull();
    }

    @Test
    void acknowledgingAFreshAlertAlsoFillsTheSeenStep() {
        when(caregiverAccess.requireRepresentativeOf(PATIENT)).thenReturn(actor);
        alertExists();

        var response = service.acknowledge(PATIENT, alertId);

        assertThat(response.status()).isEqualTo(AlertStatus.ACKNOWLEDGED);
        assertThat(response.seenBy()).isEqualTo(actor.getId());
        assertThat(response.acknowledgedBy()).isEqualTo(actor.getId());
        assertThat(response.resolvedAt()).isNull();
    }

    @Test
    void resolveByAnyWriterClosesTheAlert() {
        when(access.requireWriter(PATIENT)).thenReturn(actor);
        alertExists();

        var response = service.resolve(PATIENT, alertId);

        assertThat(response.status()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(response.resolvedBy()).isEqualTo(actor.getId());
        assertThat(response.resolvedAt()).isEqualTo(NOW);
    }

    @Test
    void everyTransitionIsAuditedForThePatient() {
        when(caregiverAccess.requireRepresentativeOf(PATIENT)).thenReturn(actor);
        alertExists();

        service.markSeen(PATIENT, alertId);

        ArgumentCaptor<AuditRecord> record = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditTrail).record(record.capture());
        assertThat(record.getValue().type()).isEqualTo(AuditEventType.ALERT_SEEN);
        assertThat(record.getValue().actorUserId()).isEqualTo(actor.getId());
        assertThat(record.getValue().patientUserId()).isEqualTo(PATIENT);
    }

    @Test
    void goingBackwardsOrRepeatingIsAConflict() {
        when(caregiverAccess.requireRepresentativeOf(PATIENT)).thenReturn(actor);
        alert.advance(AlertStatus.ACKNOWLEDGED, actor.getId(), CREATED);
        when(repository.findByIdAndPatientId(alertId, PATIENT)).thenReturn(Optional.of(alert));

        assertThatThrownBy(() -> service.markSeen(PATIENT, alertId)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.acknowledge(PATIENT, alertId)).isInstanceOf(ConflictException.class);

        assertThat(alert.getStatus()).isEqualTo(AlertStatus.ACKNOWLEDGED);
        verify(auditTrail, never()).record(any());
    }

    @Test
    void aResolvedAlertCannotBeResolvedAgain() {
        when(access.requireWriter(PATIENT)).thenReturn(actor);
        alert.advance(AlertStatus.RESOLVED, actor.getId(), CREATED);
        when(repository.findByIdAndPatientId(alertId, PATIENT)).thenReturn(Optional.of(alert));

        assertThatThrownBy(() -> service.resolve(PATIENT, alertId)).isInstanceOf(ConflictException.class);
    }

    @Test
    void unknownAlertIsNotFound() {
        when(caregiverAccess.requireRepresentativeOf(PATIENT)).thenReturn(actor);
        when(repository.findByIdAndPatientId(alertId, PATIENT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.acknowledge(PATIENT, alertId)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void nonRepresentativeCannotMarkSeen() {
        when(caregiverAccess.requireRepresentativeOf(PATIENT)).thenThrow(new AccessDeniedException("negado"));

        assertThatThrownBy(() -> service.markSeen(PATIENT, alertId)).isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void listPendingExcludesResolvedAndLogsTheAccess() {
        when(access.requireReader(PATIENT)).thenReturn(actor);
        when(repository.findByPatientIdAndStatusNotOrderByCreatedAtDesc(PATIENT, AlertStatus.RESOLVED))
                .thenReturn(List.of(alert));

        assertThat(service.list(PATIENT, true)).hasSize(1);
        verify(accessLog).record(actor.getId(), PATIENT, PatientDataArea.ALERTS);
        verify(repository, never()).findByPatientIdOrderByCreatedAtDesc(any());
    }

    @Test
    void listAllReturnsEveryAlert() {
        when(access.requireReader(PATIENT)).thenReturn(actor);
        when(repository.findByPatientIdOrderByCreatedAtDesc(PATIENT)).thenReturn(List.of(alert, alert));

        assertThat(service.list(PATIENT, false)).hasSize(2);
    }

    @Test
    void everyAlertCarriesTheNoticeThatItDoesNotReplaceEmergencyServices() {
        when(access.requireReader(PATIENT)).thenReturn(actor);
        when(repository.findByPatientIdOrderByCreatedAtDesc(PATIENT)).thenReturn(List.of(alert));

        assertThat(service.list(PATIENT, false).get(0).notice()).contains("nao substitui").contains("192");
    }

    @Test
    void detailLoadsTheAlertWithSevereAllergiesAndContacts() {
        when(access.requireReader(PATIENT)).thenReturn(actor);
        when(repository.findByIdAndPatientId(alertId, PATIENT)).thenReturn(Optional.of(alert));

        service.detail(PATIENT, alertId);

        verify(detailLoader).load(alert);
        verify(accessLog).record(actor.getId(), PATIENT, PatientDataArea.ALERTS);
    }
}
