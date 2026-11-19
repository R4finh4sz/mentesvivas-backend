package com.mentesvivas.backend.service.patient;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MeasurementLimitsTest {

    @Test
    void weightLimitsAreFixed() {
        assertThat(MeasurementLimits.WEIGHT_KG.expectedMin()).isEqualByComparingTo("30");
        assertThat(MeasurementLimits.WEIGHT_KG.expectedMax()).isEqualByComparingTo("300");
        assertThat(MeasurementLimits.WEIGHT_KG.possibleMin()).isEqualByComparingTo("10");
        assertThat(MeasurementLimits.WEIGHT_KG.possibleMax()).isEqualByComparingTo("500");
    }

    @Test
    void heightLimitsAreFixed() {
        assertThat(MeasurementLimits.HEIGHT_M.expectedMin()).isEqualByComparingTo("1.20");
        assertThat(MeasurementLimits.HEIGHT_M.expectedMax()).isEqualByComparingTo("2.30");
        assertThat(MeasurementLimits.HEIGHT_M.possibleMin()).isEqualByComparingTo("0.50");
        assertThat(MeasurementLimits.HEIGHT_M.possibleMax()).isEqualByComparingTo("2.80");
    }

    @Test
    void classifiesValuesAgainstTheTwoLevels() {
        MeasurementLimits weight = MeasurementLimits.WEIGHT_KG;

        assertThat(weight.isImpossible(new BigDecimal("9.9"))).isTrue();
        assertThat(weight.isImpossible(new BigDecimal("500.1"))).isTrue();
        assertThat(weight.isImpossible(new BigDecimal("10"))).isFalse();
        assertThat(weight.isOutsideExpected(new BigDecimal("29.9"))).isTrue();
        assertThat(weight.isOutsideExpected(new BigDecimal("72"))).isFalse();
    }
}
