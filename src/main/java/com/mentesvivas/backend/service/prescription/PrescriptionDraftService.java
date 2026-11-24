package com.mentesvivas.backend.service.prescription;

import com.mentesvivas.backend.dto.prescription.PrescriptionItemRequest;
import com.mentesvivas.backend.dto.prescription.PrescriptionResponse;
import com.mentesvivas.backend.dto.prescription.ReplacePrescriptionRequest;
import com.mentesvivas.backend.dto.prescription.SavePrescriptionRequest;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.patient.Allergy;
import com.mentesvivas.backend.entity.prescription.Prescription;
import com.mentesvivas.backend.entity.prescription.PrescriptionItem;
import com.mentesvivas.backend.entity.prescription.PrescriptionStatus;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.patient.AllergyRepository;
import com.mentesvivas.backend.repository.prescription.PrescriptionItemRepository;
import com.mentesvivas.backend.repository.prescription.PrescriptionRepository;
import com.mentesvivas.backend.service.audit.AccessLog;
import com.mentesvivas.backend.service.audit.AuditTrail;
import com.mentesvivas.backend.service.clinical.DoctorPatientAccess;
import com.mentesvivas.backend.security.HealthDataConsentAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrescriptionDraftService {

    private final DoctorPatientAccess doctorAccess;
    private final HealthDataConsentAccess healthConsent;
    private final PrescriptionRepository repository;
    private final PrescriptionItemRepository itemRepository;
    private final AllergyRepository allergyRepository;
    private final PrescriptionItemGuard itemGuard;
    private final PrescriptionViewFactory viewFactory;
    private final AccessLog accessLog;
    private final AuditTrail auditTrail;
    private final Clock clock;

    @Transactional
    public PrescriptionResponse create(UUID patientId, SavePrescriptionRequest request) {
        User doctor = doctorAccess.requireDoctorOf(patientId);
        Prescription prescription = new Prescription();
        prescription.setPatientId(patientId);
        prescription.setDoctorId(doctor.getId());
        prescription.setStatus(PrescriptionStatus.DRAFT);
        prescription.setCreatedAt(clock.instant());
        prescription.setUpdatedAt(prescription.getCreatedAt());
        return fill(prescription, request);
    }

    @Transactional
    public PrescriptionResponse update(UUID prescriptionId, SavePrescriptionRequest request) {
        Prescription prescription = ownedBy(prescriptionId);
        requireDraft(prescription);
        prescription.setUpdatedAt(clock.instant());
        return fill(prescription, request);
    }

    @Transactional
    public PrescriptionResponse get(UUID prescriptionId) {
        Prescription prescription = ownedBy(prescriptionId);
        accessLog.record(prescription.getDoctorId(), prescription.getPatientId(), PatientDataArea.PRESCRIPTIONS);
        return viewFactory.build(prescription);
    }

    @Transactional
    public List<PrescriptionResponse> list(UUID patientId) {
        User doctor = doctorAccess.requireDoctorOf(patientId);
        accessLog.record(doctor.getId(), patientId, PatientDataArea.PRESCRIPTIONS);
        return repository.findByPatientIdAndDoctorIdOrderByCreatedAtDesc(patientId, doctor.getId()).stream()
                .map(viewFactory::build)
                .toList();
    }

    @Transactional
    public PrescriptionResponse replace(UUID prescriptionId, ReplacePrescriptionRequest request) {
        Prescription original = ownedBy(prescriptionId);
        if (original.isDraft()) {
            throw new ConflictException("Apenas receitas assinadas podem ser substituidas.");
        }
        if (original.isReplaced() || repository.existsByReplacesIdAndStatus(original.getId(), PrescriptionStatus.DRAFT)) {
            throw new ConflictException("Esta receita ja foi substituida ou tem uma substituicao em andamento.");
        }
        Prescription draft = new Prescription();
        draft.setPatientId(original.getPatientId());
        draft.setDoctorId(original.getDoctorId());
        draft.setKind(original.getKind());
        draft.setStatus(PrescriptionStatus.DRAFT);
        draft.setOrientations(original.getOrientations());
        draft.setReplacesId(original.getId());
        draft.setReplacementJustification(request.justification());
        draft.setCreatedAt(clock.instant());
        draft.setUpdatedAt(draft.getCreatedAt());
        Prescription saved = repository.save(draft);
        copyItems(original, saved);
        auditTrail.record(AuditRecord.of(AuditEventType.PRESCRIPTION_REPLACED, original.getDoctorId())
                .forPatient(original.getPatientId()).withValues(original.getId().toString(),
                        saved.getId().toString()));
        return viewFactory.build(saved);
    }

    public Prescription ownedBy(UUID prescriptionId) {
        User doctor = doctorAccessUser();
        Prescription prescription = repository.findByIdAndDoctorId(prescriptionId, doctor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Receita nao encontrada."));
        doctorAccess.requireDoctorOf(prescription.getPatientId());
        healthConsent.requireActive(prescription.getPatientId());
        return prescription;
    }

    private User doctorAccessUser() {
        return doctorAccess.requireDoctor();
    }

    private void requireDraft(Prescription prescription) {
        if (!prescription.isDraft()) {
            throw new ConflictException("Uma receita assinada nunca e alterada. Emita uma substituicao.");
        }
    }

    private PrescriptionResponse fill(Prescription prescription, SavePrescriptionRequest request) {
        List<Allergy> allergies = allergyRepository.findByPatientIdOrderByCreatedAtAsc(prescription.getPatientId());
        request.items().forEach(item -> itemGuard.check(item.medicationName(), item.allergyOverrideJustification(),
                request.allergyRiskConfirmed(), allergies));
        prescription.setKind(request.kindOrDefault());
        prescription.setOrientations(request.orientations());
        Prescription saved = repository.save(prescription);
        itemRepository.deleteAllByPrescriptionId(saved.getId());
        int position = 0;
        for (PrescriptionItemRequest item : request.items()) {
            itemRepository.save(toItem(saved.getId(), position++, item));
        }
        return viewFactory.build(saved);
    }

    private void copyItems(Prescription original, Prescription target) {
        for (PrescriptionItem item : itemRepository.findByPrescriptionIdOrderByPositionAsc(original.getId())) {
            PrescriptionItem copy = new PrescriptionItem();
            copy.setPrescriptionId(target.getId());
            copy.setPosition(item.getPosition());
            copy.setMedicationName(item.getMedicationName());
            copy.setPresentation(item.getPresentation());
            copy.setDose(item.getDose());
            copy.setRoute(item.getRoute());
            copy.setFrequency(item.getFrequency());
            copy.setDuration(item.getDuration());
            copy.setQuantity(item.getQuantity());
            copy.setInstructions(item.getInstructions());
            copy.setContinuousUse(item.isContinuousUse());
            copy.setAllergyOverrideJustification(item.getAllergyOverrideJustification());
            itemRepository.save(copy);
        }
    }

    private PrescriptionItem toItem(UUID prescriptionId, int position, PrescriptionItemRequest request) {
        PrescriptionItem item = new PrescriptionItem();
        item.setPrescriptionId(prescriptionId);
        item.setPosition(position);
        item.setMedicationName(request.medicationName());
        item.setPresentation(request.presentation());
        item.setDose(request.dose());
        item.setRoute(request.route());
        item.setFrequency(request.frequency());
        item.setDuration(request.duration());
        item.setQuantity(request.quantity());
        item.setInstructions(request.instructions());
        item.setContinuousUse(request.isContinuousUse());
        item.setAllergyOverrideJustification(request.allergyOverrideJustification());
        return item;
    }
}
