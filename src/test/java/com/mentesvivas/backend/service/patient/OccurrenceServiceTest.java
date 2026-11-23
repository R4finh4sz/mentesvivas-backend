package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.dto.patient.RecordOccurrenceRequest;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.patient.MeasurementSource;
import com.mentesvivas.backend.entity.patient.Occurrence;
import com.mentesvivas.backend.entity.patient.OccurrenceSeverity;
import com.mentesvivas.backend.entity.patient.OccurrenceType;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.patient.ImpossibleValueException;
import com.mentesvivas.backend.repository.patient.OccurrenceRepository;
import com.mentesvivas.backend.service.audit.AccessLog;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OccurrenceServiceTest {

    private static final UUID PATIENT = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-23T12:00:00Z");

    @Mock
    private PatientCareAccess access;
    @Mock
    private OccurrenceRepository repository;
    @Mock
    private AccessLog accessLog;

    private OccurrenceService service;
    private User author;

    @BeforeEach
    void setUp() {
        service = new OccurrenceService(access, repository, accessLog, Clock.fixed(NOW, ZoneOffset.UTC));
        author = new User();
        author.setId(UUID.randomUUID());
    }

    private RecordOccurrenceRequest fall(MeasurementSource source) {
        return new RecordOccurrenceRequest(OccurrenceType.FALL, OccurrenceSeverity.MODERATE, "  caiu no banheiro  ",
                true, source);
    }

    @Test
    void recordsWithAuthorshipAndAutomaticTimestamp() {
        when(access.requireWriter(PATIENT)).thenReturn(author);
        when(repository.save(any(Occurrence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.record(PATIENT, fall(null));

        ArgumentCaptor<Occurrence> saved = ArgumentCaptor.forClass(Occurrence.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getPatientId()).isEqualTo(PATIENT);
        assertThat(saved.getValue().getRecordedBy()).isEqualTo(author.getId());
        assertThat(saved.getValue().getOccurredAt()).isEqualTo(NOW);
        assertThat(saved.getValue().getDescription()).isEqualTo("caiu no banheiro");
        assertThat(response.needsHospital()).isTrue();
        assertThat(response.source()).isEqualTo(MeasurementSource.TYPED);
    }

    @Test
    void keepsTheDictatedOrigin() {
        when(access.requireWriter(PATIENT)).thenReturn(author);
        when(repository.save(any(Occurrence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.record(PATIENT, fall(MeasurementSource.DICTATED)).source())
                .isEqualTo(MeasurementSource.DICTATED);
    }

    @Test
    void ocrOriginIsRejected() {
        when(access.requireWriter(PATIENT)).thenReturn(author);

        assertThatThrownBy(() -> service.record(PATIENT, fall(MeasurementSource.OCR)))
                .isInstanceOf(ImpossibleValueException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void historyLogsTheAccess() {
        when(access.requireReader(PATIENT)).thenReturn(author);
        Occurrence occurrence = new Occurrence();
        occurrence.setType(OccurrenceType.FALL);
        occurrence.setSeverity(OccurrenceSeverity.MILD);
        when(repository.findByPatientIdOrderByOccurredAtDesc(PATIENT)).thenReturn(List.of(occurrence));

        assertThat(service.history(PATIENT, null, null)).hasSize(1);
        verify(accessLog).record(author.getId(), PATIENT, PatientDataArea.OCCURRENCES);
    }

    @Test
    void blankKeywordIsTreatedAsNoFilter() {
        when(access.requireReader(PATIENT)).thenReturn(author);
        when(repository.findByPatientIdOrderByOccurredAtDesc(PATIENT)).thenReturn(List.of());

        assertThat(service.history(PATIENT, null, "   ")).isEmpty();
    }

    @Test
    void historyCanBeFilteredByType() {
        when(access.requireReader(PATIENT)).thenReturn(author);
        when(repository.findByPatientIdAndTypeOrderByOccurredAtDesc(PATIENT, OccurrenceType.FALL))
                .thenReturn(List.of());

        assertThat(service.history(PATIENT, OccurrenceType.FALL, null)).isEmpty();
        verify(repository, never()).findByPatientIdOrderByOccurredAtDesc(PATIENT);
    }

    @Test
    void historyCanBeSearchedByKeyword() {
        when(access.requireReader(PATIENT)).thenReturn(author);
        when(repository.findByPatientIdAndDescriptionContainingIgnoreCaseOrderByOccurredAtDesc(PATIENT, "banheiro"))
                .thenReturn(List.of());

        assertThat(service.history(PATIENT, null, " banheiro ")).isEmpty();
    }

    @Test
    void historyCanCombineTypeAndKeyword() {
        when(access.requireReader(PATIENT)).thenReturn(author);
        when(repository.findByPatientIdAndTypeAndDescriptionContainingIgnoreCaseOrderByOccurredAtDesc(
                PATIENT, OccurrenceType.FALL, "banheiro")).thenReturn(List.of());

        assertThat(service.history(PATIENT, OccurrenceType.FALL, "banheiro")).isEmpty();
    }
}
