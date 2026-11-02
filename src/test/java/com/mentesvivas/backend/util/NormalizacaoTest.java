package com.mentesvivas.backend.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NormalizacaoTest {

    @Test
    void emailFicaEmMinusculoSemEspacos() {
        assertThat(Normalizacao.email("  Rafael.Souza@Gmail.COM \n")).isEqualTo("rafael.souza@gmail.com");
    }

    @Test
    void emailIgualaCaracteresDeLarguraTotalEEspacoInseparavel() {
        assertThat(Normalizacao.email(" ｒａｆａｅｌ@gmail.com")).isEqualTo("rafael@gmail.com");
    }

    @Test
    void nomeRemoveEspacosExtrasEIgualaAcentoDecomposto() {
        assertThat(Normalizacao.nome("  Maria   da\tSilva ")).isEqualTo("Maria da Silva");
        assertThat(Normalizacao.nome("José")).isEqualTo("José");
    }

    @Test
    void cpfETelefoneAceitamMascaraMasNaoEngolemLetras() {
        assertThat(Normalizacao.cpf("529.982.247-25")).isEqualTo("52998224725");
        assertThat(Normalizacao.cpf("529982247abc")).isEqualTo("529982247abc");
        assertThat(Normalizacao.telefone("(11) 99999-8888")).isEqualTo("11999998888");
    }

    @Test
    void crmEUfSaoPadronizados() {
        assertThat(Normalizacao.crm(" 123.456 ")).isEqualTo("123456");
        assertThat(Normalizacao.uf(" sp ")).isEqualTo("SP");
        assertThat(Normalizacao.token("  abc \n")).isEqualTo("abc");
    }

    @Test
    void nuloContinuaNulo() {
        assertThat(Normalizacao.email(null)).isNull();
        assertThat(Normalizacao.nome(null)).isNull();
        assertThat(Normalizacao.cpf(null)).isNull();
        assertThat(Normalizacao.telefone(null)).isNull();
        assertThat(Normalizacao.crm(null)).isNull();
        assertThat(Normalizacao.uf(null)).isNull();
        assertThat(Normalizacao.token(null)).isNull();
    }
}
