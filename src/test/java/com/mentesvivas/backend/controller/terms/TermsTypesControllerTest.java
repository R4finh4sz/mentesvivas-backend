package com.mentesvivas.backend.controller.terms;

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
class TermsTypesControllerTest {

    private static final String PASSWORD = "SenhaForte1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TermsTestSupport terms;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = tokenOf(TermsTestSupport.ADMIN_EMAIL, TermsTestSupport.ADMIN_PASSWORD);
    }

    @Test
    void eachTypeHasItsOwnCurrentDocument() throws Exception {
        terms.publish();
        publish("{\"title\":\"Privacidade\",\"content\":\"Texto\",\"type\":\"PRIVACY_POLICY\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("PRIVACY_POLICY"))
                .andExpect(jsonPath("$.audience").value("ALL"));

        mockMvc.perform(get("/api/terms").param("type", "PRIVACY_POLICY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Privacidade"));
        mockMvc.perform(get("/api/terms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("TERMS_OF_USE"));
        mockMvc.perform(get("/api/terms").param("type", "SPECIFIC_CONSENT")).andExpect(status().isNotFound());
    }

    @Test
    void specificAudienceAppearsOnlyForTheChosenProfiles() throws Exception {
        terms.publish();
        publish("{\"title\":\"Consentimento medico\",\"content\":\"Texto\",\"type\":\"SPECIFIC_CONSENT\","
                + "\"audience\":\"SPECIFIC\",\"roles\":[\"DOCTOR\"]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.audience").value("SPECIFIC"))
                .andExpect(jsonPath("$.roles[0]").value("DOCTOR"));

        mockMvc.perform(get("/api/terms").param("type", "SPECIFIC_CONSENT").param("role", "DOCTOR"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/terms").param("type", "SPECIFIC_CONSENT").param("role", "PATIENT"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/terms/signup").param("role", "DOCTOR")).andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/api/terms/signup").param("role", "PATIENT")).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void inconsistentAudienceIsRejected() throws Exception {
        publish("{\"title\":\"X\",\"content\":\"Texto\",\"audience\":\"SPECIFIC\"}")
                .andExpect(status().isBadRequest());
        publish("{\"title\":\"X\",\"content\":\"Texto\",\"audience\":\"ALL\",\"roles\":[\"DOCTOR\"]}")
                .andExpect(status().isBadRequest());
        publish("{\"title\":\"X\",\"content\":\"Texto\",\"audience\":\"SPECIFIC\",\"roles\":[\"ADMINISTRATOR\"]}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void signupRequiresTheCurrentVersionOfEveryApplicableType() throws Exception {
        int tos = terms.publish();
        int privacy = JsonPath.read(publish("{\"title\":\"Privacidade\",\"content\":\"Texto\",\"type\":\"PRIVACY_POLICY\"}")
                .andReturn().getResponse().getContentAsString(), "$.version");

        register("ana@exemplo.com", tos, null).andExpect(status().isConflict());
        register("ana@exemplo.com", tos, privacy).andExpect(status().isCreated());

        String token = tokenOf("ana@exemplo.com", PASSWORD);
        mockMvc.perform(get("/api/terms/accepted").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void documentsForAnotherProfileAreNotRequiredAtSignup() throws Exception {
        int tos = terms.publish();
        publish("{\"title\":\"Consentimento medico\",\"content\":\"Texto\",\"type\":\"SPECIFIC_CONSENT\","
                + "\"audience\":\"SPECIFIC\",\"roles\":[\"DOCTOR\"]}").andExpect(status().isCreated());

        register("bia@exemplo.com", tos, null).andExpect(status().isCreated());
    }

    @Test
    void publishingANewDocumentOfATypeAsksExistingUsersToAcceptItAgain() throws Exception {
        int tos = terms.publish();
        register("carla@exemplo.com", tos, null).andExpect(status().isCreated());
        String token = tokenOf("carla@exemplo.com", PASSWORD);
        login("carla@exemplo.com").andExpect(jsonPath("$.termsAccepted").value(true));

        int privacy = JsonPath.read(publish("{\"title\":\"Privacidade\",\"content\":\"Texto\",\"type\":\"PRIVACY_POLICY\"}")
                .andReturn().getResponse().getContentAsString(), "$.version");
        login("carla@exemplo.com").andExpect(jsonPath("$.termsAccepted").value(false));

        accept(token, privacy).andExpect(status().isOk());
        login("carla@exemplo.com").andExpect(jsonPath("$.termsAccepted").value(true));
    }

    @Test
    void acceptingADocumentOfAnotherProfileOrAnOldOneIsRejected() throws Exception {
        int tos = terms.publish();
        register("edu@exemplo.com", tos, null).andExpect(status().isCreated());
        String token = tokenOf("edu@exemplo.com", PASSWORD);
        int doctorOnly = JsonPath.read(publish("{\"title\":\"Medico\",\"content\":\"Texto\",\"type\":\"SPECIFIC_CONSENT\","
                + "\"audience\":\"SPECIFIC\",\"roles\":[\"DOCTOR\"]}").andReturn().getResponse().getContentAsString(), "$.version");
        int newer = terms.publish();

        accept(token, doctorOnly).andExpect(status().isConflict());
        accept(token, tos).andExpect(status().isConflict());
        accept(token, newer).andExpect(status().isOk());
        accept(token, 9999).andExpect(status().isConflict());
    }

    private ResultActions publish(String json) throws Exception {
        return mockMvc.perform(post("/api/terms").header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions register(String email, int termsVersion, Integer privacyVersion) throws Exception {
        String privacy = privacyVersion == null ? "" : ",\"privacyPolicyVersion\":" + privacyVersion;
        return mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"fullName":"Pessoa Teste","cpf":"52998224725","birthDate":"1950-03-10",
                 "phone":"11999998888","email":"%s","password":"%s","passwordConfirmation":"%s",
                 "role":"CAREGIVER","termsVersion":%d,"acceptTerms":true,"acceptHealthData":true%s}
                """.formatted(email, PASSWORD, PASSWORD, termsVersion, privacy)));
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

    private ResultActions accept(String token, int version) throws Exception {
        return mockMvc.perform(post("/api/terms/accept").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":%d,\"acceptTerms\":true}".formatted(version)));
    }
}
