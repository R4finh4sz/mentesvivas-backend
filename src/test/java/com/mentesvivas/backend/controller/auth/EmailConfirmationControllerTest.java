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
class EmailConfirmationControllerTest {

    private static final String PASSWORD = "senhaForte1";
    private static final String EMAIL = "livia@exemplo.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TermsTestSupport terms;

    @Autowired
    private CapturedEmailSender emails;

    @BeforeEach
    void setUp() throws Exception {
        emails.clear();
        int version = terms.publish();
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"fullName":"Livia Teste","cpf":"52998224725","birthDate":"1950-03-10",
                 "phone":"11999998888","email":"%s","password":"%s","passwordConfirmation":"%s",
                 "role":"CAREGIVER","termsVersion":%d,"acceptTerms":true,"acceptHealthData":true}
                """.formatted(EMAIL, PASSWORD, PASSWORD, version)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailConfirmed").value(false));
    }

    @Test
    void registrationSendsConfirmationCodeToTheRegisteredEmail() {
        assertThat(emails.sentEmails()).singleElement().satisfies(sent -> {
            assertThat(sent.recipient()).isEqualTo(EMAIL);
            assertThat(sent.subject()).isEqualTo("Confirme seu e-mail");
        });
    }

    @Test
    void confirmingWithTheCodeMarksTheEmailAsConfirmed() throws Exception {
        confirm(EMAIL, emails.lastCode()).andExpect(status().isNoContent());

        login().andExpect(status().isOk()).andExpect(jsonPath("$.user.emailConfirmed").value(true));
    }

    @Test
    void wrongCodeIsRejectedAndEmailStaysUnconfirmed() throws Exception {
        String wrong = emails.lastCode().equals("000000") ? "000001" : "000000";

        confirm(EMAIL, wrong).andExpect(status().isBadRequest());

        login().andExpect(jsonPath("$.user.emailConfirmed").value(false));
    }

    @Test
    void codeCannotBeReused() throws Exception {
        String code = emails.lastCode();

        confirm(EMAIL, code).andExpect(status().isNoContent());
        confirm(EMAIL, code).andExpect(status().isBadRequest());
    }

    @Test
    void resendGivesTheSameAnswerForKnownAndUnknownEmailAndDoesNotCreateAccounts() throws Exception {
        String known = resend(EMAIL).andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        String unknown = resend("ninguem@exemplo.com").andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();

        assertThat(known).isEqualTo(unknown);
        assertThat(emails.sentEmails()).hasSize(2);
        assertThat(emails.sentEmails().get(1).recipient()).isEqualTo(EMAIL);
    }

    @Test
    void resendDoesNothingWhenEmailIsAlreadyConfirmed() throws Exception {
        confirm(EMAIL, emails.lastCode()).andExpect(status().isNoContent());
        int before = emails.sentEmails().size();

        resend(EMAIL).andExpect(status().isAccepted());

        assertThat(emails.sentEmails()).hasSize(before);
    }

    private ResultActions confirm(String email, String code) throws Exception {
        return mockMvc.perform(post("/api/auth/email/confirm").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"code\":\"%s\"}".formatted(email, code)));
    }

    private ResultActions resend(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/email/resend").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(email)));
    }

    private ResultActions login() throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(EMAIL, PASSWORD)));
    }
}
