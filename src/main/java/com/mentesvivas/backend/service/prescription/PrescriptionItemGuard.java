package com.mentesvivas.backend.service.prescription;

import com.mentesvivas.backend.config.PrescriptionProperties;
import com.mentesvivas.backend.entity.patient.Allergy;
import com.mentesvivas.backend.entity.prescription.DrugCatalogEntry;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.exception.patient.ImpossibleValueException;
import com.mentesvivas.backend.exception.patient.ValueNeedsConfirmationException;
import com.mentesvivas.backend.service.medication.MedicationAllergyChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PrescriptionItemGuard {

    private final DrugLookup drugLookup;
    private final MedicationAllergyChecker allergyChecker;
    private final PrescriptionProperties properties;

    public void check(String medicationName, String overrideJustification, boolean riskConfirmed,
                      List<Allergy> allergies) {
        if (medicationName == null) {
            return;
        }
        Optional<DrugCatalogEntry> entry = drugLookup.find(medicationName);
        entry.ifPresent(this::rejectControlled);
        Optional<Allergy> conflict = findConflict(medicationName, entry, allergies);
        if (conflict.isEmpty()) {
            return;
        }
        String message = "Alerta impeditivo: o paciente tem alergia registrada a \"%s\"."
                .formatted(conflict.get().getDescription());
        if (!properties.allergyOverrideAllowed()) {
            throw new ConflictException(message + " A prescricao deste medicamento esta bloqueada.");
        }
        if (overrideJustification == null) {
            throw new ConflictException(message + " Informe a justificativa para prosseguir.");
        }
        if (!riskConfirmed) {
            throw new ValueNeedsConfirmationException(message + " Confirme novamente para prosseguir.");
        }
    }

    private void rejectControlled(DrugCatalogEntry entry) {
        if (entry.isControlled()) {
            throw new ImpossibleValueException("\"%s\" e um medicamento de controle especial. "
                    .formatted(entry.getName())
                    + "Nesta versao o receituario de controle especial e emitido fora do aplicativo.");
        }
    }

    private Optional<Allergy> findConflict(String medicationName, Optional<DrugCatalogEntry> entry,
                                           List<Allergy> allergies) {
        Optional<Allergy> byName = allergyChecker.findConflict(medicationName, allergies);
        if (byName.isPresent() || entry.isEmpty()) {
            return byName;
        }
        Optional<Allergy> byIngredient = allergyChecker.findConflict(entry.get().getActiveIngredient(), allergies);
        if (byIngredient.isPresent() || entry.get().getTherapeuticClass() == null) {
            return byIngredient;
        }
        return allergyChecker.findConflict(entry.get().getTherapeuticClass(), allergies);
    }
}
