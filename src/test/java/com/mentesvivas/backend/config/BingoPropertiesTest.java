package com.mentesvivas.backend.config;

import com.mentesvivas.backend.exception.config.InvalidConfigurationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BingoPropertiesTest {

    @Test
    void acceptsPositiveValues() {
        BingoProperties properties = new BingoProperties(5, 5, 10);

        assertThat(properties.roundsPerSession()).isEqualTo(5);
        assertThat(properties.soundSeconds()).isEqualTo(5);
        assertThat(properties.minimumSounds()).isEqualTo(10);
    }

    @Test
    void rejectsAZeroValue() {
        assertThatThrownBy(() -> new BingoProperties(0, 5, 10)).isInstanceOf(InvalidConfigurationException.class);
        assertThatThrownBy(() -> new BingoProperties(5, 0, 10)).isInstanceOf(InvalidConfigurationException.class);
        assertThatThrownBy(() -> new BingoProperties(5, 5, 0)).isInstanceOf(InvalidConfigurationException.class);
    }
}
