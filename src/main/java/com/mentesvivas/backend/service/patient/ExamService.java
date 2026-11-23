package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.dto.patient.ExamResponse;
import com.mentesvivas.backend.dto.patient.UploadExamRequest;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.patient.Exam;
import com.mentesvivas.backend.entity.patient.ExamCategory;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.exception.patient.ImpossibleValueException;
import com.mentesvivas.backend.mapper.patient.PatientMapper;
import com.mentesvivas.backend.repository.patient.ExamRepository;
import com.mentesvivas.backend.service.audit.AccessLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final PatientCareAccess access;
    private final ExamRepository repository;
    private final AccessLog accessLog;
    private final Clock clock;

    @Transactional
    public ExamResponse upload(UUID patientId, UploadExamRequest request) {
        User author = access.requireWriter(patientId);
        if (request.examDate().isAfter(LocalDate.now(clock.withZone(ZoneOffset.UTC)))) {
            throw new ImpossibleValueException("A data do exame nao pode ser futura.");
        }

        Exam exam = new Exam();
        exam.setPatientId(patientId);
        exam.setCategory(request.category());
        exam.setFileType(request.fileType());
        exam.setTitle(request.title().trim());
        exam.setFileUrl(request.fileUrl().trim());
        exam.setExamDate(request.examDate());
        exam.setNotes(clean(request.notes()));
        exam.setUploadedBy(author.getId());
        exam.setCreatedAt(clock.instant());
        return PatientMapper.toResponse(repository.save(exam));
    }

    @Transactional
    public List<ExamResponse> list(UUID patientId, ExamCategory category) {
        User reader = access.requireReader(patientId);
        accessLog.record(reader.getId(), patientId, PatientDataArea.DOCUMENTS);
        List<Exam> found = category == null
                ? repository.findByPatientIdOrderByExamDateDescCreatedAtDesc(patientId)
                : repository.findByPatientIdAndCategoryOrderByExamDateDescCreatedAtDesc(patientId, category);
        return found.stream().map(PatientMapper::toResponse).toList();
    }

    @Transactional
    public void delete(UUID patientId, UUID examId) {
        access.requireWriter(patientId);
        Exam exam = repository.findByIdAndPatientId(examId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Exame nao encontrado."));
        repository.delete(exam);
    }

    private String clean(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.trim();
    }
}
