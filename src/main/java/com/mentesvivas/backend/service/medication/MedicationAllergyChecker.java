package com.mentesvivas.backend.service.medication;

import com.mentesvivas.backend.entity.patient.Allergy;
import com.mentesvivas.backend.entity.patient.AllergyCategory;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class MedicationAllergyChecker {

    private static final int MIN_TERM_LENGTH = 3;

    public Optional<Allergy> findConflict(String medicationName, List<Allergy> allergies) {
        String medication = plain(medicationName);
        return allergies.stream()
                .filter(allergy -> allergy.getCategory() == AllergyCategory.MEDICATION)
                .filter(allergy -> matches(medication, plain(allergy.getDescription())))
                .findFirst();
    }

    private boolean matches(String medication, String allergy) {
        if (medication.length() < MIN_TERM_LENGTH || allergy.length() < MIN_TERM_LENGTH) {
            return false;
        }
        return medication.contains(allergy) || allergy.contains(medication);
    }

    private String plain(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .strip();
    }
}
