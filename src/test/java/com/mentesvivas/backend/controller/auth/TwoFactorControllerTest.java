package com.mentesvivas.backend.controller.auth;

import com.mentesvivas.backend.support.CapturedEmailSender;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(ClearTemporaryStorage.class)
@AutoConfigureMockMvc
@Transactional
class TwoFactorControllerTest {

    private static final String PASSWORD = "SenhaForte1";
    private static final String DOCTOR = "dr.paulo@clinica.com";
    private static final String CAREGIVER = "cuidador@exemplo.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TermsTestSupport terms;

    @Autowired
    private CapturedEmailSender emails;

    @BeforeEach
    void setUp() throws Exception {
        int version = terms.publish();
        mockMvc.perform(post("/api/auth/register/doctor").contentType(MediaType.APPLICATION_JSON).content("""
                {"fullName":"Dr. Paulo","cpf":"12345678909","phone":"11988887777",
                 "email":"%s","password":"%s","passwordConfirmation":"%s",
                 "crm":"123456","crmState":"SP","termsVersion":%d,"acceptTerms":true}
                """.formatted(DOCTOR, PASSWORD, PASSWORD, version))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"fullName":"Cuidador Teste","cpf":"52998224725","birthDate":"1980-03-10",
                 "phone":"11999998888","email":"%s","password":"%s","passwordConfirmation":"%s",
                 "role":"CAREGIVER","termsVersion":%d,"acceptTerms":true,"acceptHealthData":true}
                """.formatted(CAREGIVER, PASSWORD, PASSWORD, version))).andExpect(status().isCreated());
        emails.clear();
    }

    @Test
    void doctorLoginWithPasswordOnlyReturnsChallengeAndSendsCodeByEmail() throws Exception {
        login(DOCTOR, PASSWORD)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.twoFactorRequired").value(true))
                .andExpect(jsonPath("$.codeExpiresInSeconds").value(600))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());

        assertThat(emails.sentEmails()).singleElement().satisfies(sent -> {
            assertThat(sent.recipient()).isEqualTo(DOCTOR);
            assertThat(sent.subject()).isEqualTo("Seu codigo de acesso");
        });
    }

    @Test
    void correctCodeCompletesLoginAndReturnsTokens() throws Exception {
        login(DOCTOR, PASSWORD).andExpect(status().isAccepted());

        verify(DOCTOR, emails.lastCode())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("DOCTOR"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void wrongPasswordNeverSendsACode() throws Exception {
        login(DOCTOR, "SenhaErrada1").andExpect(status().isUnauthorized());

        assertThat(emails.sentEmails()).isEmpty();
    }

    @Test
    void wrongCodeIsRejectedWithoutTokens() throws Exception {
        login(DOCTOR, PASSWORD);
        String wrong = emails.lastCode().equals("000000") ? "000001" : "000000";

        verify(DOCTOR, wrong).andExpect(status().isBadRequest()).andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    void codeCanOnlyBeUsedOnce() throws Exception {
        login(DOCTOR, PASSWORD);
        String code = emails.lastCode();

        verify(DOCTOR, code).andExpect(status().isOk());
        verify(DOCTOR, code).andExpect(status().isBadRequest());
    }

    @Test
    void verifyWithoutPriorPasswordStepIsRejected() throws Exception {
        verify(DOCTOR, "123456").andExpect(status().isBadRequest());
    }

    @Test
    void nonClinicalProfilesLogInWithPasswordOnly() throws Exception {
        login(CAREGIVER, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        assertThat(emails.sentEmails()).isEmpty();
    }

    @Test
    void codeIssuedForADoctorCannotBeUsedToVerifyAnotherEmail() throws Exception {
        login(DOCTOR, PASSWORD);
        String code = emails.lastCode();

        verify(CAREGIVER, code).andExpect(status().isBadRequest());
    }

    @Test
    void anotherLoginIssuesANewCodeAndInvalidatesTheOldOne() throws Exception {
        login(DOCTOR, PASSWORD);
        String first = emails.lastCode();
        login(DOCTOR, PASSWORD);
        String second = emails.lastCode();

        if (!first.equals(second)) {
            verify(DOCTOR, first).andExpect(status().isBadRequest());
        }
        verify(DOCTOR, second).andExpect(status().isOk());
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)));
    }

    private ResultActions verify(String email, String code) throws Exception {
        return mockMvc.perform(post("/api/auth/2fa/verify").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"code\":\"%s\"}".formatted(email, code)));
    }
}
