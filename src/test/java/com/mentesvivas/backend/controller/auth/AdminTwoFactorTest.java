package com.mentesvivas.backend.controller.auth;

import com.mentesvivas.backend.support.AdminTestSupport;
import com.mentesvivas.backend.support.CapturedEmailSender;
import com.mentesvivas.backend.support.ClearTemporaryStorage;
import com.mentesvivas.backend.support.TermsTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static com.mentesvivas.backend.support.AuthTestSupport.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(ClearTemporaryStorage.class)
@AutoConfigureMockMvc
@Transactional
class AdminTwoFactorTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AdminTestSupport admin;
    @Autowired
    private CapturedEmailSender emails;

    private ResultActions loginAdmin() throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}"
                        .formatted(TermsTestSupport.ADMIN_EMAIL, TermsTestSupport.ADMIN_PASSWORD)));
    }

    @Test
    void passwordAloneDoesNotGiveTheAdministratorAnyToken() throws Exception {
        emails.clear();

        loginAdmin()
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.twoFactorRequired").value(true))
                .andExpect(jsonPath("$.method").value("EMAIL"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());

        assertThat(emails.sentEmails()).singleElement()
                .satisfies(sent -> assertThat(sent.recipient()).isEqualTo(TermsTestSupport.ADMIN_EMAIL));
    }

    @Test
    void administratorReachesTheAdminAreaAfterTheSecondStep() throws Exception {
        String token = admin.token();

        mockMvc.perform(get("/api/admin/doctors/pending-crm").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void wrongCodeDoesNotGiveTheAdministratorATokenAndTheAreaStaysClosed() throws Exception {
        loginAdmin().andExpect(status().isAccepted());

        mockMvc.perform(post("/api/auth/2fa/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"code\":\"000000\"}".formatted(TermsTestSupport.ADMIN_EMAIL)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/doctors/pending-crm")).andExpect(status().isUnauthorized());
    }
}
