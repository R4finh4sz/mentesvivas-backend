package com.mentesvivas.backend.service.medication;

import com.mentesvivas.backend.dto.medication.AdjustStockRequest;
import com.mentesvivas.backend.dto.medication.CreateMedicationRequest;
import com.mentesvivas.backend.dto.medication.MedicationResponse;
import com.mentesvivas.backend.dto.medication.MedicationWarning;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.medication.Medication;
import com.mentesvivas.backend.entity.medication.MedicationOrigin;
import com.mentesvivas.backend.entity.medication.StockAdjustment;
import com.mentesvivas.backend.entity.patient.Allergy;
import com.mentesvivas.backend.entity.patient.AllergyStatus;
import com.mentesvivas.backend.entity.patient.PatientProfile;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.exception.patient.ValueNeedsConfirmationException;
import com.mentesvivas.backend.repository.medication.MedicationRepository;
import com.mentesvivas.backend.repository.medication.StockAdjustmentRepository;
import com.mentesvivas.backend.repository.patient.AllergyRepository;
import com.mentesvivas.backend.repository.patient.PatientProfileRepository;
import com.mentesvivas.backend.service.audit.AccessLog;
import com.mentesvivas.backend.service.patient.PatientCareAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MedicationService {

    private final PatientCareAccess access;
    private final MedicationRepository repository;
    private final StockAdjustmentRepository adjustmentRepository;
    private final AllergyRepository allergyRepository;
    private final PatientProfileRepository profileRepository;
    private final MedicationAllergyChecker allergyChecker;
    private final MedicationResponseFactory responseFactory;
    private final AccessLog accessLog;
    private final Clock clock;

    @Transactional
    public MedicationResponse create(UUID patientId, CreateMedicationRequest request) {
        User author = access.requireWriter(patientId);
        rejectDuplicate(patientId, request);
        List<MedicationWarning> warnings = checkAllergies(patientId, request);

        Medication medication = new Medication();
        medication.setPatientId(patientId);
        medication.setName(request.name());
        medication.setStrength(request.strength());
        medication.setUnitsPerDose(request.unitsPerDose());
        medication.setTimes(request.times().stream().distinct().sorted().toList());
        medication.setStockQuantity(request.stockQuantity());
        medication.setOrigin(MedicationOrigin.MANUAL);
        medication.setActive(true);
        medication.setCreatedBy(author.getId());
        medication.setCreatedAt(clock.instant());
        return responseFactory.create(repository.save(medication), warnings);
    }

    @Transactional
    public List<MedicationResponse> list(UUID patientId, boolean includeInactive) {
        User reader = access.requireReader(patientId);
        accessLog.record(reader.getId(), patientId, PatientDataArea.MEDICATIONS);
        List<Medication> medications = includeInactive
                ? repository.findByPatientIdOrderByNameAsc(patientId)
                : repository.findByPatientIdAndActiveTrueOrderByNameAsc(patientId);
        return medications.stream().map(medication -> responseFactory.create(medication, List.of())).toList();
    }

    @Transactional
    public MedicationResponse adjustStock(UUID patientId, UUID medicationId, AdjustStockRequest request) {
        User author = access.requireWriter(patientId);
        Medication medication = find(patientId, medicationId);
        adjustmentRepository.save(new StockAdjustment(medication.getId(), medication.getStockQuantity(),
                request.quantity(), request.justification().strip(), author.getId(), clock.instant()));
        medication.setStockQuantity(request.quantity());
        return responseFactory.create(medication, List.of());
    }

    @Transactional
    public void deactivate(UUID patientId, UUID medicationId) {
        access.requireWriter(patientId);
        find(patientId, medicationId).setActive(false);
    }

    private Medication find(UUID patientId, UUID medicationId) {
        return repository.findByIdAndPatientId(medicationId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Medicamento nao encontrado."));
    }

    private void rejectDuplicate(UUID patientId, CreateMedicationRequest request) {
        boolean duplicated = repository.findByPatientIdAndActiveTrueOrderByNameAsc(patientId).stream()
                .anyMatch(existing -> sameMedication(existing, request));
        if (duplicated) {
            throw new ConflictException("Este medicamento ja esta na rotina do paciente.");
        }
    }

    private boolean sameMedication(Medication existing, CreateMedicationRequest request) {
        return key(existing.getName()).equals(key(request.name()))
                && key(existing.getStrength()).equals(key(request.strength()));
    }

    private String key(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }

    private List<MedicationWarning> checkAllergies(UUID patientId, CreateMedicationRequest request) {
        List<MedicationWarning> warnings = new ArrayList<>();
        Optional<Allergy> conflict = allergyChecker.findConflict(request.name(),
                allergyRepository.findByPatientIdOrderByCreatedAtAsc(patientId));
        if (conflict.isPresent()) {
            if (!request.allergyRiskConfirmed()) {
                throw new ValueNeedsConfirmationException(
                        "Possivel alergia a \"%s\" registrada para este paciente. Confirme para salvar."
                                .formatted(conflict.get().getDescription()));
            }
            warnings.add(MedicationWarning.ALLERGY_MATCH_CONFIRMED);
        }
        AllergyStatus status = profileRepository.findById(patientId)
                .map(PatientProfile::getAllergyStatus).orElse(AllergyStatus.NOT_INFORMED);
        if (status == AllergyStatus.NOT_INFORMED) {
            warnings.add(MedicationWarning.ALLERGY_NOT_INFORMED);
        }
        return warnings;
    }
}
