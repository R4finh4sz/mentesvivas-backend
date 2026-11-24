package com.mentesvivas.backend.service.game;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BingoLevelPolicyTest {

    private final BingoLevelPolicy policy = new BingoLevelPolicy();

    @Test
    void levelRisesWithFinishedSessionsUpToTheMaximum() {
        assertThat(policy.levelFor(0)).isEqualTo(1);
        assertThat(policy.levelFor(2)).isEqualTo(2);
        assertThat(policy.levelFor(4)).isEqualTo(3);
        assertThat(policy.levelFor(50)).isEqualTo(3);
        assertThat(policy.levelFor(-1)).isEqualTo(1);
    }

    @Test
    void optionsAreTwoThreeOrFourByLevel() {
        assertThat(policy.optionCount(1)).isEqualTo(2);
        assertThat(policy.optionCount(2)).isEqualTo(3);
        assertThat(policy.optionCount(3)).isEqualTo(4);
    }

    @Test
    void optionCountStaysInsideTheRange() {
        assertThat(policy.optionCount(0)).isEqualTo(2);
        assertThat(policy.optionCount(9)).isEqualTo(4);
    }
}
