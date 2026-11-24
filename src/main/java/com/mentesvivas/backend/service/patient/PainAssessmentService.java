package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.dto.patient.PainAssessment;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.patient.PainRecord;
import com.mentesvivas.backend.entity.patient.PainScale;
import com.mentesvivas.backend.entity.patient.PainScaleSelection;
import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.exception.patient.ImpossibleValueException;
import com.mentesvivas.backend.repository.patient.PainRecordRepository;
import com.mentesvivas.backend.repository.patient.PainScaleSelectionRepository;
import com.mentesvivas.backend.service.audit.AccessLog;
import com.mentesvivas.backend.service.audit.AuditTrail;
import com.mentesvivas.backend.service.clinical.DoctorPatientAccess;
import com.mentesvivas.backend.service.link.CaregiverPatientAccess;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PainAssessmentService {

    private final DoctorPatientAccess doctors;
    private final CaregiverPatientAccess caregivers;
    private final PatientCareAccess care;
    private final PainScaleSelectionRepository selections;
    private final PainRecordRepository records;
    private final EntityManager entityManager;
    private final AccessLog accessLog;
    private final AuditTrail audit;
    private final Clock clock;

    @Transactional
    public PainAssessment.Selection select(UUID patientId, PainScale scale) {
        User doctor = doctors.requireDoctorOf(patientId);
        lockPatient(patientId);
        PainScaleSelection selection = selections.findById(patientId)
                .orElseGet(() -> new PainScaleSelection(patientId));
        PainScale previous = selection.getScale();
        if (previous == scale) {
            return view(selection);
        }
        selection.select(scale, doctor.getId(), clock.instant());
        selections.save(selection);
        audit.record(AuditRecord.of(AuditEventType.PAIN_SCALE_CHANGED, doctor.getId()).forPatient(patientId)
                .withValues(previous == null ? null : previous.name(), scale.name()));
        return view(selection);
    }

    @Transactional
    public PainAssessment.Selection selection(UUID patientId) {
        recordRead(patientId);
        return selections.findById(patientId).map(PainAssessmentService::view)
                .orElseGet(() -> new PainAssessment.Selection(null, List.of(), null));
    }

    @Transactional
    public PainAssessment.Recorded record(UUID patientId, PainScale scale, int score) {
        User caregiver = caregivers.requireRepresentativeOf(patientId);
        lockPatient(patientId);
        PainScale selected = selections.findById(patientId).map(PainScaleSelection::getScale)
                .orElseThrow(() -> new ConflictException("O médico precisa definir a escala de dor."));
        if (scale != selected) {
            throw new ConflictException("A escala de dor mudou. Consulte a escala atual.");
        }
        if (!selected.scores().contains(score)) {
            throw new ImpossibleValueException("A pontuação não é válida para a escala selecionada.");
        }
        PainRecord saved = records.save(new PainRecord(patientId, scale, score, caregiver.getId(), clock.instant()));
        audit.record(AuditRecord.of(AuditEventType.PAIN_RECORDED, caregiver.getId()).forPatient(patientId)
                .onBehalfOf(patientId).withValues(null, saved.getId().toString()));
        return view(saved);
    }

    @Transactional
    public List<PainAssessment.Recorded> history(UUID patientId, int page, int size) {
        recordRead(patientId);
        return records.findByPatientIdOrderByRecordedAtDescIdDesc(patientId, PageRequest.of(page, size)).stream()
                .map(PainAssessmentService::view).toList();
    }

    private void lockPatient(UUID patientId) {
        if (entityManager.find(User.class, patientId, LockModeType.PESSIMISTIC_WRITE) == null) {
            throw new ResourceNotFoundException("Paciente não encontrado.");
        }
    }

    private void recordRead(UUID patientId) {
        User actor = care.requireReader(patientId);
        if (actor.getRole() != Role.PATIENT) {
            accessLog.record(actor.getId(), patientId, PatientDataArea.PAIN);
        }
    }

    private static PainAssessment.Selection view(PainScaleSelection selection) {
        return new PainAssessment.Selection(selection.getScale(), selection.getScale().scores(),
                selection.getSelectedAt());
    }

    private static PainAssessment.Recorded view(PainRecord record) {
        return new PainAssessment.Recorded(record.getId(), record.getScale(), record.getScore(),
                record.getRecordedBy(), record.getRecordedAt());
    }
}
