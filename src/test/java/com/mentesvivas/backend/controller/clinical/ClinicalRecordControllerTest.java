package com.mentesvivas.backend.controller.clinical;

import com.jayway.jsonpath.JsonPath;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.game.BingoRound;
import com.mentesvivas.backend.entity.game.BingoSession;
import com.mentesvivas.backend.entity.game.RecognitionQuestion;
import com.mentesvivas.backend.entity.game.RecognitionSession;
import com.mentesvivas.backend.repository.game.BingoRoundRepository;
import com.mentesvivas.backend.repository.game.BingoSessionRepository;
import com.mentesvivas.backend.repository.game.RecognitionSessionRepository;
import com.mentesvivas.backend.repository.audit.AuditEventRepository;
import com.mentesvivas.backend.support.AuthTestSupport;
import com.mentesvivas.backend.support.AdminTestSupport;
import com.mentesvivas.backend.support.CapturedEmailSender;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.mentesvivas.backend.support.AuthTestSupport.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
    @Autowired
    private CapturedEmailSender emails;
    @Autowired
    private AuditEventRepository auditEvents;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private AdminTestSupport admin;

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
    void healthConsentRevocationBlocksCareUntilPatientAcceptsAgain() throws Exception {
        linkDoctor();
        accept("/api/caregiver-links/invitations", "/api/caregiver-links", caregiver);
        String consent = "/api/patients/me/consents/health-data";
        String patientToken = bearer(patient.token());
        String caregiverToken = bearer(caregiver.token());
        String vitals = "/api/care/patients/" + patient.id() + "/vital-signs";
        String measurement = "{\"heartRate\":70}";

        mockMvc.perform(get(consent).header("Authorization", patientToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.granted").value(true));
        mockMvc.perform(put(consent).header("Authorization", patientToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"granted\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.granted").value(false))
                .andExpect(jsonPath("$.revokedAt").isNotEmpty());

        mockMvc.perform(get(vitals).header("Authorization", patientToken)).andExpect(status().isForbidden());
        mockMvc.perform(post(vitals).header("Authorization", caregiverToken)
                .contentType(MediaType.APPLICATION_JSON).content(measurement)).andExpect(status().isForbidden());
        selectPainScale("NUMERIC").andExpect(status().isForbidden());
        mockMvc.perform(put(consent).header("Authorization", caregiverToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"granted\":true}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users/me/data-requests").header("Authorization", patientToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/patients/me/sos").header("Authorization", patientToken))
                .andExpect(status().isCreated());

        mockMvc.perform(put(consent).header("Authorization", patientToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"granted\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.granted").value(true))
                .andExpect(jsonPath("$.acceptedAt").isNotEmpty());
        mockMvc.perform(get(vitals).header("Authorization", patientToken)).andExpect(status().isOk());
        mockMvc.perform(post(vitals).header("Authorization", caregiverToken)
                .contentType(MediaType.APPLICATION_JSON).content(measurement)).andExpect(status().isCreated());
        selectPainScale("NUMERIC").andExpect(status().isOk());
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
                .andExpect(jsonPath("$.categoriesWithoutData[0]").value("MEDICATION_DOSES"))
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
                .andExpect(jsonPath("$.categoriesWithoutData.length()").value(6))
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
                .andExpect(jsonPath("$.categoriesWithoutData.length()").value(5));
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
                .andExpect(jsonPath("$.items.length()").value(4));
    }

    @Test
    void timelineExportRequiresReauthenticationAndAuditsThePdf() throws Exception {
        mockMvc.perform(post(base() + "/timeline/export-challenge").header("Authorization", bearer(doctor.token())))
                .andExpect(status().isForbidden());
        linkDoctor();
        care("vital-signs", "{\"systolic\":120,\"diastolic\":80}");
        String carePath = "/api/care/patients/" + patient.id();
        String medication = mockMvc.perform(post(carePath + "/medications")
                        .header("Authorization", bearer(patient.token())).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Losartana","strength":"50 mg","unitsPerDose":1,
                                 "times":["08:00"],"stockQuantity":10}
                                """))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String medicationId = JsonPath.read(medication, "$.id");
        care("medications/" + medicationId + "/doses",
                "{\"scheduledFor\":\"%s\",\"status\":\"TAKEN\"}".formatted(Instant.now().minusSeconds(60)));

        mockMvc.perform(post(base() + "/timeline/export-challenge").header("Authorization", bearer(doctor.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.method").value("EMAIL"));
        String code = emails.lastCode();
        export("senha-incorreta", code).andExpect(status().isForbidden());
        byte[] pdf = export(AuthTestSupport.PASSWORD, code)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsByteArray();
        PdfReader reader = new PdfReader(pdf);
        try {
            String text = new PdfTextExtractor(reader).getTextFromPage(1);
            assertThat(text).contains("Linha do tempo clínica", "Dr. Paulo Lima", "120/80 mmHg", "Losartana", "TAKEN");
        } finally {
            reader.close();
        }
        assertThat(auditEvents.findByPatientUserIdOrderByOccurredAtDesc(patient.id()))
                .filteredOn(event -> event.getEventType() == AuditEventType.CLINICAL_TIMELINE_EXPORTED)
                .singleElement().satisfies(event -> assertThat(event.getActorUserId()).isEqualTo(doctor.id()));
        export(AuthTestSupport.PASSWORD, code).andExpect(status().isBadRequest());
    }

    private ResultActions export(String password, String code) throws Exception {
        return mockMvc.perform(post(base() + "/timeline/export")
                .header("Authorization", bearer(doctor.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"%s\",\"code\":\"%s\"}".formatted(password, code)));
    }

    @Test
    void caregiverUsesTheScaleSelectedByTheDoctorAndHistoryPreservesScaleChanges() throws Exception {
        String path = "/api/care/patients/" + patient.id();
        String numeric = "{\"scale\":\"NUMERIC\",\"score\":7}";
        String faces = "{\"scale\":\"WONG_BAKER\",\"score\":6}";
        pain(path, numeric).andExpect(status().isForbidden());
        accept("/api/caregiver-links/invitations", "/api/caregiver-links", caregiver);
        mockMvc.perform(get(path + "/pain-scale").header("Authorization", bearer(caregiver.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.scale").value("NUMERIC"));
        pain(path, numeric).andExpect(status().isCreated());
        selectPainScale("NUMERIC").andExpect(status().isForbidden());
        linkDoctor();
        selectPainScale("NUMERIC").andExpect(status().isOk()).andExpect(jsonPath("$.allowedScores.length()").value(11));
        selectPainScale("WONG_BAKER").andExpect(status().isOk());
        pain(path, numeric).andExpect(status().isConflict());
        pain(path, faces.replace("6", "7")).andExpect(status().isBadRequest());
        pain(path, faces).andExpect(status().isCreated()).andExpect(jsonPath("$.recordedBy").value(caregiver.id().toString()));
        selectPainScale("PAINAD").andExpect(status().isOk());
        pain(path, "{\"scale\":\"PAINAD\",\"score\":10}").andExpect(status().isCreated());
        selectPainScale("DOLOPLUS_2").andExpect(status().isOk());
        pain(path, "{\"scale\":\"DOLOPLUS_2\",\"score\":31}").andExpect(status().isBadRequest());
        pain(path, "{\"scale\":\"DOLOPLUS_2\",\"score\":30}").andExpect(status().isCreated());

        mockMvc.perform(get(path + "/pain-records").header("Authorization", bearer(doctor.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].scale",
                        org.hamcrest.Matchers.containsInAnyOrder("NUMERIC", "WONG_BAKER", "PAINAD", "DOLOPLUS_2")));
    }

    private ResultActions selectPainScale(String scale) throws Exception {
        return mockMvc.perform(put(base() + "/pain-scale").header("Authorization", bearer(doctor.token()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"scale\":\"%s\"}".formatted(scale)));
    }

    private ResultActions pain(String path, String body) throws Exception {
        return mockMvc.perform(post(path + "/pain-records").header("Authorization", bearer(caregiver.token()))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void caregiverCanRestrictSelfRecordAndGamesWhileEmergencyRemainsAvailable() throws Exception {
        String autonomy = "/api/caregivers/me/patients/" + patient.id() + "/autonomy";
        String patientToken = bearer(patient.token());
        String carePath = "/api/care/patients/" + patient.id();
        String records = carePath + "/vital-signs";
        String measurement = "{\"heartRate\":70}";
        String assisted = """
                {"mode":"ASSISTED","restrictCareRecords":true,"restrictGames":true}
                """;
        configureAutonomy(autonomy, assisted).andExpect(status().isForbidden());
        accept("/api/caregiver-links/invitations", "/api/caregiver-links", caregiver);
        configureAutonomy(autonomy, assisted).andExpect(status().isOk());
        configureAutonomy(autonomy, assisted).andExpect(status().isOk());
        mockMvc.perform(get("/api/patients/me/autonomy").header("Authorization", patientToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mode").value("ASSISTED"));
        mockMvc.perform(get("/api/notifications").header("Authorization", patientToken))
                .andExpect(jsonPath("$[?(@.type == 'AUTONOMY_CHANGED')].type",
                        org.hamcrest.Matchers.contains("AUTONOMY_CHANGED")));
        mockMvc.perform(post(records).header("Authorization", patientToken)
                .contentType(MediaType.APPLICATION_JSON).content(measurement)).andExpect(status().isForbidden());
        mockMvc.perform(post(records).header("Authorization", bearer(caregiver.token()))
                .contentType(MediaType.APPLICATION_JSON).content(measurement)).andExpect(status().isCreated());
        mockMvc.perform(get(records).header("Authorization", patientToken)).andExpect(status().isOk());
        mockMvc.perform(post(carePath + "/games/musical-bingo/sessions").header("Authorization", patientToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/patients/me/sos").header("Authorization", patientToken))
                .andExpect(status().isCreated());
        configureAutonomy(autonomy, """
                {"mode":"AUTONOMOUS","restrictCareRecords":false,"restrictGames":false}
                """).andExpect(status().isOk());
        patientPost(records, measurement);
        configureAutonomy(autonomy, assisted).andExpect(status().isOk());
        String links = mockMvc.perform(get("/api/caregiver-links").header("Authorization", patientToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String linkId = JsonPath.read(links, "$[0].id");
        mockMvc.perform(post("/api/caregiver-links/" + linkId + "/end").header("Authorization", patientToken))
                .andExpect(status().isOk());
        patientPost(records, measurement);
    }

    private ResultActions configureAutonomy(String path, String body) throws Exception {
        return mockMvc.perform(put(path).header("Authorization", bearer(caregiver.token()))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void codedDiagnosisRequiresActiveLinkAndValidCatalogCodesAndPreservesHistory() throws Exception {
        String adminToken = bearer(admin.token());
        mockMvc.perform(post("/api/admin/cid10/catalog").header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"entries":[{"code":"G300","description":"Entrada de teste"},
                                    {"code":"I10","description":"Entrada secundária de teste"}]}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.imported").value(2));
        mockMvc.perform(get("/api/doctors/me/cid10").param("code", "g30")
                .header("Authorization", bearer(doctor.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].code").value("G30.0"));
        String diagnosis = """
                {"disease":"Condição validada","primaryCid":"g300","secondaryCids":["I10"]}
                """;
        confirmDiagnosis(diagnosis).andExpect(status().isForbidden());
        linkDoctor();
        confirmDiagnosis(diagnosis.replace("I10", "X99")).andExpect(status().isBadRequest());
        confirmDiagnosis(diagnosis.replace("I10", "G30.0")).andExpect(status().isBadRequest());
        confirmDiagnosis(diagnosis).andExpect(status().isCreated())
                .andExpect(jsonPath("$.primaryCid").value("G30.0"))
                .andExpect(jsonPath("$.secondaryCids[0]").value("I10"))
                .andExpect(jsonPath("$.recordedBy").value(doctor.id().toString()));
        confirmDiagnosis(diagnosis.replace("Condição validada", "Condição revisada"))
                .andExpect(status().isCreated());
        as(doctor, "/diagnoses").andExpect(status().isOk())
                .andExpect(jsonPath("$[*].disease",
                        org.hamcrest.Matchers.containsInAnyOrder("Condição validada", "Condição revisada")));
        as(doctor, "/summary").andExpect(status().isOk())
                .andExpect(jsonPath("$.declaredDisease").value("Condição revisada"))
                .andExpect(jsonPath("$.declaredDiseaseValidated").value(true));
    }

    private ResultActions confirmDiagnosis(String body) throws Exception {
        return mockMvc.perform(post(base() + "/diagnoses").header("Authorization", bearer(doctor.token()))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void doctorPatientListFiltersActiveLinksByActivityAndPendingAlert() throws Exception {
        String directory = "/api/doctors/me/patients";
        String token = bearer(doctor.token());
        mockMvc.perform(post("/api/doctor-links/invitations").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"paciente@exemplo.com\"}"))
                .andExpect(status().isAccepted());
        mockMvc.perform(get(directory).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

        String invitations = mockMvc.perform(get("/api/doctor-links").header("Authorization", bearer(patient.token())))
                .andReturn().getResponse().getContentAsString();
        String linkId = JsonPath.read(invitations, "$[0].id");
        mockMvc.perform(post("/api/doctor-links/" + linkId + "/accept").header("Authorization", bearer(patient.token())))
                .andExpect(status().isOk());
        entityManager.flush();
        mockMvc.perform(get(directory).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(patient.id().toString()))
                .andExpect(jsonPath("$[0].pendingAlert").value(false))
                .andExpect(jsonPath("$[0].lastActivityAt").isEmpty());

        care("vital-signs", "{\"systolic\":120,\"diastolic\":80}");
        mockMvc.perform(post("/api/patients/me/sos").header("Authorization", bearer(patient.token())))
                .andExpect(status().isCreated());
        entityManager.flush();
        mockMvc.perform(get(directory).param("pendingAlert", "true")
                        .param("activeSince", Instant.now().minusSeconds(60).toString()).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].pendingAlert").value(true))
                .andExpect(jsonPath("$[0].lastActivityAt").isNotEmpty());
        mockMvc.perform(get(directory).param("pendingAlert", "false").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
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
