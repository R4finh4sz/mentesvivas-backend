package com.mentesvivas.backend.controller.game;

import com.jayway.jsonpath.JsonPath;
import com.mentesvivas.backend.support.ClearTemporaryStorage;
import com.mentesvivas.backend.support.LinkTestSupport;
import com.mentesvivas.backend.support.LinkTestSupport.Account;
import jakarta.persistence.EntityManager;
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

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static com.mentesvivas.backend.support.AuthTestSupport.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    @Autowired
    private EntityManager entityManager;

    private Account patient;
    private Account caregiver;
    private Account stranger;

    @BeforeEach
    void setUp() throws Exception {
        patient = accounts.patient("paciente@exemplo.com", "11144477735");
        caregiver = accounts.caregiver("cuidador@exemplo.com", "52998224725");
        stranger = accounts.caregiver("outro@exemplo.com", "12345678909");
        linkCaregiver();
        decide("/api/patients/me/consents/FAMILY_PHOTOS", patient, true).andExpect(status().isOk());
        decide("/api/patients/me/consents/FAMILY_AUDIO", patient, true).andExpect(status().isOk());
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
    void separateConsentControlsMediaAndPatientRevocationPrevails() throws Exception {
        String represented = "/api/caregivers/me/patients/" + patient.id() + "/consents/";
        String patientConsents = "/api/patients/me/consents/";
        decide(represented + "GEOLOCATION", stranger, true).andExpect(status().isForbidden());
        decide(represented + "GEOLOCATION", caregiver, true).andExpect(status().isOk())
                .andExpect(jsonPath("$.decidedByPatient").value(false));
        String withAudio = """
                {"name":"Ana","kinship":"filha","photoUrl":"https://img/ana.png","audioUrl":"https://audio/ana.mp3"}
                """;
        register(caregiver, withAudio).andExpect(status().isCreated());

        decide(patientConsents + "FAMILY_AUDIO", patient, false).andExpect(status().isOk())
                .andExpect(jsonPath("$.decidedByPatient").value(true));
        decide(represented + "FAMILY_AUDIO", caregiver, true).andExpect(status().isConflict());
        gallery(patient).andExpect(status().isOk()).andExpect(jsonPath("$.members[0].audioUrl").isEmpty());
        register(caregiver, withAudio).andExpect(status().isForbidden());
        register(caregiver, person("Bia")).andExpect(status().isCreated());

        decide(patientConsents + "FAMILY_PHOTOS", patient, false).andExpect(status().isOk());
        gallery(patient).andExpect(status().isForbidden());
        register(caregiver, person("Caio")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/patients/me/consents").header("Authorization", bearer(patient.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.scope == 'GEOLOCATION')].granted",
                        org.hamcrest.Matchers.contains(true)));
    }

    private ResultActions decide(String path, Account actor, boolean granted) throws Exception {
        return mockMvc.perform(put(path).header("Authorization", bearer(actor.token()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"granted\":" + granted + "}"));
    }

    @Test
    void geofenceRequiresConsentAndNotifiesOnlyOncePerConfirmedExit() throws Exception {
        String consent = "/api/patients/me/consents/GEOLOCATION";
        String caregiverFence = "/api/caregivers/me/patients/" + patient.id() + "/geofence";
        configureFence(caregiverFence).andExpect(status().isForbidden());
        decide(consent, patient, true).andExpect(status().isOk());
        configureFence(caregiverFence).andExpect(status().isOk())
                .andExpect(jsonPath("$.readingIntervalSeconds").value(60));
        mockMvc.perform(get("/api/patients/me/geofence").header("Authorization", bearer(patient.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.notice").isNotEmpty());
        reading("0", 5).andExpect(status().isOk()).andExpect(jsonPath("$.position").value("INSIDE"));
        reading("0.001", 20).andExpect(status().isOk()).andExpect(jsonPath("$.position").value("UNKNOWN"));
        reading("0.003", 10).andExpect(status().isOk()).andExpect(jsonPath("$.position").value("OUTSIDE"));
        reading("0.004", 10).andExpect(status().isOk()).andExpect(jsonPath("$.position").value("OUTSIDE"));
        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(caregiver.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.type == 'GEOFENCE_EXIT')].type",
                        org.hamcrest.Matchers.contains("GEOFENCE_EXIT")));

        decide(consent, patient, false).andExpect(status().isOk());
        reading("0", 5).andExpect(status().isForbidden());
        decide(consent, patient, true).andExpect(status().isOk());
        reading("0", 5).andExpect(status().isNotFound());
    }

    private ResultActions configureFence(String route) throws Exception {
        return mockMvc.perform(put(route).header("Authorization", bearer(caregiver.token()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"latitude\":0,\"longitude\":0,\"radiusMeters\":100}"));
    }

    private ResultActions reading(String longitude, int accuracy) throws Exception {
        return mockMvc.perform(post("/api/patients/me/geofence/readings")
                .header("Authorization", bearer(patient.token())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"latitude\":0,\"longitude\":%s,\"accuracyMeters\":%d}".formatted(longitude, accuracy)));
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
        mockMvc.perform(delete(path() + "/" + UUID.randomUUID())
                .header("Authorization", bearer(caregiver.token())))
                .andExpect(status().isNotFound());
    }

    @Test
    void strangerCannotSeeTheGallery() throws Exception {
        gallery(stranger).andExpect(status().isForbidden());
    }

    @Test
    void recognitionRetriesThenCompletesAndKeepsMetricsPrivate() throws Exception {
        String sessions = "/api/care/patients/" + patient.id() + "/games/family-recognition/sessions";
        String token = bearer(patient.token());
        mockMvc.perform(post(sessions).header("Authorization", token)).andExpect(status().isConflict());

        var membersByPhoto = new HashMap<String, String>();
        for (String name : List.of("Ana", "Bia", "Caio", "Davi")) {
            String photo = "https://img/" + name + ".png";
            String member = register(caregiver, """
                    {"name":"%s","kinship":"filha","photoUrl":"%s","audioUrl":"https://audio/%s.mp3"}
                    """.formatted(name, photo, name)).andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            membersByPhoto.put(photo, JsonPath.read(member, "$.id"));
        }

        mockMvc.perform(post(sessions).header("Authorization", bearer(stranger.token())))
                .andExpect(status().isForbidden());
        String first = mockMvc.perform(post(sessions).header("Authorization", token))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.options.length()").value(4))
                .andExpect(jsonPath("$.audioUrl").doesNotExist()).andReturn().getResponse().getContentAsString();
        String sessionId = JsonPath.read(first, "$.sessionId");
        String photo = JsonPath.read(first, "$.photoUrl");
        String session = sessions + "/" + sessionId;
        entityManager.flush();
        entityManager.clear();
        mockMvc.perform(get(session + "/question").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.photoUrl").value(photo));

        String correct = membersByPhoto.get(photo);
        List<String> options = JsonPath.read(first, "$.options[*].id");
        String wrong = options.stream().filter(id -> !id.equals(correct)).findFirst().orElseThrow();
        answer(session, token, 1, wrong).andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false)).andExpect(jsonPath("$.audioUrl").isEmpty())
                .andExpect(jsonPath("$.nextQuestion.position").value(1))
                .andExpect(jsonPath("$.nextQuestion.errors").doesNotExist());

        for (int position = 1; position <= 4; position++) {
            String audio = photo.replace("https://img/", "https://audio/").replace(".png", ".mp3");
            String response = answer(session, token, position, membersByPhoto.get(photo)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.correct").value(true)).andExpect(jsonPath("$.audioUrl").value(audio))
                    .andExpect(jsonPath("$.finished").value(position == 4))
                    .andReturn().getResponse().getContentAsString();
            if (position < 4) {
                answer(session, token, position, membersByPhoto.get(photo)).andExpect(status().isConflict());
                photo = JsonPath.read(response, "$.nextQuestion.photoUrl");
            }
        }
        mockMvc.perform(get(session + "/question").header("Authorization", token)).andExpect(status().isConflict());
        entityManager.flush();
        Object[] metrics = (Object[]) entityManager.createNativeQuery("""
                SELECT errors, response_ms FROM recognition_questions WHERE session_id = :id AND position = 0
                """).setParameter("id", UUID.fromString(sessionId)).getSingleResult();
        assertThat(metrics[0]).isEqualTo(1);
        assertThat(metrics[1]).isNotNull();
    }

    private ResultActions answer(String session, String token, int position, String memberId) throws Exception {
        return mockMvc.perform(post(session + "/answers").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"position\":%d,\"memberId\":\"%s\"}".formatted(position, memberId)));
    }
}
