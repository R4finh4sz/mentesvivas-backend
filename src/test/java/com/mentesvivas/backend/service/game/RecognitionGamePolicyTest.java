package com.mentesvivas.backend.service.game;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecognitionGamePolicyTest {

    private final RecognitionGamePolicy policy = new RecognitionGamePolicy();

    @Test
    void needsAtLeastFourPeople() {
        assertThat(policy.minimumPeople()).isEqualTo(4);
        assertThat(policy.isUnlocked(3)).isFalse();
        assertThat(policy.isUnlocked(4)).isTrue();
        assertThat(policy.isUnlocked(9)).isTrue();
    }

    @Test
    void guidanceSaysHowManyAreMissing() {
        assertThat(policy.guidance(1)).contains("3");
        assertThat(policy.guidance(3)).contains("1");
    }

    @Test
    void noGuidanceWhenUnlocked() {
        assertThat(policy.guidance(4)).isNull();
    }
}
