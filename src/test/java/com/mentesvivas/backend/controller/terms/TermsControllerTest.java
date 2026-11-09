package com.mentesvivas.backend.controller.terms;

import com.jayway.jsonpath.JsonPath;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(ClearTemporaryStorage.class)
@AutoConfigureMockMvc
@Transactional
class TermsControllerTest {

    private static final String PASSWORD = "SenhaForte1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TermsTestSupport terms;

    @Test
    void withoutPublishedTermsReturns404AndRegistrationFails() throws Exception {
        mockMvc.perform(get("/api/terms")).andExpect(status().isNotFound());
        register("ana@exemplo.com", 1).andExpect(status().isNotFound());
    }

    @Test
    void adminPublishesNewVersionAndCurrentTermsArePublic() throws Exception {
        String token = tokenOf(TermsTestSupport.ADMIN_EMAIL, TermsTestSupport.ADMIN_PASSWORD);

        publish(token, "Termos v1").andExpect(status().isCreated()).andExpect(jsonPath("$.version").value(1));
        publish(token, "Termos v2").andExpect(status().isCreated()).andExpect(jsonPath("$.version").value(2));

        mockMvc.perform(get("/api/terms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.title").value("Termos v2"));
        mockMvc.perform(get("/api/terms/history").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void nonAdminCannotPublishTerms() throws Exception {
        int version = terms.publish();
        register("bia@exemplo.com", version).andExpect(status().isCreated());

        publish(tokenOf("bia@exemplo.com", PASSWORD), "Termos falsos").andExpect(status().isForbidden());
        publish(null, "Termos falsos").andExpect(status().isUnauthorized());
    }

    @Test
    void registrationWithOutdatedVersionReturnsConflict() throws Exception {
        int old = terms.publish();
        terms.publish();

        register("carla@exemplo.com", old).andExpect(status().isConflict());
    }

    @Test
    void registrationRecordsAcceptanceAndNewVersionRequiresNewOne() throws Exception {
        int v1 = terms.publish();
        register("dani@exemplo.com", v1).andExpect(status().isCreated());
        String token = tokenOf("dani@exemplo.com", PASSWORD);

        mockMvc.perform(get("/api/terms/accepted").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].version").value(v1))
                .andExpect(jsonPath("$[0].acceptedAt").exists());

        int v2 = terms.publish();
        login("dani@exemplo.com").andExpect(jsonPath("$.termsAccepted").value(false));

        accept(token, v1).andExpect(status().isConflict());
        accept(token, v2).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(v2));

        login("dani@exemplo.com").andExpect(jsonPath("$.termsAccepted").value(true));
        mockMvc.perform(get("/api/terms/accepted").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void acceptanceRequiresConfirmation() throws Exception {
        int version = terms.publish();
        register("edu@exemplo.com", version).andExpect(status().isCreated());

        mockMvc.perform(post("/api/terms/accept").header("Authorization", "Bearer " + tokenOf("edu@exemplo.com", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":%d,\"acceptTerms\":false}".formatted(version)))
                .andExpect(status().isBadRequest());
    }

    private ResultActions register(String email, int termsVersion) throws Exception {
        return mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"fullName":"Pessoa Teste","cpf":"52998224725","birthDate":"1950-03-10",
                 "phone":"11999998888","email":"%s","password":"%s","passwordConfirmation":"%s",
                 "role":"CAREGIVER","termsVersion":%d,"acceptTerms":true,"acceptHealthData":true}
                """.formatted(email, PASSWORD, PASSWORD, termsVersion)));
    }

    private ResultActions login(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)));
    }

    private String tokenOf(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private ResultActions publish(String token, String title) throws Exception {
        var request = post("/api/terms").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"%s\",\"content\":\"Texto dos termos\"}".formatted(title));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(request);
    }

    private ResultActions accept(String token, int version) throws Exception {
        return mockMvc.perform(post("/api/terms/accept").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":%d,\"acceptTerms\":true}".formatted(version)));
    }
}
