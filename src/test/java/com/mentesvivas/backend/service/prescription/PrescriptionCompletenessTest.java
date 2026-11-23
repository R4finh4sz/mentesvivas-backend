package com.mentesvivas.backend.service.prescription;

import com.mentesvivas.backend.entity.prescription.DocumentKind;
import com.mentesvivas.backend.entity.prescription.Prescription;
import com.mentesvivas.backend.entity.prescription.PrescriptionItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PrescriptionCompletenessTest {

    private final PrescriptionCompleteness completeness = new PrescriptionCompleteness();

    private Prescription prescription(DocumentKind kind, String orientations) {
        Prescription prescription = new Prescription();
        prescription.setKind(kind);
        prescription.setOrientations(orientations);
        return prescription;
    }

    private PrescriptionItem fullItem() {
        PrescriptionItem item = new PrescriptionItem();
        item.setMedicationName("Losartana");
        item.setPresentation("50 mg comprimido");
        item.setDose("1 comprimido");
        item.setRoute("Oral");
        item.setFrequency("12 em 12 horas");
        item.setDuration("30 dias");
        item.setQuantity("60 comprimidos");
        return item;
    }

    @Test
    void completePrescriptionHasNothingMissing() {
        assertThat(completeness.missingFields(prescription(DocumentKind.PRESCRIPTION, null), List.of(fullItem())))
                .isEmpty();
    }

    @Test
    void prescriptionWithoutItemsReportsTheItems() {
        assertThat(completeness.missingFields(prescription(DocumentKind.PRESCRIPTION, null), List.of()))
                .containsExactly("items");
    }

    @Test
    void reportsEachMissingFieldOfEachItem() {
        PrescriptionItem item = fullItem();
        item.setDose(null);
        item.setDuration(" ");
        PrescriptionItem other = fullItem();
        other.setQuantity(null);

        assertThat(completeness.missingFields(prescription(DocumentKind.PRESCRIPTION, null), List.of(item, other)))
                .containsExactly("items[0].dose", "items[0].duration", "items[1].quantity");
    }

    @Test
    void itemWithoutNameButWithOtherDataReportsTheName() {
        PrescriptionItem item = fullItem();
        item.setMedicationName(null);

        assertThat(completeness.missingFields(prescription(DocumentKind.PRESCRIPTION, null), List.of(item)))
                .containsExactly("items[0].medicationName");
    }

    @Test
    void completelyEmptyItemReportsOnlyTheName() {
        assertThat(completeness.missingFields(prescription(DocumentKind.PRESCRIPTION, null),
                List.of(new PrescriptionItem()))).containsExactly("items[0].medicationName");
    }

    @Test
    void guidesAndChecklistsNeedOrientationsInsteadOfItems() {
        assertThat(completeness.missingFields(prescription(DocumentKind.EXAM_GUIDE, null), List.of()))
                .containsExactly("orientations");
        assertThat(completeness.missingFields(prescription(DocumentKind.CHECKLIST, "Jejum de 8 horas"), List.of()))
                .isEmpty();
    }
}
