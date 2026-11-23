package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.dto.patient.UploadExamRequest;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.patient.Exam;
import com.mentesvivas.backend.entity.patient.ExamCategory;
import com.mentesvivas.backend.entity.patient.ExamFileType;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.exception.patient.ImpossibleValueException;
import com.mentesvivas.backend.repository.patient.ExamRepository;
import com.mentesvivas.backend.service.audit.AccessLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
class ExamServiceTest {

    private static final UUID PATIENT = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-23T12:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 23);

    @Mock
    private PatientCareAccess access;
    @Mock
    private ExamRepository repository;
    @Mock
    private AccessLog accessLog;

    private ExamService service;
    private User actor;

    @BeforeEach
    void setUp() {
        service = new ExamService(access, repository, accessLog, Clock.fixed(NOW, ZoneOffset.UTC));
        actor = new User();
        actor.setId(UUID.randomUUID());
    }

    private UploadExamRequest request(LocalDate date) {
        return new UploadExamRequest(ExamCategory.BLOOD, ExamFileType.PDF, "  Hemograma  ",
                " https://arquivos.exemplo.com/hemograma.pdf ", date, "  ");
    }

    @Test
    void uploadSavesTheExamWithTheAuthorAndTrimmedFields() {
        when(access.requireWriter(PATIENT)).thenReturn(actor);
        when(repository.save(any(Exam.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.upload(PATIENT, request(TODAY));

        ArgumentCaptor<Exam> saved = ArgumentCaptor.forClass(Exam.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getPatientId()).isEqualTo(PATIENT);
        assertThat(saved.getValue().getUploadedBy()).isEqualTo(actor.getId());
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
        assertThat(saved.getValue().getTitle()).isEqualTo("Hemograma");
        assertThat(saved.getValue().getFileUrl()).isEqualTo("https://arquivos.exemplo.com/hemograma.pdf");
        assertThat(saved.getValue().getNotes()).isNull();
        assertThat(response.category()).isEqualTo(ExamCategory.BLOOD);
        assertThat(response.fileType()).isEqualTo(ExamFileType.PDF);
    }

    @Test
    void futureExamDateIsRejected() {
        when(access.requireWriter(PATIENT)).thenReturn(actor);

        assertThatThrownBy(() -> service.upload(PATIENT, request(TODAY.plusDays(1))))
                .isInstanceOf(ImpossibleValueException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void listWithoutCategoryReturnsEverythingAndLogsTheAccess() {
        when(access.requireReader(PATIENT)).thenReturn(actor);
        when(repository.findByPatientIdOrderByExamDateDescCreatedAtDesc(PATIENT)).thenReturn(List.of(new Exam()));

        assertThat(service.list(PATIENT, null)).hasSize(1);
        verify(accessLog).record(actor.getId(), PATIENT, PatientDataArea.DOCUMENTS);
    }

    @Test
    void listWithCategoryFiltersByIt() {
        when(access.requireReader(PATIENT)).thenReturn(actor);
        when(repository.findByPatientIdAndCategoryOrderByExamDateDescCreatedAtDesc(PATIENT, ExamCategory.IMAGING))
                .thenReturn(List.of());

        assertThat(service.list(PATIENT, ExamCategory.IMAGING)).isEmpty();
        verify(repository, never()).findByPatientIdOrderByExamDateDescCreatedAtDesc(any());
    }

    @Test
    void deleteRemovesTheExam() {
        UUID id = UUID.randomUUID();
        Exam existing = new Exam();
        when(access.requireWriter(PATIENT)).thenReturn(actor);
        when(repository.findByIdAndPatientId(id, PATIENT)).thenReturn(Optional.of(existing));

        service.delete(PATIENT, id);

        verify(repository).delete(existing);
    }

    @Test
    void deleteOfUnknownExamIsNotFound() {
        UUID id = UUID.randomUUID();
        when(access.requireWriter(PATIENT)).thenReturn(actor);
        when(repository.findByIdAndPatientId(id, PATIENT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(PATIENT, id)).isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).delete(any());
    }
}
