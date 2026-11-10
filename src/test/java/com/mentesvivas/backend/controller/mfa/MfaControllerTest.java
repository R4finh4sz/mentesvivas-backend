package com.mentesvivas.backend.controller.mfa;

import com.jayway.jsonpath.JsonPath;
import com.mentesvivas.backend.service.mfa.TotpService;
import com.mentesvivas.backend.support.AuthTestSupport;
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

import java.time.Instant;

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
class MfaControllerTest {

    private static final String DOCTOR = "dr.paulo@clinica.com";
    private static final String BASE = "/api/users/me/mfa";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TermsTestSupport terms;
    @Autowired
    private CapturedEmailSender emails;
    @Autowired
    private TotpService totp;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        int version = terms.publish();
        mockMvc.perform(post("/api/auth/register/doctor").contentType(MediaType.APPLICATION_JSON).content("""
                {"fullName":"Dr. Paulo","cpf":"12345678909","phone":"11988887777",
                 "email":"%s","password":"%s","passwordConfirmation":"%s",
                 "crm":"123456","crmState":"SP","termsVersion":%d,"acceptTerms":true}
                """.formatted(DOCTOR, AuthTestSupport.PASSWORD, AuthTestSupport.PASSWORD, version)))
                .andExpect(status().isCreated());
        token = loginWithEmailCode();
    }

    @Test
    void emailIsTheDefaultMethod() throws Exception {
        mockMvc.perform(get(BASE).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("EMAIL"))
                .andExpect(jsonPath("$.availableMethods.length()").value(2));
    }

    @Test
    void routesRequireAuthentication() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE + "/authenticator/setup")).andExpect(status().isUnauthorized());
    }

    @Test
    void enrollingTheAuthenticatorSwitchesTheSecondStepOfTheNextLogin() throws Exception {
        String secret = enroll();

        login().andExpect(status().isAccepted())
                .andExpect(jsonPath("$.twoFactorRequired").value(true))
                .andExpect(jsonPath("$.method").value("AUTHENTICATOR"));
        assertThat(emails.sentEmails()).isEmpty();

        verify(nextCode(secret)).andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("DOCTOR"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void emailCodesStopWorkingOnceTheAuthenticatorIsActive() throws Exception {
        enroll();
        emails.clear();

        login().andExpect(status().isAccepted());
        verify("123456").andExpect(status().isBadRequest());
    }

    @Test
    void wrongAuthenticatorCodeIsRejectedAndAnAcceptedOneCannotBeReplayed() throws Exception {
        String secret = enroll();
        login().andExpect(status().isAccepted());
        String right = nextCode(secret);
        String wrong = right.equals("000000") ? "000001" : "000000";

        verify(wrong).andExpect(status().isBadRequest());
        verify(right).andExpect(status().isOk());

        login().andExpect(status().isAccepted());
        verify(right).andExpect(status().isBadRequest());
    }

    @Test
    void confirmingWithAWrongCodeKeepsEmailAsTheMethod() throws Exception {
        mockMvc.perform(post(BASE + "/authenticator/setup").header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        authedPost(BASE + "/authenticator/confirm", "{\"code\":\"000000\"}").andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE).header("Authorization", bearer(token))).andExpect(jsonPath("$.method").value("EMAIL"));
    }

    @Test
    void confirmingWithoutStartingTheSetupIsAConflict() throws Exception {
        authedPost(BASE + "/authenticator/confirm", "{\"code\":\"123456\"}").andExpect(status().isConflict());
    }

    @Test
    void setupExposesTheSecretAndAnOtpauthLink() throws Exception {
        mockMvc.perform(post(BASE + "/authenticator/setup").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secret").isNotEmpty())
                .andExpect(jsonPath("$.otpauthUri").value(org.hamcrest.Matchers.startsWith("otpauth://totp/MentesVivas:")));
    }

    @Test
    void settingUpAgainWhileActiveIsAConflict() throws Exception {
        enroll();

        mockMvc.perform(post(BASE + "/authenticator/setup").header("Authorization", bearer(token)))
                .andExpect(status().isConflict());
    }

    @Test
    void disablingNeedsAValidCodeAndReturnsToEmail() throws Exception {
        String secret = enroll();
        String access = completeLogin(secret);

        mockMvc.perform(post(BASE + "/authenticator/disable").header("Authorization", bearer(access))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"000000\"}"))
                .andExpect(status().isBadRequest());

        String code = previousCode(secret);
        mockMvc.perform(post(BASE + "/authenticator/disable").header("Authorization", bearer(access))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"%s\"}".formatted(code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("EMAIL"));

        emails.clear();
        login().andExpect(status().isAccepted()).andExpect(jsonPath("$.method").value("EMAIL"));
        assertThat(emails.sentEmails()).hasSize(1);
    }

    @Test
    void disablingWithoutTheAuthenticatorActiveIsAConflict() throws Exception {
        authedPost(BASE + "/authenticator/disable", "{\"code\":\"123456\"}").andExpect(status().isConflict());
    }

    private String enroll() throws Exception {
        String body = mockMvc.perform(post(BASE + "/authenticator/setup").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String secret = JsonPath.read(body, "$.secret");
        authedPost(BASE + "/authenticator/confirm", "{\"code\":\"%s\"}".formatted(codeNow(secret)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("AUTHENTICATOR"));
        return secret;
    }

    private String completeLogin(String secret) throws Exception {
        login().andExpect(status().isAccepted());
        String body = verify(nextCode(secret)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private String codeNow(String secret) {
        return totp.codeAt(secret, Instant.now());
    }

    private String previousCode(String secret) {
        return totp.codeAt(secret, Instant.now().minusSeconds(30));
    }

    private String nextCode(String secret) {
        return totp.codeAt(secret, Instant.now().plusSeconds(30));
    }

    private String loginWithEmailCode() throws Exception {
        login().andExpect(status().isAccepted());
        String body = verify(emails.lastCode()).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        emails.clear();
        return JsonPath.read(body, "$.accessToken");
    }

    private ResultActions authedPost(String url, String json) throws Exception {
        return mockMvc.perform(post(url)
                .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions login() throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(DOCTOR, AuthTestSupport.PASSWORD)));
    }

    private ResultActions verify(String code) throws Exception {
        return mockMvc.perform(post("/api/auth/2fa/verify").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"code\":\"%s\"}".formatted(DOCTOR, code)));
    }
}
