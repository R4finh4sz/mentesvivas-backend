package com.mentesvivas.backend.controller.game;

import com.jayway.jsonpath.JsonPath;
import com.mentesvivas.backend.support.AdminTestSupport;
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

import java.util.List;
import java.util.UUID;

import static com.mentesvivas.backend.support.AuthTestSupport.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(ClearTemporaryStorage.class)
@AutoConfigureMockMvc
@Transactional
class BingoGameControllerTest {

    private static final int ROUNDS = 5;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AdminTestSupport admin;
    @Autowired
    private LinkTestSupport accounts;

    private Account patient;
    private Account caregiver;
    private Account stranger;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = admin.token();
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

    private void addSounds(int count) throws Exception {
        for (int i = 0; i < count; i++) {
            mockMvc.perform(post("/api/admin/bingo-sounds").header("Authorization", bearer(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"instrument\":\"Instrumento" + i + "\",\"audioUrl\":\"https://audio/i" + i + ".mp3\"}"))
                    .andExpect(status().isCreated());
        }
    }

    private String base() {
        return "/api/care/patients/" + patient.id() + "/games/musical-bingo";
    }

    private ResultActions start(Account actor) throws Exception {
        return mockMvc.perform(post(base() + "/sessions").header("Authorization", bearer(actor.token())));
    }

    private String currentRound(Account actor, String sessionId) throws Exception {
        return mockMvc.perform(get(base() + "/sessions/" + sessionId + "/round")
                .header("Authorization", bearer(actor.token()))).andReturn().getResponse().getContentAsString();
    }

    private ResultActions answer(Account actor, String sessionId, String roundId, String soundId) throws Exception {
        return mockMvc.perform(post(base() + "/sessions/" + sessionId + "/answers")
                .header("Authorization", bearer(actor.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roundId\":\"" + roundId + "\",\"soundId\":\"" + soundId + "\"}"));
    }

    private String correctOption(String round) {
        String sound = JsonPath.read(round, "$.soundUrl");
        String instrument = "Instrumento" + sound.substring(sound.lastIndexOf("/i") + 2, sound.length() - 4);
        List<String> names = JsonPath.read(round, "$.options[*].instrument");
        return JsonPath.read(round, "$.options[" + names.indexOf(instrument) + "].soundId");
    }

    private String wrongOption(String round) {
        String correct = correctOption(round);
        List<String> ids = JsonPath.read(round, "$.options[*].soundId");
        return ids.stream().filter(id -> !id.equals(correct)).findFirst().orElseThrow();
    }

    @Test
    void gameIsUnavailableWithFewerThanTenSounds() throws Exception {
        addSounds(9);

        start(patient).andExpect(status().isConflict());
    }

    @Test
    void firstGameHasTwoOptionsFiveSecondsOfSoundAndNoTimer() throws Exception {
        addSounds(10);

        start(patient)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalRounds").value(ROUNDS))
                .andExpect(jsonPath("$.round.soundUrl").isNotEmpty())
                .andExpect(jsonPath("$.round.soundSeconds").value(5))
                .andExpect(jsonPath("$.round.options.length()").value(2))
                .andExpect(jsonPath("$.round.narrationEnabled").value(false))
                .andExpect(jsonPath("$.round.narrationText").doesNotExist())
                .andExpect(jsonPath("$.errorCount").doesNotExist());
    }

    @Test
    void caregiverTurnsNarrationOnForLowVision() throws Exception {
        addSounds(10);

        mockMvc.perform(put(base() + "/settings").header("Authorization", bearer(caregiver.token()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"narrationEnabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.narrationEnabled").value(true));

        mockMvc.perform(get(base() + "/settings").header("Authorization", bearer(patient.token())))
                .andExpect(jsonPath("$.narrationEnabled").value(true));
        start(patient)
                .andExpect(jsonPath("$.round.narrationEnabled").value(true))
                .andExpect(jsonPath("$.round.narrationText").isNotEmpty());
    }

    @Test
    void patientCannotChangeTheNarrationSetting() throws Exception {
        mockMvc.perform(put(base() + "/settings").header("Authorization", bearer(patient.token()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"narrationEnabled\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void narrationSettingRequiresAValue() throws Exception {
        mockMvc.perform(put(base() + "/settings").header("Authorization", bearer(caregiver.token()))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void wrongAnswerIsWelcomingAndKeepsTheSameRound() throws Exception {
        addSounds(10);
        String started = start(caregiver).andReturn().getResponse().getContentAsString();
        String sessionId = JsonPath.read(started, "$.sessionId");
        String round = currentRound(caregiver, sessionId);

        answer(caregiver, sessionId, JsonPath.read(round, "$.roundId"), wrongOption(round))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false))
                .andExpect(jsonPath("$.finished").value(false))
                .andExpect(jsonPath("$.round.roundId").value((String) JsonPath.read(round, "$.roundId")));
    }

    @Test
    void patientPlaysAllRoundsAndConcludes() throws Exception {
        addSounds(10);
        String sessionId = JsonPath.read(start(patient).andReturn().getResponse().getContentAsString(), "$.sessionId");
        String round = currentRound(patient, sessionId);

        for (int i = 1; i <= ROUNDS; i++) {
            ResultActions result = answer(patient, sessionId, JsonPath.read(round, "$.roundId"), correctOption(round))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.correct").value(true));
            if (i < ROUNDS) {
                result.andExpect(jsonPath("$.finished").value(false));
                round = currentRound(patient, sessionId);
            } else {
                result.andExpect(jsonPath("$.finished").value(true)).andExpect(jsonPath("$.round").doesNotExist());
            }
        }

        mockMvc.perform(get(base() + "/sessions/" + sessionId + "/round")
                .header("Authorization", bearer(patient.token()))).andExpect(status().isConflict());
    }

    @Test
    void optionsGrowAfterFinishedGames() throws Exception {
        addSounds(10);
        for (int game = 0; game < 2; game++) {
            String sessionId = JsonPath.read(start(patient).andReturn().getResponse().getContentAsString(), "$.sessionId");
            for (int i = 0; i < ROUNDS; i++) {
                String round = currentRound(patient, sessionId);
                answer(patient, sessionId, JsonPath.read(round, "$.roundId"), correctOption(round));
            }
        }

        start(patient).andExpect(jsonPath("$.round.options.length()").value(3));
    }

    @Test
    void answerOutsideTheOptionsIsRejected() throws Exception {
        addSounds(10);
        String started = start(patient).andReturn().getResponse().getContentAsString();

        answer(patient, JsonPath.read(started, "$.sessionId"), JsonPath.read(started, "$.round.roundId"),
                UUID.randomUUID().toString()).andExpect(status().isBadRequest());
    }

    @Test
    void answerForAnotherRoundIsRejected() throws Exception {
        addSounds(10);
        String started = start(patient).andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(started, "$.round.options[*].soundId");

        answer(patient, JsonPath.read(started, "$.sessionId"), UUID.randomUUID().toString(), ids.get(0))
                .andExpect(status().isConflict());
    }

    @Test
    void unlinkedCaregiverCannotPlayForThePatient() throws Exception {
        addSounds(10);

        start(stranger).andExpect(status().isForbidden());
    }

    @Test
    void unknownSessionReturnsNotFound() throws Exception {
        mockMvc.perform(get(base() + "/sessions/" + UUID.randomUUID() + "/round")
                .header("Authorization", bearer(patient.token()))).andExpect(status().isNotFound());
    }

    @Test
    void answerRequiresRoundAndSound() throws Exception {
        addSounds(10);
        String sessionId = JsonPath.read(start(patient).andReturn().getResponse().getContentAsString(), "$.sessionId");

        mockMvc.perform(post(base() + "/sessions/" + sessionId + "/answers")
                        .header("Authorization", bearer(patient.token()))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
