package com.mentesvivas.backend.controller.auth;

import com.jayway.jsonpath.JsonPath;
import com.mentesvivas.backend.support.ClearTemporaryStorage;
import com.mentesvivas.backend.support.TermsTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(ClearTemporaryStorage.class)
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    private static final String PASSWORD = "SenhaForte1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private TermsTestSupport terms;

    private int termsVersion;

    @BeforeEach
    void publishTerms() {
        termsVersion = terms.publish();
    }

    @Test
    void registersPatientAndLoginReturnsRoleInBodyAndJwt() throws Exception {
        registerApp("Maria@Exemplo.com", "529.982.247-25", "PATIENT")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("maria@exemplo.com"))
                .andExpect(jsonPath("$.role").value("PATIENT"));

        String body = login("MARIA@exemplo.com", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PATIENT"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800))
                .andExpect(jsonPath("$.refreshExpiresIn").value(3600))
                .andExpect(jsonPath("$.termsAccepted").value(true))
                .andReturn().getResponse().getContentAsString();

        Jwt jwt = jwtDecoder.decode(JsonPath.read(body, "$.accessToken"));
        assertThat(jwt.getClaimAsString("role")).isEqualTo("PATIENT");
    }

    @Test
    void registersDoctorWithCrm() throws Exception {
        mockMvc.perform(post("/api/auth/register/doctor").contentType(MediaType.APPLICATION_JSON).content("""
                        {"fullName":"Dr. Joao","cpf":"12345678909","phone":"11988887777",
                         "email":"joao@clinica.com","password":"%s","passwordConfirmation":"%s",
                         "crm":"123456","crmState":"SP","termsVersion":%d,"acceptTerms":true}
                        """.formatted(PASSWORD, PASSWORD, termsVersion)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("DOCTOR"));

        login("joao@clinica.com", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("DOCTOR"));
    }

    @Test
    void appDoesNotAllowAdministratorSelfRegistration() throws Exception {
        registerApp("admin@exemplo.com", "52998224725", "ADMINISTRATOR")
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateEmailReturnsConflict() throws Exception {
        registerApp("ana@exemplo.com", "52998224725", "CAREGIVER").andExpect(status().isCreated());
        registerApp("ANA@exemplo.com", "11144477735", "CAREGIVER").andExpect(status().isConflict());
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        registerApp("carla@exemplo.com", "52998224725", "CAREGIVER").andExpect(status().isCreated());
        login("carla@exemplo.com", "outraSenha1").andExpect(status().isUnauthorized());
        login("naoexiste@exemplo.com", PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRotatesTokenAndReuseEndsSession() throws Exception {
        registerApp("bia@exemplo.com", "52998224725", "CAREGIVER").andExpect(status().isCreated());
        String body = login("bia@exemplo.com", PASSWORD).andReturn().getResponse().getContentAsString();
        String refresh1 = JsonPath.read(body, "$.refreshToken");

        String renewed = refresh(refresh1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CAREGIVER"))
                .andReturn().getResponse().getContentAsString();
        String refresh2 = JsonPath.read(renewed, "$.refreshToken");
        assertThat(refresh2).isNotEqualTo(refresh1);

        refresh(refresh1).andExpect(status().isUnauthorized());
        refresh(refresh2).andExpect(status().isUnauthorized());
    }

    @Test
    void fiveConsecutiveFailuresLockLoginEvenWithCorrectPassword() throws Exception {
        registerApp("dani@exemplo.com", "52998224725", "CAREGIVER").andExpect(status().isCreated());
        for (int i = 0; i < 5; i++) {
            login("dani@exemplo.com", "errada123").andExpect(status().isUnauthorized());
        }
        login("dani@exemplo.com", PASSWORD)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void successfulLoginResetsFailureCount() throws Exception {
        registerApp("edu@exemplo.com", "52998224725", "CAREGIVER").andExpect(status().isCreated());
        for (int i = 0; i < 4; i++) {
            login("edu@exemplo.com", "errada123").andExpect(status().isUnauthorized());
        }
        login("edu@exemplo.com", PASSWORD).andExpect(status().isOk());
        for (int i = 0; i < 4; i++) {
            login("edu@exemplo.com", "errada123").andExpect(status().isUnauthorized());
        }
        login("edu@exemplo.com", PASSWORD).andExpect(status().isOk());
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        registerApp("fabi@exemplo.com", "52998224725", "CAREGIVER").andExpect(status().isCreated());
        String body = login("fabi@exemplo.com", PASSWORD).andReturn().getResponse().getContentAsString();
        String refresh = JsonPath.read(body, "$.refreshToken");

        mockMvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refresh)))
                .andExpect(status().isNoContent());

        refresh(refresh).andExpect(status().isUnauthorized());
    }

    @Test
    void registrationRejectsPasswordsWithoutUppercaseLowercaseOrDigit() throws Exception {
        for (String weak : new String[]{"senhaforte1", "SENHAFORTE1", "SenhaForte"}) {
            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                    {"fullName":"Pessoa Teste","cpf":"52998224725","birthDate":"1950-03-10",
                     "phone":"11999998888","email":"fraca@exemplo.com","password":"%s","passwordConfirmation":"%s",
                     "role":"CAREGIVER","termsVersion":%d,"acceptTerms":true,"acceptHealthData":true}
                    """.formatted(weak, weak, termsVersion)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.password").value("a senha deve ter ao menos uma letra maiuscula, uma minuscula e um numero"));
        }
    }

    private ResultActions registerApp(String email, String cpf, String role) throws Exception {
        return mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"fullName":"Pessoa Teste","cpf":"%s","birthDate":"1950-03-10",
                 "phone":"11999998888","email":"%s","password":"%s","passwordConfirmation":"%s",
                 "role":"%s","termsVersion":%d,"acceptTerms":true,"acceptHealthData":true}
                """.formatted(cpf, email, PASSWORD, PASSWORD, role, termsVersion)));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)));
    }
}
