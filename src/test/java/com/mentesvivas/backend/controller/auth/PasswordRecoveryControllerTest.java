package com.mentesvivas.backend.controller.auth;

import com.jayway.jsonpath.JsonPath;
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
class PasswordRecoveryControllerTest {

    private static final String PASSWORD = "senhaForte1";
    private static final String NEW_PASSWORD = "novaSenha99";
    private static final String EMAIL = "rita@exemplo.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TermsTestSupport terms;

    @Autowired
    private CapturedEmailSender emails;

    private int termsVersion;

    @BeforeEach
    void setUp() throws Exception {
        emails.clear();
        termsVersion = terms.publish();
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"fullName":"Rita Teste","cpf":"52998224725","birthDate":"1950-03-10",
                 "phone":"11999998888","email":"%s","password":"%s","passwordConfirmation":"%s",
                 "role":"CAREGIVER","termsVersion":%d,"acceptTerms":true,"acceptHealthData":true}
                """.formatted(EMAIL, PASSWORD, PASSWORD, termsVersion))).andExpect(status().isCreated());
        emails.clear();
    }

    @Test
    void responseIsTheSameForRegisteredAndUnknownEmail() throws Exception {
        String existing = requestRecovery(EMAIL).andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        String nonexistent = requestRecovery("ninguem@exemplo.com").andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();

        assertThat(existing).isEqualTo(nonexistent);
        assertThat(emails.sentEmails()).hasSize(1);
        assertThat(emails.sentEmails().get(0).recipient()).isEqualTo(EMAIL);
    }

    @Test
    void resetsPasswordEndsSessionsAndNotifiesByEmail() throws Exception {
        String login = login(EMAIL, PASSWORD).andReturn().getResponse().getContentAsString();
        String refresh = JsonPath.read(login, "$.refreshToken");

        requestRecovery(EMAIL).andExpect(status().isAccepted()).andExpect(jsonPath("$.codeExpiresInSeconds").value(1800));
        String code = emails.lastCode();

        reset(EMAIL, code, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());

        assertThat(emails.sentEmails()).hasSize(2);
        assertThat(emails.sentEmails().get(1).body()).doesNotContain(code);
        login(EMAIL, PASSWORD).andExpect(status().isUnauthorized());
        login(EMAIL, NEW_PASSWORD).andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(refresh))).andExpect(status().isUnauthorized());
    }

    @Test
    void codeCanOnlyBeUsedOnce() throws Exception {
        requestRecovery(EMAIL);
        String code = emails.lastCode();

        reset(EMAIL, code, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());
        reset(EMAIL, code, "outraSenha88", "outraSenha88").andExpect(status().isBadRequest());
    }

    @Test
    void wrongCodeOrNoRequestReturns400() throws Exception {
        reset(EMAIL, "123456", NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isBadRequest());

        requestRecovery(EMAIL);
        String code = emails.lastCode();
        String wrong = code.equals("000000") ? "000001" : "000000";
        reset(EMAIL, wrong, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isBadRequest());
    }

    @Test
    void correctCodeIsRejectedAfterAttemptLimit() throws Exception {
        requestRecovery(EMAIL);
        String code = emails.lastCode();
        String wrong = code.equals("000000") ? "000001" : "000000";
        for (int i = 0; i < 5; i++) {
            reset(EMAIL, wrong, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isBadRequest());
        }
        reset(EMAIL, code, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isBadRequest());
    }

    @Test
    void resendIsLimitedAndPasswordMustBeConfirmed() throws Exception {
        for (int i = 0; i < 5; i++) {
            requestRecovery(EMAIL).andExpect(status().isAccepted());
        }
        assertThat(emails.sentEmails()).hasSize(3);

        String code = emails.lastCode();
        reset(EMAIL, code, NEW_PASSWORD, "diferente123").andExpect(status().isBadRequest());
        reset(EMAIL, code, "curta", "curta").andExpect(status().isBadRequest());
    }

    @Test
    void resetReleasesLockedAccount() throws Exception {
        for (int i = 0; i < 5; i++) {
            login(EMAIL, "errada123").andExpect(status().isUnauthorized());
        }
        login(EMAIL, PASSWORD).andExpect(status().isTooManyRequests());

        requestRecovery(EMAIL);
        reset(EMAIL, emails.lastCode(), NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());

        login(EMAIL, NEW_PASSWORD).andExpect(status().isOk());
    }

    private ResultActions requestRecovery(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/password/forgot").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(email)));
    }

    private ResultActions reset(String email, String code, String password, String confirmation) throws Exception {
        return mockMvc.perform(post("/api/auth/password/reset").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"code\":\"%s\",\"newPassword\":\"%s\",\"passwordConfirmation\":\"%s\"}"
                        .formatted(email, code, password, confirmation)));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)));
    }
}
