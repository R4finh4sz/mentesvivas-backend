package com.mentesvivas.backend.controller.alert;

import com.jayway.jsonpath.JsonPath;
import com.mentesvivas.backend.entity.audit.AuditEvent;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.repository.audit.AuditEventRepository;
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

import static com.mentesvivas.backend.support.AuthTestSupport.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(ClearTemporaryStorage.class)
@AutoConfigureMockMvc
@Transactional
class SosControllerTest {

    private static final String SEVERE_ALLERGY =
            "{\"category\":\"MEDICATION\",\"description\":\"Dipirona\",\"severity\":\"SEVERE\"}";
    private static final String MILD_ALLERGY =
            "{\"category\":\"FOOD\",\"description\":\"Amendoim\",\"severity\":\"MILD\"}";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private LinkTestSupport accounts;
    @Autowired
    private AuditEventRepository events;

    private Account patient;
    private Account caregiver;
    private Account doctor;

    @BeforeEach
    void setUp() throws Exception {
        patient = accounts.patient("paciente@exemplo.com", "11144477735");
        caregiver = accounts.caregiver("cuidador@exemplo.com", "52998224725");
        doctor = accounts.doctor("dr.paulo@clinica.com", "12345678909", "Dr. Paulo Lima", true);
    }

    private String alertsPath(Account of) {
        return "/api/care/patients/" + of.id() + "/alerts";
    }

    private ResultActions trigger(Account actor) throws Exception {
        return mockMvc.perform(post("/api/patients/me/sos").header("Authorization", bearer(actor.token())));
    }

    private ResultActions transition(Account actor, String alertId, String step) throws Exception {
        return mockMvc.perform(post(alertsPath(patient) + "/" + alertId + "/" + step)
                .header("Authorization", bearer(actor.token())));
    }

    private String triggerId() throws Exception {
        String body = trigger(patient).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.alert.id");
    }

    private void addAllergy(String body) throws Exception {
        mockMvc.perform(post("/api/patients/me/allergies").header("Authorization", bearer(patient.token()))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
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

    private List<AuditEventType> typesOf(Account actor) {
        return events.findByActorUserIdOrderByOccurredAtDesc(actor.id()).stream().map(AuditEvent::getEventType).toList();
    }

    @Test
    void patientTriggersTheAlertShowingSevereAllergiesAndEmergencyContacts() throws Exception {
        addAllergy(SEVERE_ALLERGY);
        addAllergy(MILD_ALLERGY);

        trigger(patient)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alert.status").value("CREATED"))
                .andExpect(jsonPath("$.alert.patientId").value(patient.id().toString()))
                .andExpect(jsonPath("$.alert.notice").value(org.hamcrest.Matchers.containsString("192")))
                .andExpect(jsonPath("$.severeAllergies.length()").value(1))
                .andExpect(jsonPath("$.severeAllergies[0].description").value("Dipirona"))
                .andExpect(jsonPath("$.emergencyContacts.length()").value(2))
                .andExpect(jsonPath("$.emergencyContacts[0].order").value(1));
    }

    @Test
    void linkedCaregiverIsNotifiedAboutTheAlert() throws Exception {
        linkCaregiver();
        String alertId = triggerId();

        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(caregiver.token())))
                .andExpect(jsonPath("$[?(@.type == 'SOS_ALERT')].referenceId").value(alertId))
                .andExpect(jsonPath("$[?(@.type == 'SOS_ALERT')].read").value(false));
    }

    @Test
    void triggerIsRecordedInTheAuditTrail() throws Exception {
        triggerId();

        assertThat(typesOf(patient)).contains(AuditEventType.SOS_TRIGGERED);
    }

    @Test
    void caregiverWalksTheAlertThroughSeenAcknowledgedAndResolved() throws Exception {
        linkCaregiver();
        String alertId = triggerId();

        transition(caregiver, alertId, "seen")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SEEN"))
                .andExpect(jsonPath("$.seenBy").value(caregiver.id().toString()));
        transition(caregiver, alertId, "acknowledge")
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.acknowledgedBy").value(caregiver.id().toString()));
        transition(caregiver, alertId, "resolve")
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedBy").value(caregiver.id().toString()));

        assertThat(typesOf(caregiver)).contains(AuditEventType.ALERT_SEEN, AuditEventType.ALERT_ACKNOWLEDGED,
                AuditEventType.ALERT_RESOLVED);
    }

    @Test
    void acknowledgingDirectlyAlsoFillsTheSeenStep() throws Exception {
        linkCaregiver();
        String alertId = triggerId();

        transition(caregiver, alertId, "acknowledge")
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.seenBy").value(caregiver.id().toString()));
    }

    @Test
    void repeatedOrBackwardTransitionIsAConflict() throws Exception {
        linkCaregiver();
        String alertId = triggerId();
        transition(caregiver, alertId, "acknowledge").andExpect(status().isOk());

        transition(caregiver, alertId, "acknowledge").andExpect(status().isConflict());
        transition(caregiver, alertId, "seen").andExpect(status().isConflict());
    }

    @Test
    void patientCanResolveItsOwnAlertButCannotMarkItSeen() throws Exception {
        String alertId = triggerId();

        transition(patient, alertId, "seen").andExpect(status().isForbidden());
        transition(patient, alertId, "resolve")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedBy").value(patient.id().toString()));
    }

    @Test
    void pendingFilterHidesResolvedAlerts() throws Exception {
        String resolved = triggerId();
        triggerId();
        transition(patient, resolved, "resolve").andExpect(status().isOk());

        mockMvc.perform(get(alertsPath(patient)).header("Authorization", bearer(patient.token())))
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get(alertsPath(patient)).param("pending", "true")
                        .header("Authorization", bearer(patient.token())))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("CREATED"));
    }

    @Test
    void caregiverReadsTheDetailAndTheAccessIsLogged() throws Exception {
        linkCaregiver();
        addAllergy(SEVERE_ALLERGY);
        String alertId = triggerId();

        mockMvc.perform(get(alertsPath(patient) + "/" + alertId).header("Authorization", bearer(caregiver.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.severeAllergies[0].description").value("Dipirona"));

        mockMvc.perform(get("/api/patients/me/access-log").header("Authorization", bearer(patient.token())))
                .andExpect(jsonPath("$.items[0].area").value("ALERTS"))
                .andExpect(jsonPath("$.items[0].accessorId").value(caregiver.id().toString()));
    }

    @Test
    void unlinkedCaregiverIsDeniedEverywhere() throws Exception {
        String alertId = triggerId();

        mockMvc.perform(get(alertsPath(patient)).header("Authorization", bearer(caregiver.token())))
                .andExpect(status().isForbidden());
        transition(caregiver, alertId, "seen").andExpect(status().isForbidden());
        transition(caregiver, alertId, "acknowledge").andExpect(status().isForbidden());
        transition(caregiver, alertId, "resolve").andExpect(status().isForbidden());
    }

    @Test
    void doctorWithoutLinkCannotSeeTheAlerts() throws Exception {
        mockMvc.perform(get(alertsPath(patient)).header("Authorization", bearer(doctor.token())))
                .andExpect(status().isForbidden());
    }

    @Test
    void anotherPatientCannotReachTheAlert() throws Exception {
        Account other = accounts.patient("outro@exemplo.com", "39053344705");
        String alertId = triggerId();

        mockMvc.perform(get(alertsPath(patient)).header("Authorization", bearer(other.token())))
                .andExpect(status().isForbidden());
        transition(other, alertId, "resolve").andExpect(status().isForbidden());
    }

    @Test
    void alertIsNotReachableThroughAnotherPatientPath() throws Exception {
        Account other = accounts.patient("outro@exemplo.com", "39053344705");
        String alertId = triggerId();

        mockMvc.perform(get("/api/care/patients/" + other.id() + "/alerts/" + alertId)
                        .header("Authorization", bearer(other.token())))
                .andExpect(status().isNotFound());
    }

    @Test
    void onlyPatientsTriggerTheSos() throws Exception {
        trigger(caregiver).andExpect(status().isForbidden());
        trigger(doctor).andExpect(status().isForbidden());
    }

    @Test
    void anonymousRequestsAreUnauthorized() throws Exception {
        mockMvc.perform(post("/api/patients/me/sos")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(alertsPath(patient))).andExpect(status().isUnauthorized());
    }
}
