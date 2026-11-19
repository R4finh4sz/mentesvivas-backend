package com.mentesvivas.backend.controller.audit;

import com.jayway.jsonpath.JsonPath;
import com.mentesvivas.backend.entity.audit.AuditEvent;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.repository.audit.AuditEventRepository;
import com.mentesvivas.backend.support.AdminTestSupport;
import com.mentesvivas.backend.support.AuthTestSupport;
import com.mentesvivas.backend.support.CapturedEmailSender;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(ClearTemporaryStorage.class)
@AutoConfigureMockMvc
@Transactional
class AuditTrailIntegrationTest {

    private static final String ALLERGY = "{\"category\":\"MEDICATION\",\"description\":\"Dipirona\",\"severity\":\"SEVERE\"}";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AuditEventRepository events;
    @Autowired
    private LinkTestSupport accounts;
    @Autowired
    private AdminTestSupport admin;
    @Autowired
    private CapturedEmailSender emails;

    private Account patient;
    private Account caregiver;
    private Account doctor;

    @BeforeEach
    void setUp() throws Exception {
        patient = accounts.patient("paciente@exemplo.com", "11144477735");
        caregiver = accounts.caregiver("cuidador@exemplo.com", "52998224725");
        doctor = accounts.doctor("dr.paulo@clinica.com", "12345678909", "Dr. Paulo Lima", true);
    }

    private List<AuditEventType> typesOf(UUID actor) {
        return events.findByActorUserIdOrderByOccurredAtDesc(actor).stream().map(AuditEvent::getEventType).toList();
    }

    private AuditEvent latest(UUID actor, AuditEventType type) {
        return events.findByActorUserIdOrderByOccurredAtDesc(actor).stream()
                .filter(event -> event.getEventType() == type).findFirst().orElseThrow();
    }

    private ResultActions send(String path, String token, String body) throws Exception {
        var request = post(path).contentType(MediaType.APPLICATION_JSON).content(body);
        if (token != null) {
            request.header("Authorization", bearer(token));
        }
        return mockMvc.perform(request);
    }

    private void link(String invitePath, Account sender, String linksPath, String acceptPath) throws Exception {
        send(invitePath, sender.token(), "{\"email\":\"paciente@exemplo.com\"}").andExpect(status().isAccepted());
        String id = JsonPath.read(mockMvc.perform(get(linksPath).header("Authorization", bearer(patient.token())))
                .andReturn().getResponse().getContentAsString(), "$[0].id");
        send(acceptPath.formatted(id), patient.token(), "{}").andExpect(status().isOk());
    }

    @Test
    void registrationAndLoginAreRecordedWithTheUserIdAndNoIdentification() throws Exception {
        assertThat(typesOf(patient.id())).contains(AuditEventType.ACCOUNT_REGISTERED, AuditEventType.LOGIN_SUCCEEDED);
        AuditEvent registered = latest(patient.id(), AuditEventType.ACCOUNT_REGISTERED);
        assertThat(registered.getAfterValue()).isEqualTo("PATIENT");
        assertThat(registered.getBeforeValue()).isNull();
    }

    @Test
    void wrongPasswordIsRecordedAsAFailureOfTheUserEvenThoughTheRequestFails() throws Exception {
        send("/api/auth/login", null, "{\"email\":\"paciente@exemplo.com\",\"password\":\"SenhaErrada1\"}")
                .andExpect(status().isUnauthorized());

        assertThat(typesOf(patient.id())).contains(AuditEventType.LOGIN_FAILED);
    }

    @Test
    void unknownEmailFailureIsRecordedWithoutAnyUser() throws Exception {
        long before = events.findByActorUserIdOrderByOccurredAtDesc(null).size();

        send("/api/auth/login", null, "{\"email\":\"ninguem@exemplo.com\",\"password\":\"SenhaErrada1\"}")
                .andExpect(status().isUnauthorized());

        List<AuditEvent> anonymous = events.findByActorUserIdOrderByOccurredAtDesc(null);
        assertThat(anonymous).hasSize((int) before + 1);
        assertThat(anonymous.get(0).getEventType()).isEqualTo(AuditEventType.LOGIN_FAILED);
    }

    @Test
    void secondFactorChallengeSuccessAndFailureAreRecorded() throws Exception {
        send("/api/auth/login", null, "{\"email\":\"dr.paulo@clinica.com\",\"password\":\"%s\"}"
                .formatted(AuthTestSupport.PASSWORD)).andExpect(status().isAccepted());
        send("/api/auth/2fa/verify", null, "{\"email\":\"dr.paulo@clinica.com\",\"code\":\"000000\"}")
                .andExpect(status().isBadRequest());

        assertThat(typesOf(doctor.id())).contains(AuditEventType.TWO_FACTOR_CHALLENGED,
                AuditEventType.TWO_FACTOR_SUCCEEDED, AuditEventType.TWO_FACTOR_FAILED);
    }

    @Test
    void passwordRecoveryRequestAndResetAreRecorded() throws Exception {
        send("/api/auth/password/forgot", null, "{\"email\":\"paciente@exemplo.com\"}").andExpect(status().isAccepted());
        send("/api/auth/password/reset", null, ("{\"email\":\"paciente@exemplo.com\",\"code\":\"%s\","
                + "\"newPassword\":\"NovaSenha123\",\"passwordConfirmation\":\"NovaSenha123\"}").formatted(emails.lastCode()))
                .andExpect(status().isNoContent());

        assertThat(typesOf(patient.id())).contains(AuditEventType.PASSWORD_RECOVERY_REQUESTED, AuditEventType.PASSWORD_RESET);
    }

    @Test
    void logoutRefreshAndReuseAreRecorded() throws Exception {
        String login = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"paciente@exemplo.com\",\"password\":\"%s\"}".formatted(AuthTestSupport.PASSWORD)))
                .andReturn().getResponse().getContentAsString();
        String first = JsonPath.read(login, "$.refreshToken");
        String renewed = send("/api/auth/refresh", null, "{\"refreshToken\":\"%s\"}".formatted(first))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        send("/api/auth/refresh", null, "{\"refreshToken\":\"%s\"}".formatted(first)).andExpect(status().isUnauthorized());
        String second = JsonPath.read(renewed, "$.refreshToken");
        send("/api/auth/logout", null, "{\"refreshToken\":\"%s\"}".formatted(second));

        assertThat(typesOf(patient.id())).contains(AuditEventType.TOKEN_REFRESHED, AuditEventType.REFRESH_TOKEN_REUSED);
    }

    @Test
    void caregiverLinkLifecycleIsRecordedWithTheAffectedPatient() throws Exception {
        link("/api/caregiver-links/invitations", caregiver, "/api/caregiver-links", "/api/caregiver-links/%s/accept");
        String id = JsonPath.read(mockMvc.perform(get("/api/caregiver-links").header("Authorization", bearer(patient.token())))
                .andReturn().getResponse().getContentAsString(), "$[0].id");
        send("/api/caregiver-links/" + id + "/end", patient.token(), "{}").andExpect(status().isOk());

        assertThat(typesOf(caregiver.id())).contains(AuditEventType.CAREGIVER_LINK_INVITED);
        assertThat(typesOf(patient.id())).contains(AuditEventType.CAREGIVER_LINK_ACCEPTED, AuditEventType.CAREGIVER_LINK_ENDED);
        assertThat(latest(patient.id(), AuditEventType.CAREGIVER_LINK_ACCEPTED).getPatientUserId()).isEqualTo(patient.id());
    }

    @Test
    void doctorLinkLifecycleIsRecorded() throws Exception {
        link("/api/doctor-links/invitations", doctor, "/api/doctor-links", "/api/doctor-links/%s/accept");

        assertThat(typesOf(doctor.id())).contains(AuditEventType.DOCTOR_LINK_INVITED);
        assertThat(typesOf(patient.id())).contains(AuditEventType.DOCTOR_LINK_ACCEPTED);
    }

    @Test
    void allergyAndDiseaseChangesCarryBeforeAndAfterValues() throws Exception {
        link("/api/doctor-links/invitations", doctor, "/api/doctor-links", "/api/doctor-links/%s/accept");
        String created = send("/api/doctors/me/patients/" + patient.id() + "/allergies", doctor.token(), ALLERGY)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String allergyId = JsonPath.read(created, "$.id");
        mockMvc.perform(put("/api/doctors/me/patients/" + patient.id() + "/allergies/" + allergyId)
                        .header("Authorization", bearer(doctor.token())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"FOOD\",\"description\":\"Amendoim\",\"severity\":\"MILD\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/doctors/me/patients/" + patient.id() + "/declared-disease")
                        .header("Authorization", bearer(doctor.token())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disease\":\"Alzheimer inicial\"}"))
                .andExpect(status().isNoContent());

        AuditEvent createdEvent = latest(doctor.id(), AuditEventType.ALLERGY_CREATED);
        assertThat(createdEvent.getPatientUserId()).isEqualTo(patient.id());
        assertThat(createdEvent.getBeforeValue()).isNull();
        assertThat(createdEvent.getAfterValue()).contains("Dipirona");
        AuditEvent updatedEvent = latest(doctor.id(), AuditEventType.ALLERGY_UPDATED);
        assertThat(updatedEvent.getBeforeValue()).contains("Dipirona");
        assertThat(updatedEvent.getAfterValue()).contains("Amendoim");
        AuditEvent disease = latest(doctor.id(), AuditEventType.DECLARED_DISEASE_CHANGED);
        assertThat(disease.getAfterValue()).isEqualTo("Alzheimer inicial");
    }

    @Test
    void patientAllergyAndStatusChangesAreRecordedWithThePatientAsActorAndSubject() throws Exception {
        send("/api/patients/me/allergies", patient.token(), ALLERGY).andExpect(status().isCreated());

        AuditEvent event = latest(patient.id(), AuditEventType.ALLERGY_CREATED);
        assertThat(event.getPatientUserId()).isEqualTo(patient.id());
        assertThat(event.getActorUserId()).isEqualTo(patient.id());
    }

    @Test
    void termsPublicationAndAcceptanceAreRecorded() throws Exception {
        String adminToken = admin.token();
        UUID adminId = UUID.fromString(JsonPath.read(mockMvc.perform(get("/api/users/me")
                .header("Authorization", bearer(adminToken))).andReturn().getResponse().getContentAsString(), "$.id"));
        send("/api/terms", adminToken, "{\"title\":\"Termos novos\",\"content\":\"Conteudo\"}").andExpect(status().isCreated());

        assertThat(latest(adminId, AuditEventType.TERMS_PUBLISHED).getAfterValue()).startsWith("TERMS_OF_USE v");
        assertThat(typesOf(patient.id())).contains(AuditEventType.TERMS_ACCEPTED);
    }

    @Test
    void crmVerificationIsRecordedWithTheAdministratorAsActor() throws Exception {
        Account unverified = accounts.doctor("dra.ana@clinica.com", "16899535009", "Dra. Ana Lima", false);
        String adminToken = admin.token();
        UUID adminId = UUID.fromString(JsonPath.read(mockMvc.perform(get("/api/users/me")
                .header("Authorization", bearer(adminToken))).andReturn().getResponse().getContentAsString(), "$.id"));

        send("/api/admin/doctors/" + unverified.id() + "/verify-crm", adminToken, "{}").andExpect(status().isNoContent());

        AuditEvent event = latest(adminId, AuditEventType.CRM_VERIFIED);
        assertThat(event.getBeforeValue()).isEqualTo("false");
        assertThat(event.getAfterValue()).isEqualTo("true");
    }

    @Test
    void administratorRecordsAnErrataOnceAndTheOriginalIsKept() throws Exception {
        send("/api/patients/me/allergies", patient.token(), ALLERGY).andExpect(status().isCreated());
        AuditEvent original = latest(patient.id(), AuditEventType.ALLERGY_CREATED);
        String adminToken = admin.token();

        send("/api/admin/audit-events/" + original.getId() + "/errata", adminToken,
                "{\"justification\":\"Descricao digitada errada\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correctsEventId").value(original.getId().toString()));

        send("/api/admin/audit-events/" + original.getId() + "/errata", adminToken,
                "{\"justification\":\"Outra\"}").andExpect(status().isConflict());
        assertThat(events.findById(original.getId())).isPresent();
        assertThat(events.existsByCorrectsEventId(original.getId())).isTrue();
    }

    @Test
    void errataNeedsAJustificationAndTheAdministratorRole() throws Exception {
        send("/api/patients/me/allergies", patient.token(), ALLERGY).andExpect(status().isCreated());
        AuditEvent original = latest(patient.id(), AuditEventType.ALLERGY_CREATED);

        send("/api/admin/audit-events/" + original.getId() + "/errata", admin.token(), "{\"justification\":\" \"}")
                .andExpect(status().isBadRequest());
        send("/api/admin/audit-events/" + original.getId() + "/errata", patient.token(),
                "{\"justification\":\"Quero apagar\"}").andExpect(status().isForbidden());
        send("/api/admin/audit-events/" + UUID.randomUUID() + "/errata", admin.token(),
                "{\"justification\":\"Nao existe\"}").andExpect(status().isNotFound());
    }
}
