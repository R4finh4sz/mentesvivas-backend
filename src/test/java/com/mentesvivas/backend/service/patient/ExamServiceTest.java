package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.config.ExamProperties;
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
import com.mentesvivas.backend.service.patient.exam.ExamFileInspector;
import com.mentesvivas.backend.service.patient.exam.ExamFileStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.unit.DataSize;

import java.nio.charset.StandardCharsets;
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
    private static final byte[] PDF = "%PDF-1.7 conteudo".getBytes(StandardCharsets.US_ASCII);

    @Mock
    private PatientCareAccess access;
    @Mock
    private ExamRepository repository;
    @Mock
    private ExamFileStore fileStore;
    @Mock
    private AccessLog accessLog;

    private ExamService service;
    private User actor;

    @BeforeEach
    void setUp() {
        service = newService(DataSize.ofMegabytes(10));
        actor = new User();
        actor.setId(UUID.randomUUID());
    }

    private ExamService newService(DataSize limit) {
        return new ExamService(access, repository, fileStore, new ExamFileInspector(), accessLog,
                new ExamProperties(limit), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private UploadExamRequest request(LocalDate date) {
        return new UploadExamRequest(ExamCategory.BLOOD, "  Hemograma  ", date, "  ");
    }

    @Test
    void uploadSavesTheExamAndTheFile() {
        UUID examId = UUID.randomUUID();
        when(access.requireWriter(PATIENT)).thenReturn(actor);
        when(repository.save(any(Exam.class))).thenAnswer(invocation -> {
            Exam exam = invocation.getArgument(0);
            exam.setId(examId);
            return exam;
        });

        var response = service.upload(PATIENT, request(TODAY), "laudo.pdf", PDF);

        ArgumentCaptor<Exam> saved = ArgumentCaptor.forClass(Exam.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getPatientId()).isEqualTo(PATIENT);
        assertThat(saved.getValue().getUploadedBy()).isEqualTo(actor.getId());
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
        assertThat(saved.getValue().getTitle()).isEqualTo("Hemograma");
        assertThat(saved.getValue().getNotes()).isNull();
        assertThat(saved.getValue().getFileName()).isEqualTo("laudo.pdf");
        assertThat(saved.getValue().getSizeBytes()).isEqualTo(PDF.length);
        assertThat(response.fileType()).isEqualTo(ExamFileType.PDF);
        assertThat(response.contentType()).isEqualTo("application/pdf");
        verify(fileStore).save(examId, PDF);
    }

    @Test
    void fileNameLosesTheClientPathAndFallsBackWhenEmpty() {
        when(access.requireWriter(PATIENT)).thenReturn(actor);
        when(repository.save(any(Exam.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.upload(PATIENT, request(TODAY), "C:\\Users\\ana\\laudo.pdf", PDF).fileName())
                .isEqualTo("laudo.pdf");
        assertThat(service.upload(PATIENT, request(TODAY), null, PDF).fileName()).isEqualTo("exame");
    }

    @Test
    void futureExamDateIsRejected() {
        when(access.requireWriter(PATIENT)).thenReturn(actor);

        assertThatThrownBy(() -> service.upload(PATIENT, request(TODAY.plusDays(1)), "a.pdf", PDF))
                .isInstanceOf(ImpossibleValueException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void emptyFileIsRejected() {
        when(access.requireWriter(PATIENT)).thenReturn(actor);

        assertThatThrownBy(() -> service.upload(PATIENT, request(TODAY), "a.pdf", new byte[0]))
                .isInstanceOf(ImpossibleValueException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void fileAboveTheConfiguredLimitIsRejected() {
        service = newService(DataSize.ofBytes(PDF.length - 1));
        when(access.requireWriter(PATIENT)).thenReturn(actor);

        assertThatThrownBy(() -> service.upload(PATIENT, request(TODAY), "a.pdf", PDF))
                .isInstanceOf(ImpossibleValueException.class);
        verify(fileStore, never()).save(any(), any());
    }

    @Test
    void unsupportedContentIsRejectedEvenWithAPdfName() {
        when(access.requireWriter(PATIENT)).thenReturn(actor);

        assertThatThrownBy(() -> service.upload(PATIENT, request(TODAY), "a.pdf",
                "<html></html>".getBytes(StandardCharsets.US_ASCII)))
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
    void downloadReturnsTheFileAndLogsTheAccess() {
        Exam exam = exam();
        when(access.requireReader(PATIENT)).thenReturn(actor);
        when(repository.findByIdAndPatientId(exam.getId(), PATIENT)).thenReturn(Optional.of(exam));
        when(fileStore.find(exam.getId())).thenReturn(Optional.of(PDF));

        var file = service.download(PATIENT, exam.getId());

        assertThat(file.fileName()).isEqualTo("laudo.pdf");
        assertThat(file.contentType()).isEqualTo("application/pdf");
        assertThat(file.content()).isEqualTo(PDF);
        verify(accessLog).record(actor.getId(), PATIENT, PatientDataArea.DOCUMENTS);
    }

    @Test
    void downloadOfUnknownExamIsNotFound() {
        UUID id = UUID.randomUUID();
        when(access.requireReader(PATIENT)).thenReturn(actor);
        when(repository.findByIdAndPatientId(id, PATIENT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.download(PATIENT, id)).isInstanceOf(ResourceNotFoundException.class);
        verify(accessLog, never()).record(any(), any(), any());
    }

    @Test
    void downloadWithoutStoredFileIsNotFound() {
        Exam exam = exam();
        when(access.requireReader(PATIENT)).thenReturn(actor);
        when(repository.findByIdAndPatientId(exam.getId(), PATIENT)).thenReturn(Optional.of(exam));
        when(fileStore.find(exam.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.download(PATIENT, exam.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRemovesTheFileAndTheExam() {
        Exam exam = exam();
        when(access.requireWriter(PATIENT)).thenReturn(actor);
        when(repository.findByIdAndPatientId(exam.getId(), PATIENT)).thenReturn(Optional.of(exam));

        service.delete(PATIENT, exam.getId());

        verify(fileStore).delete(exam.getId());
        verify(repository).delete(exam);
    }

    @Test
    void deleteOfUnknownExamIsNotFound() {
        UUID id = UUID.randomUUID();
        when(access.requireWriter(PATIENT)).thenReturn(actor);
        when(repository.findByIdAndPatientId(id, PATIENT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(PATIENT, id)).isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).delete(any());
        verify(fileStore, never()).delete(any());
    }

    private Exam exam() {
        Exam exam = new Exam();
        exam.setId(UUID.randomUUID());
        exam.setFileName("laudo.pdf");
        exam.setContentType("application/pdf");
        return exam;
    }
}
