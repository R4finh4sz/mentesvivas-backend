package com.mentesvivas.backend.controller.game;

import com.jayway.jsonpath.JsonPath;
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

import static com.mentesvivas.backend.support.AuthTestSupport.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(ClearTemporaryStorage.class)
@AutoConfigureMockMvc
@Transactional
class FamilyMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private LinkTestSupport accounts;

    private Account patient;
    private Account caregiver;
    private Account stranger;

    @BeforeEach
    void setUp() throws Exception {
        patient = accounts.patient("paciente@exemplo.com", "11144477735");
        caregiver = accounts.caregiver("cuidador@exemplo.com", "52998224725");
        stranger = accounts.caregiver("outro@exemplo.com", "12345678909");
        linkCaregiver();
    }

    private void linkCaregiver() throws Exception {
        mockMvc.perform(post("/api/caregiver-links/invitations").header("Authorization", bearer(caregiver.token()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"paciente@exemplo.com\"}"))
                .andExpect(status().isAccepted());
        String links = mockMvc.perform(get("/api/caregiver-links").header("Authorization", bearer(patient.token())))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(links, "$[0].id");
        mockMvc.perform(post("/api/caregiver-links/" + id + "/accept").header("Authorization", bearer(patient.token())))
                .andExpect(status().isOk());
    }

    private String path() {
        return "/api/care/patients/" + patient.id() + "/family-members";
    }

    private String person(String name) {
        return "{\"name\":\"" + name + "\",\"kinship\":\"filha\",\"photoUrl\":\"https://img/" + name + ".png\"}";
    }

    private ResultActions register(Account actor, String body) throws Exception {
        return mockMvc.perform(post(path()).header("Authorization", bearer(actor.token()))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions gallery(Account actor) throws Exception {
        return mockMvc.perform(get(path()).header("Authorization", bearer(actor.token())));
    }

    @Test
    void caregiverRegistersAPersonWithOptionalAudio() throws Exception {
        register(caregiver, "{\"name\":\"Ana\",\"kinship\":\"filha\",\"photoUrl\":\"https://img/ana.png\","
                + "\"audioUrl\":\"https://audio/ana.mp3\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Ana"))
                .andExpect(jsonPath("$.kinship").value("filha"))
                .andExpect(jsonPath("$.audioUrl").value("https://audio/ana.mp3"));
    }

    @Test
    void photoMustBeAnHttpsLink() throws Exception {
        register(caregiver, "{\"name\":\"Ana\",\"kinship\":\"filha\",\"photoUrl\":\"http://img/ana.png\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void nameAndKinshipAreRequired() throws Exception {
        register(caregiver, "{\"photoUrl\":\"https://img/ana.png\"}").andExpect(status().isBadRequest());
    }

    @Test
    void patientCannotRegisterPeople() throws Exception {
        register(patient, person("Ana")).andExpect(status().isForbidden());
    }

    @Test
    void unlinkedCaregiverCannotRegisterPeople() throws Exception {
        register(stranger, person("Ana")).andExpect(status().isForbidden());
    }

    @Test
    void gameStaysLockedUntilFourPeopleAreRegistered() throws Exception {
        register(caregiver, person("Ana")).andExpect(status().isCreated());
        register(caregiver, person("Bia")).andExpect(status().isCreated());
        register(caregiver, person("Caio")).andExpect(status().isCreated());

        gallery(patient)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(3))
                .andExpect(jsonPath("$.recognitionGameUnlocked").value(false))
                .andExpect(jsonPath("$.guidance").isNotEmpty());

        register(caregiver, person("Davi")).andExpect(status().isCreated());

        gallery(caregiver)
                .andExpect(jsonPath("$.recognitionGameUnlocked").value(true))
                .andExpect(jsonPath("$.minimumForRecognitionGame").value(4));
    }

    @Test
    void patientCannotDeleteFamilyPhotos() throws Exception {
        String created = register(caregiver, person("Ana")).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");

        mockMvc.perform(delete(path() + "/" + id).header("Authorization", bearer(patient.token())))
                .andExpect(status().isForbidden());
    }

    @Test
    void caregiverRemovesAPerson() throws Exception {
        String created = register(caregiver, person("Ana")).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");

        mockMvc.perform(delete(path() + "/" + id).header("Authorization", bearer(caregiver.token())))
                .andExpect(status().isNoContent());
        gallery(caregiver).andExpect(jsonPath("$.members.length()").value(0));
    }

    @Test
    void removingAnUnknownPersonReturnsNotFound() throws Exception {
        mockMvc.perform(delete(path() + "/" + java.util.UUID.randomUUID())
                .header("Authorization", bearer(caregiver.token())))
                .andExpect(status().isNotFound());
    }

    @Test
    void strangerCannotSeeTheGallery() throws Exception {
        gallery(stranger).andExpect(status().isForbidden());
    }
}
