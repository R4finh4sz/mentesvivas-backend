package com.mentesvivas.backend.service.patient.hydration;

import com.mentesvivas.backend.config.HydrationProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HydrationGoalCalculatorTest {

    private final HydrationProperties properties = new HydrationProperties(new BigDecimal("25"),
            new BigDecimal("30"), ZoneId.of("UTC"), List.of("renal"), List.of("cardi"), List.of("hipertens"));
    private final HydrationGoalCalculator calculator =
            new HydrationGoalCalculator(properties, new HydrationConditionClassifier(properties));

    @Test
    void formulaUsesTheWeightRange() {
        HydrationGoal goal = calculator.calculate("Alzheimer", null, new BigDecimal("72.5"));

        assertThat(goal.status()).isEqualTo(HydrationGoalStatus.FORMULA);
        assertThat(goal.minMl()).isEqualTo(1813);
        assertThat(goal.maxMl()).isEqualTo(2175);
        assertThat(goal.consultDoctor()).isFalse();
    }

    @Test
    void hypertensionKeepsTheFormulaButAsksToConsultTheDoctor() {
        HydrationGoal goal = calculator.calculate("Hipertensao", null, new BigDecimal("80"));

        assertThat(goal.status()).isEqualTo(HydrationGoalStatus.FORMULA);
        assertThat(goal.consultDoctor()).isTrue();
    }

    @Test
    void renalOrCardiacConditionInhibitsTheFormula() {
        HydrationGoal goal = calculator.calculate("Insuficiencia renal", null, new BigDecimal("80"));

        assertThat(goal.status()).isEqualTo(HydrationGoalStatus.DOCTOR_GOAL_REQUIRED);
        assertThat(goal.minMl()).isNull();
        assertThat(goal.maxMl()).isNull();
    }

    @Test
    void doctorGoalPrevailsEvenForRestrictedConditions() {
        HydrationGoal goal = calculator.calculate("Insuficiencia cardiaca", 1500, new BigDecimal("80"));

        assertThat(goal.status()).isEqualTo(HydrationGoalStatus.DOCTOR_DEFINED);
        assertThat(goal.minMl()).isEqualTo(1500);
        assertThat(goal.maxMl()).isEqualTo(1500);
    }

    @Test
    void missingWeightHasNoGoal() {
        HydrationGoal goal = calculator.calculate(null, null, null);

        assertThat(goal.status()).isEqualTo(HydrationGoalStatus.WEIGHT_MISSING);
        assertThat(goal.minMl()).isNull();
    }
}
