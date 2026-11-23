package com.mentesvivas.backend.service.medication;

import com.mentesvivas.backend.config.MedicationProperties;
import com.mentesvivas.backend.entity.medication.Medication;
import com.mentesvivas.backend.entity.patient.Allergy;
import com.mentesvivas.backend.entity.patient.AllergyCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MedicationRulesTest {

    private final MedicationStockCalculator stock = new MedicationStockCalculator(new MedicationProperties(5));
    private final MedicationAllergyChecker checker = new MedicationAllergyChecker();

    private Medication medication(String unitsPerDose, int timesPerDay, String stockQuantity) {
        Medication medication = new Medication();
        medication.setUnitsPerDose(new BigDecimal(unitsPerDose));
        medication.setStockQuantity(new BigDecimal(stockQuantity));
        medication.setTimes(java.util.stream.IntStream.range(0, timesPerDay).mapToObj(i -> LocalTime.of(6 + i, 0)).toList());
        return medication;
    }

    private Allergy allergy(AllergyCategory category, String description) {
        Allergy allergy = new Allergy();
        allergy.setCategory(category);
        allergy.setDescription(description);
        return allergy;
    }

    @Test
    void daysLeftUsesTheDailyDose() {
        assertThat(stock.daysLeft(medication("1", 2, "30"))).isEqualTo(15);
        assertThat(stock.daysLeft(medication("0.5", 2, "7"))).isEqualTo(7);
        assertThat(stock.daysLeft(medication("2", 3, "11"))).isEqualTo(1);
    }

    @Test
    void emptyStockHasNoDaysLeft() {
        assertThat(stock.daysLeft(medication("1", 1, "0"))).isZero();
    }

    @Test
    void medicationWithoutTimesHasNoDaysLeft() {
        assertThat(stock.daysLeft(medication("1", 0, "30"))).isZero();
    }

    @Test
    void stockIsCriticalAtOrBelowTheConfiguredDays() {
        assertThat(stock.isCritical(medication("1", 1, "5"))).isTrue();
        assertThat(stock.isCritical(medication("1", 1, "4"))).isTrue();
        assertThat(stock.isCritical(medication("1", 1, "6"))).isFalse();
    }

    @Test
    void conflictIgnoresCaseAndAccents() {
        List<Allergy> allergies = List.of(allergy(AllergyCategory.MEDICATION, "Dipirona"));

        assertThat(checker.findConflict("DIPIRONA sódica", allergies)).isPresent();
        assertThat(checker.findConflict("dipirona", allergies)).isPresent();
    }

    @Test
    void conflictWorksWhenTheAllergyDescribesMoreThanTheName() {
        List<Allergy> allergies = List.of(allergy(AllergyCategory.MEDICATION, "Penicilina e derivados"));

        assertThat(checker.findConflict("Penicilina", allergies)).isPresent();
    }

    @Test
    void onlyMedicationAllergiesAreCrossed() {
        List<Allergy> allergies = List.of(allergy(AllergyCategory.FOOD, "Dipirona"));

        assertThat(checker.findConflict("Dipirona", allergies)).isEmpty();
    }

    @Test
    void unrelatedOrTooShortTermsDoNotMatch() {
        List<Allergy> allergies = List.of(allergy(AllergyCategory.MEDICATION, "Dipirona"),
                allergy(AllergyCategory.MEDICATION, "Ab"));

        assertThat(checker.findConflict("Paracetamol", allergies)).isEmpty();
        assertThat(checker.findConflict("Abc", allergies)).isEmpty();
        assertThat(checker.findConflict("A", allergies)).isEmpty();
    }
}
