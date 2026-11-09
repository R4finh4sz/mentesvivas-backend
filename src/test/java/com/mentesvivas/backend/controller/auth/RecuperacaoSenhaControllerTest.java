package com.mentesvivas.backend.controller.auth;

import com.jayway.jsonpath.JsonPath;
import com.mentesvivas.backend.support.EmailCapturadoSender;
import com.mentesvivas.backend.support.LimparArmazenamentoTemporario;
import com.mentesvivas.backend.support.TermosTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(LimparArmazenamentoTemporario.class)
@AutoConfigureMockMvc
@Transactional
class RecuperacaoSenhaControllerTest {

    private static final String SENHA = "senhaForte1";
    private static final String NOVA_SENHA = "novaSenha99";
    private static final String EMAIL = "rita@exemplo.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TermosTestSupport termos;

    @Autowired
    private EmailCapturadoSender emails;

    private int versaoTermos;

    @BeforeEach
    void preparar() throws Exception {
        emails.limpar();
        versaoTermos = termos.publicar();
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"nomeCompleto":"Rita Teste","cpf":"52998224725","dataNascimento":"1950-03-10",
                 "telefone":"11999998888","email":"%s","senha":"%s","confirmacaoSenha":"%s",
                 "role":"CUIDADOR","versaoTermos":%d,"aceiteTermos":true,"aceiteDadosSaude":true}
                """.formatted(EMAIL, SENHA, SENHA, versaoTermos))).andExpect(status().isCreated());
    }

    @Test
    void respostaEhIgualParaEmailCadastradoEInexistente() throws Exception {
        String existente = solicitar(EMAIL).andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        String inexistente = solicitar("ninguem@exemplo.com").andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();

        assertThat(existente).isEqualTo(inexistente);
        assertThat(emails.enviados()).hasSize(1);
        assertThat(emails.enviados().get(0).destinatario()).isEqualTo(EMAIL);
    }

    @Test
    void redefineSenhaEncerraSessoesEAvisaPorEmail() throws Exception {
        String login = login(EMAIL, SENHA).andReturn().getResponse().getContentAsString();
        String refresh = JsonPath.read(login, "$.refreshToken");

        solicitar(EMAIL).andExpect(status().isAccepted()).andExpect(jsonPath("$.codigoExpiraEmSegundos").value(1800));
        String codigo = emails.ultimoCodigo();

        redefinir(EMAIL, codigo, NOVA_SENHA, NOVA_SENHA).andExpect(status().isNoContent());

        assertThat(emails.enviados()).hasSize(2);
        assertThat(emails.enviados().get(1).corpo()).doesNotContain(codigo);
        login(EMAIL, SENHA).andExpect(status().isUnauthorized());
        login(EMAIL, NOVA_SENHA).andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(refresh))).andExpect(status().isUnauthorized());
    }

    @Test
    void codigoSoPodeSerUsadoUmaVez() throws Exception {
        solicitar(EMAIL);
        String codigo = emails.ultimoCodigo();

        redefinir(EMAIL, codigo, NOVA_SENHA, NOVA_SENHA).andExpect(status().isNoContent());
        redefinir(EMAIL, codigo, "outraSenha88", "outraSenha88").andExpect(status().isBadRequest());
    }

    @Test
    void codigoErradoOuSemSolicitacaoRetorna400() throws Exception {
        redefinir(EMAIL, "123456", NOVA_SENHA, NOVA_SENHA).andExpect(status().isBadRequest());

        solicitar(EMAIL);
        String codigo = emails.ultimoCodigo();
        String errado = codigo.equals("000000") ? "000001" : "000000";
        redefinir(EMAIL, errado, NOVA_SENHA, NOVA_SENHA).andExpect(status().isBadRequest());
    }

    @Test
    void aposLimiteDeTentativasOCodigoCorretoTambemEhRecusado() throws Exception {
        solicitar(EMAIL);
        String codigo = emails.ultimoCodigo();
        String errado = codigo.equals("000000") ? "000001" : "000000";
        for (int i = 0; i < 5; i++) {
            redefinir(EMAIL, errado, NOVA_SENHA, NOVA_SENHA).andExpect(status().isBadRequest());
        }
        redefinir(EMAIL, codigo, NOVA_SENHA, NOVA_SENHA).andExpect(status().isBadRequest());
    }

    @Test
    void reenvioTemLimiteESenhaPrecisaSerConfirmada() throws Exception {
        for (int i = 0; i < 5; i++) {
            solicitar(EMAIL).andExpect(status().isAccepted());
        }
        assertThat(emails.enviados()).hasSize(3);

        String codigo = emails.ultimoCodigo();
        redefinir(EMAIL, codigo, NOVA_SENHA, "diferente123").andExpect(status().isBadRequest());
        redefinir(EMAIL, codigo, "curta", "curta").andExpect(status().isBadRequest());
    }

    @Test
    void redefinicaoLiberaContaBloqueada() throws Exception {
        for (int i = 0; i < 5; i++) {
            login(EMAIL, "errada123").andExpect(status().isUnauthorized());
        }
        login(EMAIL, SENHA).andExpect(status().isTooManyRequests());

        solicitar(EMAIL);
        redefinir(EMAIL, emails.ultimoCodigo(), NOVA_SENHA, NOVA_SENHA).andExpect(status().isNoContent());

        login(EMAIL, NOVA_SENHA).andExpect(status().isOk());
    }

    private ResultActions solicitar(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/password/forgot").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(email)));
    }

    private ResultActions redefinir(String email, String codigo, String senha, String confirmacao) throws Exception {
        return mockMvc.perform(post("/api/auth/password/reset").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"codigo\":\"%s\",\"novaSenha\":\"%s\",\"confirmacaoSenha\":\"%s\"}"
                        .formatted(email, codigo, senha, confirmacao)));
    }

    private ResultActions login(String email, String senha) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha)));
    }
}
