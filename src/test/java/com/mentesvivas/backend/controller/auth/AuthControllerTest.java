package com.mentesvivas.backend.controller.auth;

import com.jayway.jsonpath.JsonPath;
import com.mentesvivas.backend.support.TermosTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    private static final String SENHA = "senhaForte1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private TermosTestSupport termos;

    private int versaoTermos;

    @BeforeEach
    void publicarTermos() {
        versaoTermos = termos.publicar();
    }

    @Test
    void registraPacienteELoginRetornaRoleNoCorpoENoJwt() throws Exception {
        registrarApp("Maria@Exemplo.com", "529.982.247-25", "PACIENTE")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("maria@exemplo.com"))
                .andExpect(jsonPath("$.role").value("PACIENTE"));

        String body = login("MARIA@exemplo.com", SENHA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PACIENTE"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800))
                .andExpect(jsonPath("$.refreshExpiresIn").value(3600))
                .andExpect(jsonPath("$.termosAceitos").value(true))
                .andReturn().getResponse().getContentAsString();

        Jwt jwt = jwtDecoder.decode(JsonPath.read(body, "$.accessToken"));
        assertThat(jwt.getClaimAsString("role")).isEqualTo("PACIENTE");
    }

    @Test
    void registraMedicoComCrm() throws Exception {
        mockMvc.perform(post("/api/auth/register/medico").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nomeCompleto":"Dr. Joao","cpf":"12345678909","telefone":"11988887777",
                         "email":"joao@clinica.com","senha":"%s","confirmacaoSenha":"%s",
                         "crm":"123456","crmUf":"SP","versaoTermos":%d,"aceiteTermos":true}
                        """.formatted(SENHA, SENHA, versaoTermos)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MEDICO"));

        login("joao@clinica.com", SENHA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MEDICO"));
    }

    @Test
    void appNaoPermiteAutocadastroDeAdministrador() throws Exception {
        registrarApp("admin@exemplo.com", "52998224725", "ADMINISTRADOR")
                .andExpect(status().isBadRequest());
    }

    @Test
    void emailDuplicadoRetornaConflito() throws Exception {
        registrarApp("ana@exemplo.com", "52998224725", "CUIDADOR").andExpect(status().isCreated());
        registrarApp("ANA@exemplo.com", "11144477735", "CUIDADOR").andExpect(status().isConflict());
    }

    @Test
    void senhaErradaRetorna401() throws Exception {
        registrarApp("carla@exemplo.com", "52998224725", "CUIDADOR").andExpect(status().isCreated());
        login("carla@exemplo.com", "outraSenha1").andExpect(status().isUnauthorized());
        login("naoexiste@exemplo.com", SENHA).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRotacionaTokenEReusoEncerraSessao() throws Exception {
        registrarApp("bia@exemplo.com", "52998224725", "CUIDADOR").andExpect(status().isCreated());
        String body = login("bia@exemplo.com", SENHA).andReturn().getResponse().getContentAsString();
        String refresh1 = JsonPath.read(body, "$.refreshToken");

        String renovado = refresh(refresh1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUIDADOR"))
                .andReturn().getResponse().getContentAsString();
        String refresh2 = JsonPath.read(renovado, "$.refreshToken");
        assertThat(refresh2).isNotEqualTo(refresh1);

        refresh(refresh1).andExpect(status().isUnauthorized());
        refresh(refresh2).andExpect(status().isUnauthorized());
    }

    private ResultActions registrarApp(String email, String cpf, String role) throws Exception {
        return mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"nomeCompleto":"Pessoa Teste","cpf":"%s","dataNascimento":"1950-03-10",
                 "telefone":"11999998888","email":"%s","senha":"%s","confirmacaoSenha":"%s",
                 "role":"%s","versaoTermos":%d,"aceiteTermos":true,"aceiteDadosSaude":true}
                """.formatted(cpf, email, SENHA, SENHA, role, versaoTermos)));
    }

    private ResultActions login(String email, String senha) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha)));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)));
    }
}
