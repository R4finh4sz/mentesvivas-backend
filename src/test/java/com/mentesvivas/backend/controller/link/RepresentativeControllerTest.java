package com.mentesvivas.backend.controller.link;

import com.jayway.jsonpath.JsonPath;
import com.mentesvivas.backend.entity.audit.AuditEvent;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.repository.audit.AuditEventRepository;
import com.mentesvivas.backend.support.AuthTestSupport;
import com.mentesvivas.backend.support.ClearTemporaryStorage;
import com.mentesvivas.backend.support.LinkTestSupport;
import com.mentesvivas.backend.support.LinkTestSupport.Account;
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

import java.util.UUID;

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
class RepresentativeControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private LinkTestSupport accounts;
    @Autowired
    private TermsTestSupport terms;
    @Autowired
    private AuditEventRepository events;

    private Account patient;
    private Account representative;
    private Account otherCaregiver;
    private Account stranger;
    private Account doctor;

    @BeforeEach
    void setUp() throws Exception {
        patient = accounts.patient("paciente@exemplo.com", "11144477735");
        representative = accounts.caregiver("representante@exemplo.com", "52998224725");
        otherCaregiver = accounts.caregiver("outro@exemplo.com", "39053344705");
        stranger = accounts.caregiver("estranho@exemplo.com", "16899535009");
        doctor = accounts.doctor("dr.paulo@clinica.com", "12345678909", "Dr. Paulo Lima", true);
        invite("/api/caregiver-links/invitations", representative);
        accept("/api/caregiver-links", "/api/caregiver-links/%s/accept", patient);
    }

    private ResultActions send(String path, Account account, String body) throws Exception {
        return mockMvc.perform(post(path).header("Authorization", bearer(account.token()))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void invite(String path, Account sender) throws Exception {
        send(path, sender, "{\"email\":\"paciente@exemplo.com\"}").andExpect(status().isAccepted());
    }

    private String firstLinkId(String path, Account viewer) throws Exception {
        return JsonPath.read(mockMvc.perform(get(path).header("Authorization", bearer(viewer.token())))
                .andReturn().getResponse().getContentAsString(), "$[0].id");
    }

    private void accept(String listPath, String acceptPath, Account who) throws Exception {
        send(acceptPath.formatted(firstLinkId(listPath, who)), who, "{}").andExpect(status().isOk());
    }

    private String pendingCaregiverLinkId(Account inviter) throws Exception {
        String body = mockMvc.perform(get("/api/caregiver-links").header("Authorization", bearer(inviter.token())))
                .andReturn().getResponse().getContentAsString();
        java.util.List<String> ids = JsonPath.read(body, "$[?(@.status == 'PENDING')].id");
        return ids.get(0);
    }

    private AuditEvent latest(UUID actor, AuditEventType type) {
        return events.findByActorUserIdOrderByOccurredAtDesc(actor).stream()
                .filter(event -> event.getEventType() == type).findFirst().orElseThrow();
    }

    @Test
    void representativeAcceptsAnotherCaregiverInvitationInTheNameOfThePatient() throws Exception {
        invite("/api/caregiver-links/invitations", otherCaregiver);
        String linkId = pendingCaregiverLinkId(otherCaregiver);

        send("/api/caregiver-links/" + linkId + "/accept", representative, "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        AuditEvent event = latest(representative.id(), AuditEventType.CAREGIVER_LINK_ACCEPTED);
        assertThat(event.getPatientUserId()).isEqualTo(patient.id());
        assertThat(event.getOnBehalfOfUserId()).isEqualTo(patient.id());
    }

    @Test
    void representativeDeclinesAnInvitationInTheNameOfThePatient() throws Exception {
        invite("/api/caregiver-links/invitations", otherCaregiver);

        send("/api/caregiver-links/" + pendingCaregiverLinkId(otherCaregiver) + "/decline", representative, "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"));
    }

    @Test
    void thePatientStillAnswersByThemselvesAndTheAuditHasNoOnBehalf() throws Exception {
        invite("/api/caregiver-links/invitations", otherCaregiver);

        send("/api/caregiver-links/" + pendingCaregiverLinkId(otherCaregiver) + "/accept", patient, "{}")
                .andExpect(status().isOk());

        assertThat(latest(patient.id(), AuditEventType.CAREGIVER_LINK_ACCEPTED).getOnBehalfOfUserId()).isNull();
    }

    @Test
    void caregiverWithoutAnActiveLinkCannotAnswerForThePatient() throws Exception {
        invite("/api/caregiver-links/invitations", otherCaregiver);

        send("/api/caregiver-links/" + pendingCaregiverLinkId(otherCaregiver) + "/accept", stranger, "{}")
                .andExpect(status().isForbidden());
    }

    @Test
    void theInvitingCaregiverCannotApproveTheirOwnInvitation() throws Exception {
        invite("/api/caregiver-links/invitations", otherCaregiver);

        send("/api/caregiver-links/" + pendingCaregiverLinkId(otherCaregiver) + "/accept", otherCaregiver, "{}")
                .andExpect(status().isForbidden());
    }

    @Test
    void representativeAcceptsTheDoctorInvitationInTheNameOfThePatient() throws Exception {
        invite("/api/doctor-links/invitations", doctor);
        String linkId = firstLinkId("/api/doctor-links", patient);

        send("/api/doctor-links/" + linkId + "/accept", representative, "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        assertThat(latest(representative.id(), AuditEventType.DOCTOR_LINK_ACCEPTED).getOnBehalfOfUserId())
                .isEqualTo(patient.id());
    }

    @Test
    void caregiverWithoutAnActiveLinkCannotAnswerTheDoctorInvitation() throws Exception {
        invite("/api/doctor-links/invitations", doctor);
        String linkId = firstLinkId("/api/doctor-links", patient);

        send("/api/doctor-links/" + linkId + "/accept", stranger, "{}").andExpect(status().isForbidden());
    }

    @Test
    void representativeAcceptsTheNewTermsInTheNameOfThePatient() throws Exception {
        int version = terms.publish();
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"paciente@exemplo.com\",\"password\":\"%s\"}".formatted(AuthTestSupport.PASSWORD)))
                .andExpect(jsonPath("$.termsAccepted").value(false));

        send("/api/caregivers/me/patients/" + patient.id() + "/terms/accept", representative,
                "{\"version\":%d,\"acceptTerms\":true}".formatted(version))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(version));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"paciente@exemplo.com\",\"password\":\"%s\"}".formatted(AuthTestSupport.PASSWORD)))
                .andExpect(jsonPath("$.termsAccepted").value(true));
        AuditEvent event = latest(representative.id(), AuditEventType.TERMS_ACCEPTED);
        assertThat(event.getPatientUserId()).isEqualTo(patient.id());
        assertThat(event.getOnBehalfOfUserId()).isEqualTo(patient.id());
    }

    @Test
    void termsCannotBeAcceptedForAPatientBySomeoneWhoDoesNotRepresentThem() throws Exception {
        int version = terms.publish();
        String body = "{\"version\":%d,\"acceptTerms\":true}".formatted(version);

        send("/api/caregivers/me/patients/" + patient.id() + "/terms/accept", stranger, body).andExpect(status().isForbidden());
        send("/api/caregivers/me/patients/" + patient.id() + "/terms/accept", doctor, body).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/caregivers/me/patients/" + patient.id() + "/terms/accept")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endingTheCaregiverLinkEndsTheRepresentation() throws Exception {
        String linkId = firstLinkId("/api/caregiver-links", patient);
        send("/api/caregiver-links/" + linkId + "/end", patient, "{}").andExpect(status().isOk());
        int version = terms.publish();

        send("/api/caregivers/me/patients/" + patient.id() + "/terms/accept", representative,
                "{\"version\":%d,\"acceptTerms\":true}".formatted(version)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/caregivers/me/patients/" + patient.id() + "/access-log")
                .header("Authorization", bearer(representative.token()))).andExpect(status().isForbidden());
    }

    @Test
    void representativeConsultsTheAccessLogOfThePatient() throws Exception {
        mockMvc.perform(get("/api/caregivers/me/patients/" + patient.id() + "/access-log")
                        .header("Authorization", bearer(representative.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.totalItems").value(0));
        mockMvc.perform(get("/api/caregivers/me/patients/" + patient.id() + "/access-log")
                        .header("Authorization", bearer(stranger.token())))
                .andExpect(status().isForbidden());
    }
}
