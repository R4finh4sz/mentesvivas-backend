package com.mentesvivas.backend.service.patient.hydration;

import com.mentesvivas.backend.config.HydrationProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HydrationConditionClassifierTest {

    private final HydrationConditionClassifier classifier = new HydrationConditionClassifier(
            new HydrationProperties(new BigDecimal("25"), new BigDecimal("30"), ZoneId.of("UTC"),
                    List.of("renal", "rim"), List.of("cardi", "coracao"), List.of("hipertens", "diabet")));

    @Test
    void renalAndCardiacConditionsRequireTheDoctorGoal() {
        assertThat(classifier.classify("Doenca renal cronica")).isEqualTo(HydrationCondition.MEDICAL_GOAL_REQUIRED);
        assertThat(classifier.classify("Insuficiência CARDÍACA")).isEqualTo(HydrationCondition.MEDICAL_GOAL_REQUIRED);
        assertThat(classifier.classify("problema no coração")).isEqualTo(HydrationCondition.MEDICAL_GOAL_REQUIRED);
    }

    @Test
    void hypertensionAndDiabetesOnlyAskToConsultTheDoctor() {
        assertThat(classifier.classify("Hipertensão")).isEqualTo(HydrationCondition.CONSULT_DOCTOR);
        assertThat(classifier.classify("diabetes tipo 2")).isEqualTo(HydrationCondition.CONSULT_DOCTOR);
    }

    @Test
    void restrictedConditionWinsOverConsult() {
        assertThat(classifier.classify("Diabetes e doença renal")).isEqualTo(HydrationCondition.MEDICAL_GOAL_REQUIRED);
    }

    @Test
    void otherOrMissingConditionsHaveNoRestriction() {
        assertThat(classifier.classify("Alzheimer")).isEqualTo(HydrationCondition.NONE);
        assertThat(classifier.classify("  ")).isEqualTo(HydrationCondition.NONE);
        assertThat(classifier.classify(null)).isEqualTo(HydrationCondition.NONE);
    }
}
