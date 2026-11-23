package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.entity.patient.ChangeType;
import com.mentesvivas.backend.entity.patient.SensitiveDataChange;
import com.mentesvivas.backend.entity.patient.SensitiveField;
import com.mentesvivas.backend.repository.patient.SensitiveDataChangeRepository;
import com.mentesvivas.backend.service.audit.AuditTrail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SensitiveChangeRecorder {

    private final SensitiveDataChangeRepository repository;
    private final AuditTrail auditTrail;

    public void record(UUID patientId, SensitiveField field, ChangeType type, String summary, UUID authorId) {
        record(patientId, field, type, summary, authorId, null, summary);
    }

    public void record(UUID patientId, SensitiveField field, ChangeType type, String summary, UUID authorId,
                       String before, String after) {
        repository.save(new SensitiveDataChange(patientId, field, type, summary, authorId, Instant.now()));
        auditTrail.record(AuditRecord.of(auditType(field, type), authorId)
                .forPatient(patientId)
                .withValues(before, after));
    }

    private AuditEventType auditType(SensitiveField field, ChangeType type) {
        return switch (field) {
            case ALLERGY -> switch (type) {
                case CREATED -> AuditEventType.ALLERGY_CREATED;
                case DELETED -> AuditEventType.ALLERGY_DELETED;
                default -> AuditEventType.ALLERGY_UPDATED;
            };
            case ALLERGY_STATUS -> AuditEventType.ALLERGY_STATUS_CHANGED;
            case DECLARED_DISEASE -> AuditEventType.DECLARED_DISEASE_CHANGED;
            case EMERGENCY_CONTACTS -> AuditEventType.EMERGENCY_CONTACTS_REPLACED;
            case HYDRATION_GOAL -> AuditEventType.HYDRATION_GOAL_CHANGED;
        };
    }
}
