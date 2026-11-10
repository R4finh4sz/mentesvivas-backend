package com.mentesvivas.backend.support;

import com.jayway.jsonpath.JsonPath;
import com.mentesvivas.backend.repository.user.UserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.mentesvivas.backend.support.AuthTestSupport.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Component
public class LinkTestSupport {

    public record Account(String token, UUID id, String email) {
    }

    private final ObjectProvider<MockMvc> mockMvcProvider;
    private final AuthTestSupport auth;
    private final TermsTestSupport terms;
    private final CapturedEmailSender emails;
    private final UserRepository userRepository;

    public LinkTestSupport(ObjectProvider<MockMvc> mockMvcProvider, AuthTestSupport auth, TermsTestSupport terms,
                           CapturedEmailSender emails, UserRepository userRepository) {
        this.mockMvcProvider = mockMvcProvider;
        this.auth = auth;
        this.terms = terms;
        this.emails = emails;
        this.userRepository = userRepository;
    }

    public Account patient(String email, String cpf) throws Exception {
        return account(auth.registerAndLogin(email, cpf, "PATIENT"), email);
    }

    public Account caregiver(String email, String cpf) throws Exception {
        return account(auth.registerAndLogin(email, cpf, "CAREGIVER"), email);
    }

    public Account doctor(String email, String cpf, String name, boolean crmVerified) throws Exception {
        MockMvc mockMvc = mockMvcProvider.getObject();
        int version = terms.publish();
        mockMvc.perform(post("/api/auth/register/doctor").contentType(MediaType.APPLICATION_JSON).content("""
                {"fullName":"%s","cpf":"%s","phone":"11988887777",
                 "email":"%s","password":"%s","passwordConfirmation":"%s",
                 "crm":"123456","crmState":"SP","termsVersion":%d,"acceptTerms":true}
                """.formatted(name, cpf, email, AuthTestSupport.PASSWORD, AuthTestSupport.PASSWORD, version)))
                .andExpect(status().isCreated());
        var doctor = userRepository.findByEmail(email).orElseThrow();
        doctor.setCrmVerified(crmVerified);
        userRepository.saveAndFlush(doctor);

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, AuthTestSupport.PASSWORD)))
                .andExpect(status().isAccepted());
        String body = mockMvc.perform(post("/api/auth/2fa/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"code\":\"%s\"}".formatted(email, emails.lastCode())))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return new Account(JsonPath.read(body, "$.accessToken"), doctor.getId(), email);
    }

    public void confirmEmail(String email) throws Exception {
        MockMvc mockMvc = mockMvcProvider.getObject();
        mockMvc.perform(post("/api/auth/email/resend").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(email))).andExpect(status().isAccepted());
        mockMvc.perform(post("/api/auth/email/confirm").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"code\":\"%s\"}".formatted(email, emails.lastCode())))
                .andExpect(status().isNoContent());
    }

    private Account account(String token, String email) throws Exception {
        String body = mockMvcProvider.getObject().perform(get("/api/users/me").header("Authorization", bearer(token)))
                .andReturn().getResponse().getContentAsString();
        return new Account(token, UUID.fromString(JsonPath.read(body, "$.id")), email);
    }
}
