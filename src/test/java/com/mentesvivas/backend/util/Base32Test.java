package com.mentesvivas.backend.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Base32Test {

    @Test
    void encodesTheRfc4648Vectors() {
        assertThat(Base32.encode("foobar".getBytes(StandardCharsets.UTF_8))).isEqualTo("MZXW6YTBOI");
        assertThat(Base32.encode("fo".getBytes(StandardCharsets.UTF_8))).isEqualTo("MZXQ");
    }

    @Test
    void decodesWhatItEncodes() {
        byte[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, (byte) 250};

        assertThat(Base32.decode(Base32.encode(data))).isEqualTo(data);
        assertThat(new String(Base32.decode("mzxw6ytboi"), StandardCharsets.UTF_8)).isEqualTo("foobar");
    }

    @Test
    void rejectsCharactersOutsideTheAlphabet() {
        assertThatThrownBy(() -> Base32.decode("AB!1")).isInstanceOf(IllegalArgumentException.class);
    }
}
