package com.mentesvivas.backend.controller.auth;

import com.mentesvivas.backend.support.ClearTemporaryStorage;
import com.mentesvivas.backend.support.TermsTestSupport;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(ClearTemporaryStorage.class)
@AutoConfigureMockMvc
@Transactional
class RegistrationNormalizationTest {

    private static final String PASSWORD = "senhaForte1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TermsTestSupport terms;

    private int termsVersion;

    @BeforeEach
    void publishTerms() {
        termsVersion = terms.publish();
    }

    @Test
    void emailSavedLowercaseAndDuplicateInOtherCaseReturns409() throws Exception {
        registerApp("  Rafael.Souza@Gmail.com ", "52998224725")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("rafael.souza@gmail.com"));

        registerApp("rafael.souza@gmail.com", "11144477735").andExpect(status().isConflict());
        registerApp("RAFAEL.SOUZA@GMAIL.COM", "11144477735").andExpect(status().isConflict());
    }

    @Test
    void loginAcceptsEmailInAnyCaseAndWithSpaces() throws Exception {
        registerApp("rafael.souza@gmail.com", "52998224725").andExpect(status().isCreated());

        login("Rafael.Souza@Gmail.com").andExpect(status().isOk());
        login("  RAFAEL.SOUZA@GMAIL.COM  ").andExpect(status().isOk());
    }

    @Test
    void registrationAcceptsMasksAndNameWithExtraSpaces() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"fullName":"  Maria   da  Silva ","cpf":"529.982.247-25","birthDate":"1950-03-10",
                         "phone":"(11) 99999-8888","email":"maria@exemplo.com","password":"%s","passwordConfirmation":"%s",
                         "role":"PATIENT","termsVersion":%d,"acceptTerms":true,"acceptHealthData":true}
                        """.formatted(PASSWORD, PASSWORD, termsVersion)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Maria da Silva"));
    }

    @Test
    void doctorAcceptsLowercaseStateAndCrmWithPunctuation() throws Exception {
        mockMvc.perform(post("/api/auth/register/doctor").contentType(MediaType.APPLICATION_JSON).content("""
                        {"fullName":"Dr. Joao","cpf":"12345678909","phone":"11988887777",
                         "email":"Joao@Clinica.com","password":"%s","passwordConfirmation":"%s",
                         "crm":"123.456","crmState":"sp","termsVersion":%d,"acceptTerms":true}
                        """.formatted(PASSWORD, PASSWORD, termsVersion)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("joao@clinica.com"));
    }

    @Test
    void refreshAcceptsTokenWithSurroundingSpaces() throws Exception {
        registerApp("bia@exemplo.com", "52998224725").andExpect(status().isCreated());
        String body = login("bia@exemplo.com").andReturn().getResponse().getContentAsString();
        String refresh = com.jayway.jsonpath.JsonPath.read(body, "$.refreshToken");

        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"  %s\\n \"}".formatted(refresh)))
                .andExpect(status().isOk());
    }

    private ResultActions registerApp(String email, String cpf) throws Exception {
        return mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"fullName":"Pessoa Teste","cpf":"%s","birthDate":"1950-03-10",
                 "phone":"11999998888","email":"%s","password":"%s","passwordConfirmation":"%s",
                 "role":"CAREGIVER","termsVersion":%d,"acceptTerms":true,"acceptHealthData":true}
                """.formatted(cpf, email, PASSWORD, PASSWORD, termsVersion)));
    }

    private ResultActions login(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)));
    }
}
