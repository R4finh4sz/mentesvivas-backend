package com.mentesvivas.backend.service.prescription;

import com.mentesvivas.backend.dto.medication.MedicationResponse;
import com.mentesvivas.backend.dto.medication.MedicationWarning;
import com.mentesvivas.backend.dto.prescription.AddToRoutineRequest;
import com.mentesvivas.backend.entity.medication.Medication;
import com.mentesvivas.backend.entity.medication.MedicationOrigin;
import com.mentesvivas.backend.entity.patient.Allergy;
import com.mentesvivas.backend.entity.prescription.DocumentKind;
import com.mentesvivas.backend.entity.prescription.Prescription;
import com.mentesvivas.backend.entity.prescription.PrescriptionItem;
import com.mentesvivas.backend.entity.prescription.PrescriptionStatus;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.exception.patient.ValueNeedsConfirmationException;
import com.mentesvivas.backend.repository.medication.MedicationRepository;
import com.mentesvivas.backend.repository.patient.AllergyRepository;
import com.mentesvivas.backend.repository.prescription.PrescriptionItemRepository;
import com.mentesvivas.backend.repository.prescription.PrescriptionRepository;
import com.mentesvivas.backend.service.medication.MedicationAllergyChecker;
import com.mentesvivas.backend.service.medication.MedicationResponseFactory;
import com.mentesvivas.backend.service.patient.PatientCareAccess;
import com.mentesvivas.backend.util.PlainText;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrescriptionRoutineService {

    private final PatientCareAccess access;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository itemRepository;
    private final MedicationRepository medicationRepository;
    private final AllergyRepository allergyRepository;
    private final MedicationAllergyChecker allergyChecker;
    private final MedicationResponseFactory responseFactory;
    private final Clock clock;

    @Transactional
    public MedicationResponse addToRoutine(UUID patientId, UUID prescriptionId, UUID itemId,
                                           AddToRoutineRequest request) {
        User author = access.requireWriter(patientId);
        Prescription prescription = prescriptionRepository
                .findByIdAndPatientIdAndStatus(prescriptionId, patientId, PrescriptionStatus.SIGNED)
                .orElseThrow(() -> new ResourceNotFoundException("Receita nao encontrada."));
        if (prescription.getKind() != DocumentKind.PRESCRIPTION || prescription.isReplaced()) {
            throw new ConflictException("Esta receita nao pode entrar na rotina: so receitas vigentes de medicamentos.");
        }
        PrescriptionItem item = itemRepository.findByIdAndPrescriptionId(itemId, prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Item da receita nao encontrado."));
        rejectDuplicate(patientId, item);
        List<MedicationWarning> warnings = checkAllergies(patientId, item, request);

        Medication medication = new Medication();
        medication.setPatientId(patientId);
        medication.setName(item.getMedicationName());
        medication.setStrength(item.getPresentation());
        medication.setUnitsPerDose(request.unitsPerDose());
        medication.setTimes(request.times().stream().distinct().sorted().toList());
        medication.setStockQuantity(request.stockQuantity());
        medication.setOrigin(MedicationOrigin.PRESCRIPTION);
        medication.setPrescriptionId(prescriptionId);
        medication.setActive(true);
        medication.setCreatedBy(author.getId());
        medication.setCreatedAt(clock.instant());
        return responseFactory.create(medicationRepository.save(medication), warnings);
    }

    private void rejectDuplicate(UUID patientId, PrescriptionItem item) {
        boolean duplicated = medicationRepository.findByPatientIdAndActiveTrueOrderByNameAsc(patientId).stream()
                .anyMatch(existing -> PlainText.key(existing.getName()).equals(PlainText.key(item.getMedicationName()))
                        && PlainText.key(existing.getStrength()).equals(PlainText.key(item.getPresentation())));
        if (duplicated) {
            throw new ConflictException("Este medicamento ja esta na rotina do paciente.");
        }
    }

    private List<MedicationWarning> checkAllergies(UUID patientId, PrescriptionItem item, AddToRoutineRequest request) {
        List<Allergy> allergies = allergyRepository.findByPatientIdOrderByCreatedAtAsc(patientId);
        Optional<Allergy> conflict = allergyChecker.findConflict(item.getMedicationName(), allergies);
        if (conflict.isEmpty()) {
            return List.of();
        }
        if (!request.allergyRiskConfirmed()) {
            throw new ValueNeedsConfirmationException(
                    "Possivel alergia a \"%s\" registrada para este paciente. Confirme para salvar."
                            .formatted(conflict.get().getDescription()));
        }
        return List.of(MedicationWarning.ALLERGY_MATCH_CONFIRMED);
    }
}
