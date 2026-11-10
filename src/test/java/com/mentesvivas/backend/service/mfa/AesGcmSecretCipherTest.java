package com.mentesvivas.backend.service.mfa;

import com.mentesvivas.backend.exception.config.InvalidConfigurationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmSecretCipherTest {

    private static final String KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Test
    void decryptsWhatItEncryptsAndHidesThePlainText() {
        AesGcmSecretCipher cipher = new AesGcmSecretCipher(KEY);

        String encrypted = cipher.encrypt("JBSWY3DPEHPK3PXP");

        assertThat(encrypted).doesNotContain("JBSWY3DPEHPK3PXP");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("JBSWY3DPEHPK3PXP");
        assertThat(cipher.encrypt("JBSWY3DPEHPK3PXP")).isNotEqualTo(encrypted);
    }

    @Test
    void refusesToWorkWithoutAValidKey() {
        assertThatThrownBy(() -> new AesGcmSecretCipher("").encrypt("x")).isInstanceOf(InvalidConfigurationException.class);
        assertThatThrownBy(() -> new AesGcmSecretCipher("curta").encrypt("x")).isInstanceOf(InvalidConfigurationException.class);
        assertThatThrownBy(() -> new AesGcmSecretCipher("YWJj").encrypt("x")).isInstanceOf(InvalidConfigurationException.class);
    }

    @Test
    void detectsTamperedData() {
        AesGcmSecretCipher cipher = new AesGcmSecretCipher(KEY);
        String encrypted = cipher.encrypt("segredo");
        String tampered = encrypted.substring(0, encrypted.length() - 4) + "AAAA";

        assertThatThrownBy(() -> cipher.decrypt(tampered)).isInstanceOf(InvalidConfigurationException.class);
    }
}
