package com.mentesvivas.backend.controller.auth;

import com.mentesvivas.backend.support.TermosTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CadastroNormalizacaoTest {

    private static final String SENHA = "senhaForte1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TermosTestSupport termos;

    private int versaoTermos;

    @BeforeEach
    void publicarTermos() {
        versaoTermos = termos.publicar();
    }

    @Test
    void emailSalvoEmMinusculoEDuplicadoDeOutraCaixaRetorna409() throws Exception {
        registrarApp("  Rafael.Souza@Gmail.com ", "52998224725")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("rafael.souza@gmail.com"));

        registrarApp("rafael.souza@gmail.com", "11144477735").andExpect(status().isConflict());
        registrarApp("RAFAEL.SOUZA@GMAIL.COM", "11144477735").andExpect(status().isConflict());
    }

    @Test
    void loginAceitaEmailEmQualquerCaixaEComEspacos() throws Exception {
        registrarApp("rafael.souza@gmail.com", "52998224725").andExpect(status().isCreated());

        login("Rafael.Souza@Gmail.com").andExpect(status().isOk());
        login("  RAFAEL.SOUZA@GMAIL.COM  ").andExpect(status().isOk());
    }

    @Test
    void cadastroAceitaMascarasENomeComEspacosExtras() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nomeCompleto":"  Maria   da  Silva ","cpf":"529.982.247-25","dataNascimento":"1950-03-10",
                         "telefone":"(11) 99999-8888","email":"maria@exemplo.com","senha":"%s","confirmacaoSenha":"%s",
                         "role":"PACIENTE","versaoTermos":%d,"aceiteTermos":true,"aceiteDadosSaude":true}
                        """.formatted(SENHA, SENHA, versaoTermos)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomeCompleto").value("Maria da Silva"));
    }

    @Test
    void medicoAceitaUfEmMinusculoECrmComPontuacao() throws Exception {
        mockMvc.perform(post("/api/auth/register/medico").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nomeCompleto":"Dr. Joao","cpf":"12345678909","telefone":"11988887777",
                         "email":"Joao@Clinica.com","senha":"%s","confirmacaoSenha":"%s",
                         "crm":"123.456","crmUf":"sp","versaoTermos":%d,"aceiteTermos":true}
                        """.formatted(SENHA, SENHA, versaoTermos)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("joao@clinica.com"));
    }

    @Test
    void refreshAceitaTokenComEspacosNasPontas() throws Exception {
        registrarApp("bia@exemplo.com", "52998224725").andExpect(status().isCreated());
        String body = login("bia@exemplo.com").andReturn().getResponse().getContentAsString();
        String refresh = com.jayway.jsonpath.JsonPath.read(body, "$.refreshToken");

        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"  %s\\n \"}".formatted(refresh)))
                .andExpect(status().isOk());
    }

    private ResultActions registrarApp(String email, String cpf) throws Exception {
        return mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"nomeCompleto":"Pessoa Teste","cpf":"%s","dataNascimento":"1950-03-10",
                 "telefone":"11999998888","email":"%s","senha":"%s","confirmacaoSenha":"%s",
                 "role":"CUIDADOR","versaoTermos":%d,"aceiteTermos":true,"aceiteDadosSaude":true}
                """.formatted(cpf, email, SENHA, SENHA, versaoTermos)));
    }

    private ResultActions login(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, SENHA)));
    }
}
