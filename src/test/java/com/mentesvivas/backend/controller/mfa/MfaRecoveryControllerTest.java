package com.mentesvivas.backend.controller.mfa;

import com.jayway.jsonpath.JsonPath;
import com.mentesvivas.backend.service.mfa.TotpService;
import com.mentesvivas.backend.support.AuthTestSupport;
import com.mentesvivas.backend.support.CapturedEmailSender;
import com.mentesvivas.backend.support.ClearTemporaryStorage;
import com.mentesvivas.backend.support.LinkTestSupport;
import com.mentesvivas.backend.support.LinkTestSupport.Account;
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
import java.util.List;

import static com.mentesvivas.backend.support.AuthTestSupport.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(ClearTemporaryStorage.class)
@AutoConfigureMockMvc
@Transactional
class MfaRecoveryControllerTest {

    private static final String DOCTOR = "dr.paulo@clinica.com";
    private static final String BASE = "/api/users/me/mfa";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private LinkTestSupport accounts;
    @Autowired
    private CapturedEmailSender emails;
    @Autowired
    private TotpService totp;

    private Account doctor;

    @BeforeEach
    void setUp() throws Exception {
        doctor = accounts.doctor(DOCTOR, "12345678909", "Dr. Paulo Lima", true);
        emails.clear();
    }

    private ResultActions send(String path, String token, String body) throws Exception {
        var request = post(path).contentType(MediaType.APPLICATION_JSON).content(body);
        if (token != null) {
            request.header("Authorization", bearer(token));
        }
        return mockMvc.perform(request);
    }

    private ResultActions login(String password) throws Exception {
        return send("/api/auth/login", null, "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(DOCTOR, password));
    }

    private ResultActions verify(String code) throws Exception {
        return send("/api/auth/2fa/verify", null, "{\"email\":\"%s\",\"code\":\"%s\"}".formatted(DOCTOR, code));
    }

    private ResultActions recover(String password) throws Exception {
        return send("/api/auth/2fa/recover", null, "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(DOCTOR, password));
    }

    private record Enrollment(String secret, List<String> backupCodes) {
    }

    private Enrollment enroll() throws Exception {
        String setup = mockMvc.perform(post(BASE + "/authenticator/setup").header("Authorization", bearer(doctor.token())))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String secret = JsonPath.read(setup, "$.secret");
        String body = send(BASE + "/authenticator/confirm", doctor.token(),
                "{\"code\":\"%s\"}".formatted(totp.codeAt(secret, Instant.now())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("AUTHENTICATOR"))
                .andReturn().getResponse().getContentAsString();
        List<String> codes = JsonPath.read(body, "$.backupCodes");
        return new Enrollment(secret, codes);
    }

    private String nextCode(String secret) {
        return totp.codeAt(secret, Instant.now().plusSeconds(30));
    }

    private String previousCode(String secret) {
        return totp.codeAt(secret, Instant.now().minusSeconds(30));
    }

    @Test
    void activationReturnsTenBackupCodesOnce() throws Exception {
        Enrollment enrollment = enroll();

        assertThat(enrollment.backupCodes()).hasSize(10).doesNotHaveDuplicates()
                .allMatch(code -> code.matches("\\d{10}"));
        mockMvc.perform(get(BASE).header("Authorization", bearer(doctor.token())))
                .andExpect(jsonPath("$.backupCodes").doesNotExist());
    }

    @Test
    void backupCodeCompletesTheLoginAndWorksOnlyOnce() throws Exception {
        String code = enroll().backupCodes().get(0);

        login(AuthTestSupport.PASSWORD).andExpect(status().isAccepted()).andExpect(jsonPath("$.method").value("AUTHENTICATOR"));
        verify(code).andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty());

        login(AuthTestSupport.PASSWORD).andExpect(status().isAccepted());
        verify(code).andExpect(status().isBadRequest());
    }

    @Test
    void unknownBackupCodeIsRejected() throws Exception {
        enroll();

        login(AuthTestSupport.PASSWORD).andExpect(status().isAccepted());
        verify("0000000000").andExpect(status().isBadRequest());
    }

    @Test
    void backupCodesAreRejectedForAccountsOnEmail() throws Exception {
        login(AuthTestSupport.PASSWORD).andExpect(status().isAccepted());

        verify("1234567890").andExpect(status().isBadRequest());
    }

    @Test
    void regeneratingInvalidatesThePreviousCodes() throws Exception {
        Enrollment enrollment = enroll();
        login(AuthTestSupport.PASSWORD).andExpect(status().isAccepted());
        String access = JsonPath.read(verify(nextCode(enrollment.secret())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.accessToken");

        String body = send(BASE + "/backup-codes/regenerate", access,
                "{\"code\":\"%s\"}".formatted(previousCode(enrollment.secret())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.backupCodes.length()").value(10))
                .andReturn().getResponse().getContentAsString();
        List<String> fresh = JsonPath.read(body, "$.backupCodes");

        assertThat(fresh).doesNotContainAnyElementsOf(enrollment.backupCodes());
        login(AuthTestSupport.PASSWORD).andExpect(status().isAccepted());
        verify(enrollment.backupCodes().get(0)).andExpect(status().isBadRequest());
        verify(fresh.get(0)).andExpect(status().isOk());
    }

    @Test
    void regeneratingNeedsAValidCodeAndTheAuthenticator() throws Exception {
        send(BASE + "/backup-codes/regenerate", doctor.token(), "{\"code\":\"123456\"}").andExpect(status().isConflict());

        enroll();
        send(BASE + "/backup-codes/regenerate", doctor.token(), "{\"code\":\"000000\"}").andExpect(status().isBadRequest());
        send(BASE + "/backup-codes/regenerate", null, "{\"code\":\"000000\"}").andExpect(status().isUnauthorized());
    }

    @Test
    void disablingWithABackupCodeReturnsToEmailAndDiscardsTheOtherCodes() throws Exception {
        Enrollment enrollment = enroll();

        send(BASE + "/authenticator/disable", doctor.token(),
                "{\"code\":\"%s\"}".formatted(enrollment.backupCodes().get(0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("EMAIL"));

        login(AuthTestSupport.PASSWORD).andExpect(status().isAccepted()).andExpect(jsonPath("$.method").value("EMAIL"));
        verify(enrollment.backupCodes().get(1)).andExpect(status().isBadRequest());
    }

    @Test
    void lostAppCanBeRecoveredWithEmailAndPassword() throws Exception {
        Enrollment enrollment = enroll();
        emails.clear();

        recover(AuthTestSupport.PASSWORD)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.twoFactorRequired").value(true))
                .andExpect(jsonPath("$.method").value("EMAIL"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());

        assertThat(emails.sentEmails()).hasSize(2);
        assertThat(emails.sentEmails().get(0).subject()).isEqualTo("Seu segundo fator foi alterado");
        assertThat(emails.sentEmails().get(0).recipient()).isEqualTo(DOCTOR);
        assertThat(emails.sentEmails().get(0).body()).doesNotContain("Paulo");

        verify(emails.lastCode()).andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty());
        mockMvc.perform(get(BASE).header("Authorization", bearer(doctor.token())))
                .andExpect(jsonPath("$.method").value("EMAIL"));
        login(AuthTestSupport.PASSWORD).andExpect(status().isAccepted());
        verify(enrollment.backupCodes().get(0)).andExpect(status().isBadRequest());
    }

    @Test
    void recoveryNeedsTheCorrectPasswordAndChangesNothingOtherwise() throws Exception {
        Enrollment enrollment = enroll();
        emails.clear();

        recover("SenhaErrada1").andExpect(status().isUnauthorized());

        assertThat(emails.sentEmails()).isEmpty();
        login(AuthTestSupport.PASSWORD).andExpect(status().isAccepted()).andExpect(jsonPath("$.method").value("AUTHENTICATOR"));
        verify(nextCode(enrollment.secret())).andExpect(status().isOk());
    }

    @Test
    void wrongPasswordsOnRecoveryCountTowardsTheLoginLockout() throws Exception {
        enroll();

        for (int i = 0; i < 5; i++) {
            recover("SenhaErrada1").andExpect(status().isUnauthorized());
        }

        recover(AuthTestSupport.PASSWORD).andExpect(status().isTooManyRequests());
    }

    @Test
    void recoveryWithoutTheAuthenticatorIsAConflict() throws Exception {
        recover(AuthTestSupport.PASSWORD).andExpect(status().isConflict());
    }

    @Test
    void recoveryIsNotAvailableForProfilesWithoutSecondFactor() throws Exception {
        accounts.patient("paciente@exemplo.com", "11144477735");

        send("/api/auth/2fa/recover", null,
                "{\"email\":\"paciente@exemplo.com\",\"password\":\"%s\"}".formatted(AuthTestSupport.PASSWORD))
                .andExpect(status().isConflict());
    }

    @Test
    void fourthCodeRequestInTheWindowIsBlockedWithRetryAfter() throws Exception {
        login(AuthTestSupport.PASSWORD).andExpect(status().isAccepted());
        login(AuthTestSupport.PASSWORD).andExpect(status().isAccepted());

        login(AuthTestSupport.PASSWORD)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.retryAfterSeconds").isNumber());
        assertThat(emails.sentEmails()).hasSize(2);
    }
}
