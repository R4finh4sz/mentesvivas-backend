package com.mentesvivas.backend.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NormalizationTest {

    @Test
    void emailIsLowercaseAndTrimmed() {
        assertThat(Normalization.email("  Rafael.Souza@Gmail.COM \n")).isEqualTo("rafael.souza@gmail.com");
    }

    @Test
    void emailNormalizesFullWidthCharactersAndNonBreakingSpace() {
        assertThat(Normalization.email(" ｒａｆａｅｌ@gmail.com")).isEqualTo("rafael@gmail.com");
    }

    @Test
    void nameTrimsExtraSpacesAndComposesAccents() {
        assertThat(Normalization.name("  Maria   da\tSilva ")).isEqualTo("Maria da Silva");
        assertThat(Normalization.name("José")).isEqualTo("José");
    }

    @Test
    void cpfAndPhoneAcceptMasksButNotLetters() {
        assertThat(Normalization.cpf("529.982.247-25")).isEqualTo("52998224725");
        assertThat(Normalization.cpf("529982247abc")).isEqualTo("529982247abc");
        assertThat(Normalization.phone("(11) 99999-8888")).isEqualTo("11999998888");
    }

    @Test
    void crmAndStateAreNormalized() {
        assertThat(Normalization.crm(" 123.456 ")).isEqualTo("123456");
        assertThat(Normalization.state(" sp ")).isEqualTo("SP");
        assertThat(Normalization.token("  abc \n")).isEqualTo("abc");
    }

    @Test
    void nullStaysNull() {
        assertThat(Normalization.email(null)).isNull();
        assertThat(Normalization.name(null)).isNull();
        assertThat(Normalization.cpf(null)).isNull();
        assertThat(Normalization.phone(null)).isNull();
        assertThat(Normalization.crm(null)).isNull();
        assertThat(Normalization.state(null)).isNull();
        assertThat(Normalization.token(null)).isNull();
    }
}
