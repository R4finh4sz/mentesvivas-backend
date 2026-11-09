package com.mentesvivas.backend.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StrongPasswordValidatorTest {

    private final StrongPasswordValidator validator = new StrongPasswordValidator();

    @Test
    void acceptsUppercaseLowercaseAndDigit() {
        assertThat(validator.isValid("Senha1ok", null)).isTrue();
        assertThat(validator.isValid("1aB", null)).isTrue();
    }

    @Test
    void rejectsPasswordWithoutUppercase() {
        assertThat(validator.isValid("senhaforte1", null)).isFalse();
    }

    @Test
    void rejectsPasswordWithoutLowercase() {
        assertThat(validator.isValid("SENHAFORTE1", null)).isFalse();
    }

    @Test
    void rejectsPasswordWithoutDigit() {
        assertThat(validator.isValid("SenhaForte", null)).isFalse();
    }

    @Test
    void acceptsSpecialCharactersAlongsideTheRequiredOnes() {
        assertThat(validator.isValid("Senha#Forte1!", null)).isTrue();
    }

    @Test
    void emptyAndNullAreLeftToOtherConstraints() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("", null)).isTrue();
    }
}
