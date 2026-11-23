package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.config.AgendaProperties;
import com.mentesvivas.backend.dto.patient.SaveAppointmentRequest;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.patient.Appointment;
import com.mentesvivas.backend.entity.patient.AppointmentType;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.exception.patient.ImpossibleValueException;
import com.mentesvivas.backend.repository.patient.AppointmentRepository;
import com.mentesvivas.backend.service.audit.AccessLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
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
class AppointmentServiceTest {

    private static final UUID PATIENT = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-23T12:00:00Z");
    private static final Instant TOMORROW = Instant.parse("2026-11-24T15:00:00Z");

    @Mock
    private PatientCareAccess access;
    @Mock
    private AppointmentRepository repository;
    @Mock
    private AccessLog accessLog;

    private AppointmentService service;
    private User actor;

    @BeforeEach
    void setUp() {
        service = new AppointmentService(access, repository, accessLog, new AgendaProperties(Duration.ofHours(1)),
                Clock.fixed(NOW, ZoneOffset.UTC));
        actor = new User();
        actor.setId(UUID.randomUUID());
    }

    private SaveAppointmentRequest request(Instant when) {
        return new SaveAppointmentRequest(AppointmentType.CONSULTATION, "  Cardiologista  ", " Clinica Central ", "  ",
                when);
    }

    @Test
    void createsWithTheReminderOneHourBefore() {
        when(access.requireWriter(PATIENT)).thenReturn(actor);
        when(repository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(PATIENT, request(TOMORROW));

        ArgumentCaptor<Appointment> saved = ArgumentCaptor.forClass(Appointment.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getPatientId()).isEqualTo(PATIENT);
        assertThat(saved.getValue().getCreatedBy()).isEqualTo(actor.getId());
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
        assertThat(saved.getValue().getTitle()).isEqualTo("Cardiologista");
        assertThat(saved.getValue().getLocation()).isEqualTo("Clinica Central");
        assertThat(saved.getValue().getNotes()).isNull();
        assertThat(response.remindAt()).isEqualTo(TOMORROW.minus(Duration.ofHours(1)));
    }

    @Test
    void usesTheConfiguredReminderLead() {
        service = new AppointmentService(access, repository, accessLog, new AgendaProperties(Duration.ofMinutes(30)),
                Clock.fixed(NOW, ZoneOffset.UTC));
        when(access.requireWriter(PATIENT)).thenReturn(actor);
        when(repository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.create(PATIENT, request(TOMORROW)).remindAt()).isEqualTo(TOMORROW.minusSeconds(1800));
    }

    @Test
    void pastDateIsRejected() {
        when(access.requireWriter(PATIENT)).thenReturn(actor);

        assertThatThrownBy(() -> service.create(PATIENT, request(NOW)))
                .isInstanceOf(ImpossibleValueException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void listWithoutRangeStartsNowAndLogsTheAccess() {
        when(access.requireWriter(PATIENT)).thenReturn(actor);
        Appointment appointment = new Appointment();
        appointment.setType(AppointmentType.EXAM);
        when(repository.findByPatientIdAndScheduledAtGreaterThanEqualOrderByScheduledAtAsc(PATIENT, NOW))
                .thenReturn(List.of(appointment));

        assertThat(service.list(PATIENT, null, null)).hasSize(1);
        verify(accessLog).record(actor.getId(), PATIENT, PatientDataArea.APPOINTMENTS);
    }

    @Test
    void listWithRangeUsesBothLimits() {
        when(access.requireWriter(PATIENT)).thenReturn(actor);
        when(repository.findByPatientIdAndScheduledAtBetweenOrderByScheduledAtAsc(PATIENT, NOW, TOMORROW))
                .thenReturn(List.of());

        assertThat(service.list(PATIENT, NOW, TOMORROW)).isEmpty();
    }

    @Test
    void updateRewritesTheFieldsAndTheReminder() {
        UUID id = UUID.randomUUID();
        Appointment existing = new Appointment();
        existing.setPatientId(PATIENT);
        when(access.requireWriter(PATIENT)).thenReturn(actor);
        when(repository.findByIdAndPatientId(id, PATIENT)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        var response = service.update(PATIENT, id, request(TOMORROW));

        assertThat(response.title()).isEqualTo("Cardiologista");
        assertThat(response.scheduledAt()).isEqualTo(TOMORROW);
        assertThat(response.remindAt()).isEqualTo(TOMORROW.minusSeconds(3600));
    }

    @Test
    void updateOfAnotherPatientsAppointmentIsNotFound() {
        UUID id = UUID.randomUUID();
        when(access.requireWriter(PATIENT)).thenReturn(actor);
        when(repository.findByIdAndPatientId(id, PATIENT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(PATIENT, id, request(TOMORROW)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRemovesTheAppointment() {
        UUID id = UUID.randomUUID();
        Appointment existing = new Appointment();
        when(access.requireWriter(PATIENT)).thenReturn(actor);
        when(repository.findByIdAndPatientId(id, PATIENT)).thenReturn(Optional.of(existing));

        service.delete(PATIENT, id);

        verify(repository).delete(existing);
    }

    @Test
    void deleteOfUnknownAppointmentIsNotFound() {
        UUID id = UUID.randomUUID();
        when(access.requireWriter(PATIENT)).thenReturn(actor);
        when(repository.findByIdAndPatientId(id, PATIENT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(PATIENT, id)).isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).delete(any());
    }
}
