package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.config.ExamProperties;
import com.mentesvivas.backend.dto.patient.ExamFileContent;
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
import com.mentesvivas.backend.service.patient.exam.ExamFileInspector;
import com.mentesvivas.backend.service.patient.exam.ExamFileStore;
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

    private static final int MAX_FILE_NAME = 255;

    private final PatientCareAccess access;
    private final ExamRepository repository;
    private final ExamFileStore fileStore;
    private final ExamFileInspector inspector;
    private final AccessLog accessLog;
    private final ExamProperties properties;
    private final Clock clock;

    @Transactional
    public ExamResponse upload(UUID patientId, UploadExamRequest request, String originalName, byte[] content) {
        User author = access.requireWriter(patientId);
        if (request.examDate().isAfter(LocalDate.now(clock.withZone(ZoneOffset.UTC)))) {
            throw new ImpossibleValueException("A data do exame nao pode ser futura.");
        }
        if (content.length == 0) {
            throw new ImpossibleValueException("Envie o arquivo do exame.");
        }
        if (content.length > properties.maxFileSize().toBytes()) {
            throw new ImpossibleValueException("O arquivo excede o tamanho maximo permitido.");
        }
        ExamFileInspector.Detected detected = inspector.inspect(content);

        Exam exam = new Exam();
        exam.setPatientId(patientId);
        exam.setCategory(request.category());
        exam.setFileType(detected.type());
        exam.setContentType(detected.contentType());
        exam.setSizeBytes(content.length);
        exam.setFileName(fileName(originalName));
        exam.setTitle(request.title().trim());
        exam.setExamDate(request.examDate());
        exam.setNotes(clean(request.notes()));
        exam.setUploadedBy(author.getId());
        exam.setCreatedAt(clock.instant());
        Exam saved = repository.save(exam);
        fileStore.save(saved.getId(), content);
        return PatientMapper.toResponse(saved);
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
    public ExamFileContent download(UUID patientId, UUID examId) {
        User reader = access.requireReader(patientId);
        Exam exam = find(patientId, examId);
        byte[] content = fileStore.find(exam.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Arquivo do exame nao encontrado."));
        accessLog.record(reader.getId(), patientId, PatientDataArea.DOCUMENTS);
        return new ExamFileContent(exam.getFileName(), exam.getContentType(), content);
    }

    @Transactional
    public void delete(UUID patientId, UUID examId) {
        access.requireWriter(patientId);
        Exam exam = find(patientId, examId);
        fileStore.delete(exam.getId());
        repository.delete(exam);
    }

    private Exam find(UUID patientId, UUID examId) {
        return repository.findByIdAndPatientId(examId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Exame nao encontrado."));
    }

    private String fileName(String originalName) {
        String name = originalName == null ? "" : originalName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).strip();
        if (name.isEmpty()) {
            return "exame";
        }
        return name.length() > MAX_FILE_NAME ? name.substring(0, MAX_FILE_NAME) : name;
    }

    private String clean(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.trim();
    }
}
