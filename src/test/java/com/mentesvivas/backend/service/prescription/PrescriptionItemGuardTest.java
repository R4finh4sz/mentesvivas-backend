package com.mentesvivas.backend.service.prescription;

import com.mentesvivas.backend.config.PrescriptionProperties;
import com.mentesvivas.backend.entity.patient.Allergy;
import com.mentesvivas.backend.entity.patient.AllergyCategory;
import com.mentesvivas.backend.entity.patient.AllergySeverity;
import com.mentesvivas.backend.entity.prescription.DrugCatalogEntry;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.exception.patient.ImpossibleValueException;
import com.mentesvivas.backend.exception.patient.ValueNeedsConfirmationException;
import com.mentesvivas.backend.service.medication.MedicationAllergyChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionItemGuardTest {

    @Mock
    private DrugLookup drugLookup;

    private PrescriptionItemGuard blocking;
    private PrescriptionItemGuard withOverride;

    @BeforeEach
    void setUp() {
        blocking = new PrescriptionItemGuard(drugLookup, new MedicationAllergyChecker(),
                new PrescriptionProperties(30, 5, false, "https://x/"));
        withOverride = new PrescriptionItemGuard(drugLookup, new MedicationAllergyChecker(),
                new PrescriptionProperties(30, 5, true, "https://x/"));
        lenient().when(drugLookup.find(anyString())).thenReturn(Optional.empty());
    }

    private Allergy allergy(String description) {
        Allergy allergy = new Allergy();
        allergy.setCategory(AllergyCategory.MEDICATION);
        allergy.setDescription(description);
        allergy.setSeverity(AllergySeverity.SEVERE);
        return allergy;
    }

    private DrugCatalogEntry entry(String name, String ingredient, String therapeuticClass, boolean controlled) {
        DrugCatalogEntry entry = new DrugCatalogEntry();
        entry.setName(name);
        entry.setActiveIngredient(ingredient);
        entry.setTherapeuticClass(therapeuticClass);
        entry.setControlled(controlled);
        return entry;
    }

    @Test
    void itemWithoutNameIsIgnored() {
        assertThatCode(() -> blocking.check(null, null, false, List.of(allergy("Dipirona")))).doesNotThrowAnyException();
    }

    @Test
    void harmlessItemPasses() {
        assertThatCode(() -> blocking.check("Losartana", null, false, List.of(allergy("Dipirona"))))
                .doesNotThrowAnyException();
    }

    @Test
    void allergyByNameBlocksWhenOverrideIsNotAllowed() {
        assertThatThrownBy(() -> blocking.check("Dipirona", "paciente tolera", true, List.of(allergy("Dipirona"))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Alerta impeditivo");
    }

    @Test
    void allergyByActiveIngredientIsFoundThroughTheCatalog() {
        when(drugLookup.find("Novalgina"))
                .thenReturn(Optional.of(entry("Novalgina", "Dipirona", null, false)));

        assertThatThrownBy(() -> blocking.check("Novalgina", null, false, List.of(allergy("dipirona"))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void allergyByTherapeuticClassIsFoundThroughTheCatalog() {
        when(drugLookup.find("Amoxicilina"))
                .thenReturn(Optional.of(entry("Amoxicilina", "Amoxicilina tri-hidratada", "Antibiotico", false)));

        assertThatThrownBy(() -> blocking.check("Amoxicilina", null, false, List.of(allergy("Antibioticos"))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void overrideNeedsJustificationAndConfirmation() {
        List<Allergy> allergies = List.of(allergy("Dipirona"));

        assertThatThrownBy(() -> withOverride.check("Dipirona", null, true, allergies))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("justificativa");
        assertThatThrownBy(() -> withOverride.check("Dipirona", "beneficio maior que o risco", false, allergies))
                .isInstanceOf(ValueNeedsConfirmationException.class);
        assertThatCode(() -> withOverride.check("Dipirona", "beneficio maior que o risco", true, allergies))
                .doesNotThrowAnyException();
    }

    @Test
    void controlledDrugIsRefusedWithTheOutOfAppGuidance() {
        when(drugLookup.find("Clonazepam")).thenReturn(Optional.of(entry("Clonazepam", "Clonazepam", null, true)));

        assertThatThrownBy(() -> blocking.check("Clonazepam", null, false, List.of()))
                .isInstanceOf(ImpossibleValueException.class)
                .hasMessageContaining("controle especial")
                .hasMessageContaining("fora do aplicativo");
    }
}
