package com.mentesvivas.backend.entity.patient;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PainScaleTest {

    @Test
    void supportsTheNumericDefaultAndBothObservationalScoreRanges() {
        assertThat(PainScale.DEFAULT).isEqualTo(PainScale.NUMERIC);
        assertThat(PainScale.PAINAD.scores()).hasSize(11).startsWith(0).endsWith(10);
        assertThat(PainScale.DOLOPLUS_2.scores()).hasSize(31).startsWith(0).endsWith(30);
    }
}
