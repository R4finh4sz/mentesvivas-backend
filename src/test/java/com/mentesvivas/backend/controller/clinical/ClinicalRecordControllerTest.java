package com.mentesvivas.backend.controller.clinical;

import com.jayway.jsonpath.JsonPath;
import com.mentesvivas.backend.entity.game.BingoRound;
import com.mentesvivas.backend.entity.game.BingoSession;
import com.mentesvivas.backend.entity.game.RecognitionQuestion;
import com.mentesvivas.backend.entity.game.RecognitionSession;
import com.mentesvivas.backend.repository.game.BingoRoundRepository;
import com.mentesvivas.backend.repository.game.BingoSessionRepository;
import com.mentesvivas.backend.repository.game.RecognitionSessionRepository;
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
class ClinicalRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private LinkTestSupport accounts;

    @Autowired
    private RecognitionSessionRepository recognitionSessions;
    @Autowired
    private BingoSessionRepository bingoSessions;
    @Autowired
    private BingoRoundRepository bingoRounds;

    private Account patient;
    private Account caregiver;
    private Account doctor;

    @BeforeEach
    void setUp() throws Exception {
        patient = accounts.patient("paciente@exemplo.com", "11144477735");
        caregiver = accounts.caregiver("cuidador@exemplo.com", "52998224725");
        doctor = accounts.doctor("dr.paulo@clinica.com", "12345678909", "Dr. Paulo Lima", true);
    }

    private String base() {
        return "/api/doctors/me/patients/" + patient.id();
    }

    private ResultActions as(Account actor, String path) throws Exception {
        return mockMvc.perform(get(base() + path).header("Authorization", bearer(actor.token())));
    }

    private void accept(String invitationPath, String linksPath, Account sender) throws Exception {
        mockMvc.perform(post(invitationPath).header("Authorization", bearer(sender.token()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"paciente@exemplo.com\"}"))
                .andExpect(status().isAccepted());
        String links = mockMvc.perform(get(linksPath).header("Authorization", bearer(patient.token())))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(links, "$[0].id");
        mockMvc.perform(post(linksPath + "/" + id + "/accept").header("Authorization", bearer(patient.token())))
                .andExpect(status().isOk());
    }

    private void linkDoctor() throws Exception {
        accept("/api/doctor-links/invitations", "/api/doctor-links", doctor);
    }

    private void patientPost(String path, String body) throws Exception {
        mockMvc.perform(post(path).header("Authorization", bearer(patient.token()))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
    }

    private void care(String resource, String body) throws Exception {
        patientPost("/api/care/patients/" + patient.id() + "/" + resource, body);
    }

    @Test
    void summaryShowsRegistrationDataAndKeepsUnvalidatedInformationLabeled() throws Exception {
        linkDoctor();
        mockMvc.perform(put("/api/patients/me/profile").header("Authorization", bearer(patient.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"declaredDisease\":\"Esquecimentos\",\"bloodType\":\"O_POSITIVE\"}"))
                .andExpect(status().isOk());
        patientPost("/api/patients/me/allergies",
                "{\"category\":\"MEDICATION\",\"description\":\"Dipirona\",\"severity\":\"SEVERE\"}");
        patientPost("/api/patients/me/measurements", "{\"weightKg\":72.5,\"heightM\":1.70}");

        as(doctor, "/summary")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(patient.id().toString()))
                .andExpect(jsonPath("$.email").value("paciente@exemplo.com"))
                .andExpect(jsonPath("$.declaredDisease").value("Esquecimentos"))
                .andExpect(jsonPath("$.declaredDiseaseValidated").value(false))
                .andExpect(jsonPath("$.bloodType").value("O_POSITIVE"))
                .andExpect(jsonPath("$.allergyStatus").value("INFORMED"))
                .andExpect(jsonPath("$.allergies[0].description").value("Dipirona"))
                .andExpect(jsonPath("$.weightKg").value(72.5))
                .andExpect(jsonPath("$.bmi").isNotEmpty())
                .andExpect(jsonPath("$.careTeam.doctors[0].fullName").value("Dr. Paulo Lima"))
                .andExpect(jsonPath("$.cpf").doesNotExist());
    }

    @Test
    void summaryOfAPatientWithoutDataNeverImpliesNoAllergies() throws Exception {
        linkDoctor();

        as(doctor, "/summary")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allergyStatus").value("NOT_INFORMED"))
                .andExpect(jsonPath("$.allergies.length()").value(0))
                .andExpect(jsonPath("$.weightKg").doesNotExist())
                .andExpect(jsonPath("$.declaredDisease").doesNotExist());
    }

    @Test
    void doctorReadingIsRegisteredInThePatientAccessLog() throws Exception {
        linkDoctor();
        as(doctor, "/summary").andExpect(status().isOk());

        mockMvc.perform(get("/api/patients/me/access-log").header("Authorization", bearer(patient.token())))
                .andExpect(jsonPath("$.items[*].area", org.hamcrest.Matchers.containsInAnyOrder("PROFILE", "MEDICATIONS")))
                .andExpect(jsonPath("$.items[0].accessorId").value(doctor.id().toString()));
    }

    @Test
    void gamePerformanceReportsOnlyCompletedSessionsOfTheLinkedPatientAndLogsTheRead() throws Exception {
        String route = "/game-performance";
        as(doctor, route).andExpect(status().isForbidden());
        linkDoctor();
        as(patient, route).andExpect(status().isForbidden());
        as(caregiver, route).andExpect(status().isForbidden());
        as(doctor, route).andExpect(status().isOk())
                .andExpect(jsonPath("$.familyRecognition.completedSessions").value(0))
                .andExpect(jsonPath("$.musicalBingo.averageResponseMs").isEmpty());

        saveRecognition(patient.id(), true);
        saveRecognition(patient.id(), false);
        Account other = accounts.patient("outro@exemplo.com", "39053344705");
        saveRecognition(other.id(), true);
        saveBingo(patient.id());

        as(doctor, route).andExpect(status().isOk())
                .andExpect(jsonPath("$.familyRecognition.completedSessions").value(1))
                .andExpect(jsonPath("$.familyRecognition.answeredQuestions").value(2))
                .andExpect(jsonPath("$.familyRecognition.errors").value(1))
                .andExpect(jsonPath("$.familyRecognition.averageResponseMs").value(250.0))
                .andExpect(jsonPath("$.musicalBingo.completedSessions").value(1))
                .andExpect(jsonPath("$.musicalBingo.answeredQuestions").value(1))
                .andExpect(jsonPath("$.musicalBingo.errors").value(2))
                .andExpect(jsonPath("$.musicalBingo.averageResponseMs").value(400.0))
                .andExpect(jsonPath("$.notice").value(org.hamcrest.Matchers.containsString("avaliação neuropsicológica")));

        mockMvc.perform(get("/api/patients/me/access-log").header("Authorization", bearer(patient.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].area").value("GAME_PERFORMANCE"));
    }

    private void saveRecognition(UUID patientId, boolean completed) {
        Instant now = Instant.now();
        RecognitionSession session = new RecognitionSession();
        session.setPatientId(patientId);
        session.setStartedBy(patientId);
        session.setStartedAt(now);
        session.setQuestionShownAt(now);
        session.setFinishedAt(completed ? now : null);
        UUID memberId = UUID.randomUUID();
        RecognitionQuestion first = new RecognitionQuestion(memberId, List.of(memberId));
        first.recordError();
        first.recordAnswer(200);
        RecognitionQuestion second = new RecognitionQuestion(memberId, List.of(memberId));
        second.recordAnswer(300);
        session.getQuestions().addAll(List.of(first, second));
        recognitionSessions.save(session);
    }

    private void saveBingo(UUID patientId) {
        Instant now = Instant.now();
        BingoSession session = new BingoSession();
        session.setPatientId(patientId);
        session.setStartedBy(patientId);
        session.setStartedAt(now);
        session.setFinishedAt(now);
        session.setTotalRounds(1);
        session.setErrorCount(2);
        bingoSessions.save(session);
        BingoRound round = new BingoRound();
        round.setSessionId(session.getId());
        round.setSoundId(UUID.randomUUID());
        round.defineOptions(List.of(round.getSoundId()));
        round.setAttempts(3);
        round.setAnsweredAt(now);
        round.setResponseMs(400L);
        bingoRounds.save(round);
    }

    @Test
    void timelineGathersTheRecordsOfThePeriod() throws Exception {
        linkDoctor();
        care("vital-signs", "{\"systolic\":120,\"diastolic\":80,\"heartRate\":72}");
        care("routine", "{\"type\":\"MOOD\",\"mood\":\"CALM\"}");
        care("routine", "{\"type\":\"SLEEP\",\"sleepQuality\":\"GOOD\",\"sleepHours\":7.5}");
        care("routine", "{\"type\":\"HYDRATION\",\"amountMl\":200}");
        care("occurrences", "{\"type\":\"FALL\",\"severity\":\"MILD\",\"description\":\"Escorregou\","
                + "\"needsHospital\":false}");
        patientPost("/api/patients/me/measurements", "{\"weightKg\":72.5}");

        as(doctor, "/timeline")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasData").value(true))
                .andExpect(jsonPath("$.categoriesWithoutData.length()").value(0))
                .andExpect(jsonPath("$.vitalSigns[0].systolic").value(120))
                .andExpect(jsonPath("$.moods[0].mood").value("CALM"))
                .andExpect(jsonPath("$.sleeps[0].sleepHours").value(7.5))
                .andExpect(jsonPath("$.occurrences[0].type").value("FALL"))
                .andExpect(jsonPath("$.weights[0].weightKg").value(72.5));
    }

    @Test
    void emptyPeriodReportsTheAbsenceWithoutPresumedValues() throws Exception {
        linkDoctor();

        as(doctor, "/timeline")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasData").value(false))
                .andExpect(jsonPath("$.categoriesWithoutData.length()").value(5))
                .andExpect(jsonPath("$.vitalSigns.length()").value(0))
                .andExpect(jsonPath("$.moods.length()").value(0))
                .andExpect(jsonPath("$.weights.length()").value(0));
    }

    @Test
    void periodFiltersTheRecords() throws Exception {
        linkDoctor();
        care("routine", "{\"type\":\"MOOD\",\"mood\":\"JOY\",\"occurredAt\":\"2026-01-10T08:00:00Z\"}");
        care("routine", "{\"type\":\"MOOD\",\"mood\":\"SADNESS\",\"occurredAt\":\"2026-03-10T08:00:00Z\"}");

        as(doctor, "/timeline?from=2026-01-01T00:00:00Z&to=2026-01-31T00:00:00Z")
                .andExpect(jsonPath("$.moods.length()").value(1))
                .andExpect(jsonPath("$.moods[0].mood").value("JOY"))
                .andExpect(jsonPath("$.categoriesWithoutData.length()").value(4));
    }

    @Test
    void invalidPeriodIsRejected() throws Exception {
        linkDoctor();

        as(doctor, "/timeline?from=2026-02-01T00:00:00Z&to=2026-01-01T00:00:00Z")
                .andExpect(status().isBadRequest());
        as(doctor, "/timeline?from=2020-01-01T00:00:00Z&to=2026-01-01T00:00:00Z")
                .andExpect(status().isBadRequest());
    }

    @Test
    void timelineReadingIsLoggedPerArea() throws Exception {
        linkDoctor();
        as(doctor, "/timeline").andExpect(status().isOk());

        mockMvc.perform(get("/api/patients/me/access-log").header("Authorization", bearer(patient.token())))
                .andExpect(jsonPath("$.items.length()").value(3));
    }

    @Test
    void unlinkedDoctorIsDenied() throws Exception {
        as(doctor, "/summary").andExpect(status().isForbidden());
        as(doctor, "/timeline").andExpect(status().isForbidden());
    }

    @Test
    void doctorWithoutVerifiedCrmIsDenied() throws Exception {
        Account unverified = accounts.doctor("dra.ana@clinica.com", "39053344705", "Dra. Ana", false);

        as(unverified, "/summary").andExpect(status().isForbidden());
        as(unverified, "/timeline").andExpect(status().isForbidden());
    }

    @Test
    void patientsAndCaregiversCannotUseTheDoctorRoutes() throws Exception {
        as(patient, "/summary").andExpect(status().isForbidden());
        as(caregiver, "/timeline").andExpect(status().isForbidden());
    }

    @Test
    void anonymousRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get(base() + "/summary")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(base() + "/timeline")).andExpect(status().isUnauthorized());
    }
}
