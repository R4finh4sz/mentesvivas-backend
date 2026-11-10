package com.mentesvivas.backend.controller.user;

import com.mentesvivas.backend.support.AuthTestSupport;
import com.mentesvivas.backend.support.CapturedEmailSender;
import com.mentesvivas.backend.support.ClearTemporaryStorage;
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

import static com.mentesvivas.backend.support.AuthTestSupport.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(ClearTemporaryStorage.class)
@AutoConfigureMockMvc
@Transactional
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthTestSupport auth;

    @Autowired
    private CapturedEmailSender emails;

    @Autowired
    private com.mentesvivas.backend.support.TermsTestSupport terms;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = auth.registerAndLogin("mara@exemplo.com", "52998224725", "PATIENT");
        emails.clear();
    }

    @Test
    void getReturnsTheOwnProfile() throws Exception {
        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("mara@exemplo.com"))
                .andExpect(jsonPath("$.cpf").value("52998224725"))
                .andExpect(jsonPath("$.role").value("PATIENT"));
    }

    @Test
    void profileRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/users/me").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void patchUpdatesOnlyTheInformedFields() throws Exception {
        update("{\"fullName\":\"  Mara   Souza \",\"phone\":\"(11) 98888-7777\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Mara Souza"))
                .andExpect(jsonPath("$.phone").value("11988887777"))
                .andExpect(jsonPath("$.birthDate").value("1950-03-10"))
                .andExpect(jsonPath("$.emailConfirmed").value(false));
    }

    @Test
    void cpfAndCrmCannotBeChangedByTheUser() throws Exception {
        update("{\"cpf\":\"11144477735\",\"crm\":\"999999\",\"role\":\"ADMINISTRATOR\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value("52998224725"))
                .andExpect(jsonPath("$.role").value("PATIENT"))
                .andExpect(jsonPath("$.crm").doesNotExist());
    }

    @Test
    void invalidValuesAreRejected() throws Exception {
        update("{\"phone\":\"123\"}").andExpect(status().isBadRequest());
        update("{\"birthDate\":\"2999-01-01\"}").andExpect(status().isBadRequest());
        update("{\"fullName\":\"   \"}").andExpect(status().isBadRequest());
    }

    @Test
    void changingEmailRequiresNewConfirmationAndSendsCodeToTheNewAddress() throws Exception {
        confirmCurrentEmail();

        update("{\"email\":\"Nova.Mara@Exemplo.com\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("nova.mara@exemplo.com"))
                .andExpect(jsonPath("$.emailConfirmed").value(false));

        assertThat(emails.sentEmails()).singleElement()
                .satisfies(sent -> assertThat(sent.recipient()).isEqualTo("nova.mara@exemplo.com"));
        assertThat(auth.login("nova.mara@exemplo.com")).isNotBlank();
    }

    @Test
    void emailAlreadyInUseReturnsConflict() throws Exception {
        auth.registerAndLogin("outra@exemplo.com", "11144477735", "CAREGIVER");

        update("{\"email\":\"outra@exemplo.com\"}").andExpect(status().isConflict());
    }

    @Test
    void sendingTheSameEmailKeepsConfirmationState() throws Exception {
        confirmCurrentEmail();

        update("{\"email\":\"MARA@exemplo.com\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailConfirmed").value(true));

        assertThat(emails.sentEmails()).isEmpty();
    }

    @Test
    void photoIsStoredAsALinkAndCanBeRemoved() throws Exception {
        update("{\"photoUrl\":\"https://fotos.exemplo.com/mara.jpg\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").value("https://fotos.exemplo.com/mara.jpg"));
        update("{\"fullName\":\"Mara\"}").andExpect(jsonPath("$.photoUrl").value("https://fotos.exemplo.com/mara.jpg"));

        update("{\"photoUrl\":\"\"}").andExpect(status().isOk()).andExpect(jsonPath("$.photoUrl").doesNotExist());
    }

    @Test
    void photoMustBeAnHttpsLink() throws Exception {
        update("{\"photoUrl\":\"http://fotos.exemplo.com/mara.jpg\"}").andExpect(status().isBadRequest());
        update("{\"photoUrl\":\"javascript:alert(1)\"}").andExpect(status().isBadRequest());
        update("{\"photoUrl\":\"https://com espaco.com/a.jpg\"}").andExpect(status().isBadRequest());
    }

    @Test
    void registrationAcceptsAPhotoLink() throws Exception {
        int version = terms.publish();
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"fullName":"Pessoa Teste","cpf":"11144477735","birthDate":"1950-03-10",
                 "phone":"11999998888","email":"foto@exemplo.com","password":"%s","passwordConfirmation":"%s",
                 "role":"CAREGIVER","termsVersion":%d,"acceptTerms":true,"acceptHealthData":true,
                 "photoUrl":"https://fotos.exemplo.com/p.jpg"}
                """.formatted(AuthTestSupport.PASSWORD, AuthTestSupport.PASSWORD, version))).andExpect(status().isCreated());

        String other = auth.login("foto@exemplo.com");
        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(other)))
                .andExpect(jsonPath("$.photoUrl").value("https://fotos.exemplo.com/p.jpg"));
    }

    private void confirmCurrentEmail() throws Exception {
        mockMvc.perform(post("/api/auth/email/resend").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"mara@exemplo.com\"}")).andExpect(status().isAccepted());
        String code = emails.lastCode();
        mockMvc.perform(post("/api/auth/email/confirm").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"mara@exemplo.com\",\"code\":\"%s\"}".formatted(code))).andExpect(status().isNoContent());
        emails.clear();
    }

    private ResultActions update(String json) throws Exception {
        return mockMvc.perform(patch("/api/users/me").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
