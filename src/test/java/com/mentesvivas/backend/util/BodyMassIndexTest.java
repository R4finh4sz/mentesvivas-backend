package com.mentesvivas.backend.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BodyMassIndexTest {

    @Test
    void dividesWeightByHeightSquaredRoundedToOneDecimal() {
        assertThat(BodyMassIndex.calculate(new BigDecimal("72.5"), new BigDecimal("1.68")))
                .isEqualByComparingTo("25.7");
    }

    @Test
    void withoutWeightOrHeightThereIsNoIndex() {
        assertThat(BodyMassIndex.calculate(null, new BigDecimal("1.70"))).isNull();
        assertThat(BodyMassIndex.calculate(new BigDecimal("70"), null)).isNull();
        assertThat(BodyMassIndex.calculate(new BigDecimal("70"), BigDecimal.ZERO)).isNull();
    }
}
