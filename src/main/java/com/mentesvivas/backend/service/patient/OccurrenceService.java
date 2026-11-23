package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.dto.patient.OccurrenceResponse;
import com.mentesvivas.backend.dto.patient.RecordOccurrenceRequest;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.patient.MeasurementSource;
import com.mentesvivas.backend.entity.patient.Occurrence;
import com.mentesvivas.backend.entity.patient.OccurrenceType;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.patient.ImpossibleValueException;
import com.mentesvivas.backend.mapper.patient.PatientMapper;
import com.mentesvivas.backend.repository.patient.OccurrenceRepository;
import com.mentesvivas.backend.service.audit.AccessLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OccurrenceService {

    private final PatientCareAccess access;
    private final OccurrenceRepository repository;
    private final AccessLog accessLog;
    private final Clock clock;

    @Transactional
    public OccurrenceResponse record(UUID patientId, RecordOccurrenceRequest request) {
        User author = access.requireWriter(patientId);
        MeasurementSource source = request.source() == null ? MeasurementSource.TYPED : request.source();
        if (source == MeasurementSource.OCR) {
            throw new ImpossibleValueException("A ocorrencia aceita apenas texto digitado ou ditado.");
        }

        Occurrence occurrence = new Occurrence();
        occurrence.setPatientId(patientId);
        occurrence.setType(request.type());
        occurrence.setSeverity(request.severity());
        occurrence.setDescription(request.description().trim());
        occurrence.setNeedsHospital(request.needsHospital());
        occurrence.setSource(source);
        occurrence.setRecordedBy(author.getId());
        occurrence.setOccurredAt(clock.instant());
        return PatientMapper.toResponse(repository.save(occurrence));
    }

    @Transactional
    public List<OccurrenceResponse> history(UUID patientId, OccurrenceType type, String keyword) {
        User reader = access.requireReader(patientId);
        accessLog.record(reader.getId(), patientId, PatientDataArea.OCCURRENCES);
        String term = keyword == null ? "" : keyword.trim();
        return find(patientId, type, term).stream().map(PatientMapper::toResponse).toList();
    }

    private List<Occurrence> find(UUID patientId, OccurrenceType type, String term) {
        if (term.isEmpty()) {
            return type == null
                    ? repository.findByPatientIdOrderByOccurredAtDesc(patientId)
                    : repository.findByPatientIdAndTypeOrderByOccurredAtDesc(patientId, type);
        }
        return type == null
                ? repository.findByPatientIdAndDescriptionContainingIgnoreCaseOrderByOccurredAtDesc(patientId, term)
                : repository.findByPatientIdAndTypeAndDescriptionContainingIgnoreCaseOrderByOccurredAtDesc(
                        patientId, type, term);
    }
}
