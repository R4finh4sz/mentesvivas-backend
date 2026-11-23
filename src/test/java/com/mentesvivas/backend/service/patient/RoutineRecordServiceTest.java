package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.dto.patient.RecordRoutineRequest;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.patient.MeasurementSource;
import com.mentesvivas.backend.entity.patient.Mood;
import com.mentesvivas.backend.entity.patient.RoutineRecord;
import com.mentesvivas.backend.entity.patient.RoutineType;
import com.mentesvivas.backend.entity.patient.SleepQuality;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.patient.ImpossibleValueException;
import com.mentesvivas.backend.repository.patient.RoutineRecordRepository;
import com.mentesvivas.backend.service.audit.AccessLog;
import com.mentesvivas.backend.service.patient.routine.RoutineValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoutineRecordServiceTest {

    private static final UUID PATIENT = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-23T12:00:00Z");

    @Mock
    private PatientCareAccess access;
    @Mock
    private RoutineRecordRepository repository;
    @Mock
    private RoutineValidator validator;
    @Mock
    private AccessLog accessLog;

    private RoutineRecordService service;
    private User author;

    @BeforeEach
    void setUp() {
        service = new RoutineRecordService(access, repository, validator, accessLog, Clock.fixed(NOW, ZoneOffset.UTC));
        author = new User();
        author.setId(UUID.randomUUID());
    }

    private RecordRoutineRequest sleep(MeasurementSource source, Instant occurredAt) {
        return new RecordRoutineRequest(RoutineType.SLEEP, null, SleepQuality.GOOD, new BigDecimal("7.46"), null,
                null, null, null, null, null, null, null, "dormiu bem", source, occurredAt);
    }

    @Test
    void recordsWithAuthorshipAndAutomaticTimestamps() {
        when(access.requireWriter(PATIENT)).thenReturn(author);
        when(repository.save(any(RoutineRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.record(PATIENT, sleep(MeasurementSource.DICTATED, null));

        ArgumentCaptor<RoutineRecord> saved = ArgumentCaptor.forClass(RoutineRecord.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getPatientId()).isEqualTo(PATIENT);
        assertThat(saved.getValue().getRecordedBy()).isEqualTo(author.getId());
        assertThat(saved.getValue().getOccurredAt()).isEqualTo(NOW);
        assertThat(saved.getValue().getRecordedAt()).isEqualTo(NOW);
        assertThat(response.sleepHours()).isEqualByComparingTo("7.5");
        assertThat(response.source()).isEqualTo(MeasurementSource.DICTATED);
        assertThat(response.checkAgainstOriginal()).isFalse();
    }

    @Test
    void keepsTheInformedOccurrenceTime() {
        when(access.requireWriter(PATIENT)).thenReturn(author);
        when(repository.save(any(RoutineRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Instant earlier = NOW.minusSeconds(3600);

        var response = service.record(PATIENT, sleep(null, earlier));

        assertThat(response.occurredAt()).isEqualTo(earlier);
        assertThat(response.recordedAt()).isEqualTo(NOW);
        assertThat(response.source()).isEqualTo(MeasurementSource.TYPED);
    }

    @Test
    void ocrValuesAreFlaggedToCheckAgainstTheOriginal() {
        when(access.requireWriter(PATIENT)).thenReturn(author);
        when(repository.save(any(RoutineRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.record(PATIENT, sleep(MeasurementSource.OCR, null)).checkAgainstOriginal()).isTrue();
    }

    @Test
    void invalidRecordIsNotSaved() {
        when(access.requireWriter(PATIENT)).thenReturn(author);
        RecordRoutineRequest request = sleep(null, null);
        doThrow(new ImpossibleValueException("invalido")).when(validator).validate(request);

        assertThatThrownBy(() -> service.record(PATIENT, request)).isInstanceOf(ImpossibleValueException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void historyLogsTheAccess() {
        when(access.requireReader(PATIENT)).thenReturn(author);
        RoutineRecord record = new RoutineRecord();
        record.setType(RoutineType.MOOD);
        record.setMood(Mood.JOY);
        record.setSource(MeasurementSource.TYPED);
        record.setOccurredAt(NOW);
        when(repository.findByPatientIdOrderByOccurredAtDesc(PATIENT)).thenReturn(List.of(record));

        var history = service.history(PATIENT, null);

        assertThat(history).hasSize(1);
        verify(accessLog).record(author.getId(), PATIENT, PatientDataArea.ROUTINE);
    }

    @Test
    void historyCanBeFilteredByType() {
        when(access.requireReader(PATIENT)).thenReturn(author);
        when(repository.findByPatientIdAndTypeOrderByOccurredAtDesc(PATIENT, RoutineType.MOOD)).thenReturn(List.of());

        assertThat(service.history(PATIENT, RoutineType.MOOD)).isEmpty();
        verify(repository, never()).findByPatientIdOrderByOccurredAtDesc(PATIENT);
    }
}
