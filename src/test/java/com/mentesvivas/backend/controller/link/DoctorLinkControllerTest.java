package com.mentesvivas.backend.controller.link;

import com.jayway.jsonpath.JsonPath;
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

import static com.mentesvivas.backend.support.AuthTestSupport.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(ClearTemporaryStorage.class)
@AutoConfigureMockMvc
@Transactional
class DoctorLinkControllerTest {

    private static final String BASE = "/api/doctor-links";
    private static final String ALLERGY = "{\"category\":\"MEDICATION\",\"description\":\"Dipirona\",\"severity\":\"SEVERE\"}";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private LinkTestSupport accounts;
    @Autowired
    private CapturedEmailSender emails;

    private Account doctor;
    private Account patient;
    private Account caregiver;

    @BeforeEach
    void setUp() throws Exception {
        patient = accounts.patient("paciente@exemplo.com", "11144477735");
        caregiver = accounts.caregiver("cuidador@exemplo.com", "52998224725");
        doctor = accounts.doctor("dr.paulo@clinica.com", "12345678909", "Dr. Paulo Lima", true);
        emails.clear();
    }

    @Test
    void directoryListsOnlyVerifiedDoctorsAndNeedsAtLeastThreeCharacters() throws Exception {
        accounts.doctor("dr.ana@clinica.com", "16899535009", "Dra. Ana Lima", false);

        mockMvc.perform(get("/api/doctor-directory").param("q", "lima").header("Authorization", bearer(patient.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(doctor.id().toString()))
                .andExpect(jsonPath("$[0].crm").value("123456"));
        mockMvc.perform(get("/api/doctor-directory").param("q", "li").header("Authorization", bearer(patient.token())))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/doctor-directory").param("q", "lima").header("Authorization", bearer(caregiver.token())))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/doctor-directory").param("q", "lima").header("Authorization", bearer(doctor.token())))
                .andExpect(status().isForbidden());
    }

    @Test
    void patientRequestNeedsTheDoctorConfirmationAndGivesAccessOnlyAfterIt() throws Exception {
        String body = request(patient, doctor, null).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.patientAuthorized").value(true))
                .andExpect(jsonPath("$.doctorConfirmed").value(false))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(doctor.token())))
                .andExpect(jsonPath("$[0].type").value("DOCTOR_LINK_REQUEST"));
        allergy(doctor, patient).andExpect(status().isForbidden());

        act(doctor, id, "accept").andExpect(jsonPath("$.status").value("ACTIVE"));

        allergy(doctor, patient).andExpect(status().isCreated());
        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(patient.token())))
                .andExpect(jsonPath("$[0].type").value("LINK_ACCEPTED"));
    }

    @Test
    void caregiverRequestNeedsBothThePatientAuthorizationAndTheDoctorConfirmation() throws Exception {
        Account other = accounts.patient("outro@exemplo.com", "39053344705");
        request(caregiver, doctor, patient).andExpect(status().isForbidden());

        mockMvc.perform(post("/api/caregiver-links/invitations").header("Authorization", bearer(caregiver.token()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"paciente@exemplo.com\"}"));
        String careId = JsonPath.read(mockMvc.perform(get("/api/caregiver-links")
                .header("Authorization", bearer(patient.token()))).andReturn().getResponse().getContentAsString(), "$[0].id");
        mockMvc.perform(post("/api/caregiver-links/" + careId + "/accept").header("Authorization", bearer(patient.token())))
                .andExpect(status().isOk());

        request(caregiver, doctor, other).andExpect(status().isForbidden());
        String body = request(caregiver, doctor, patient).andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientAuthorized").value(false))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(patient.token())))
                .andExpect(jsonPath("$[0].type").value("DOCTOR_LINK_REQUEST"));
        act(doctor, id, "accept").andExpect(jsonPath("$.status").value("PENDING"));
        allergy(doctor, patient).andExpect(status().isForbidden());
        act(caregiver, id, "accept").andExpect(status().isForbidden());

        act(patient, id, "accept").andExpect(jsonPath("$.status").value("ACTIVE"));
        allergy(doctor, patient).andExpect(status().isCreated());
    }

    @Test
    void doctorInvitationIsAcceptedByThePatient() throws Exception {
        invite(doctor, "paciente@exemplo.com").andExpect(status().isAccepted());
        String id = JsonPath.read(mockMvc.perform(get(BASE).header("Authorization", bearer(patient.token())))
                .andReturn().getResponse().getContentAsString(), "$[0].id");

        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(patient.token())))
                .andExpect(jsonPath("$[0].type").value("DOCTOR_INVITATION"));
        allergy(doctor, patient).andExpect(status().isForbidden());

        act(patient, id, "accept").andExpect(jsonPath("$.status").value("ACTIVE"));
        allergy(doctor, patient).andExpect(status().isCreated());
    }

    @Test
    void invitationToAnUnregisteredEmailBecomesAPendingInvitationAfterConfirmation() throws Exception {
        invite(doctor, "novo@exemplo.com").andExpect(status().isAccepted());
        Account novo = accounts.patient("novo@exemplo.com", "39053344705");
        accounts.confirmEmail("novo@exemplo.com");

        mockMvc.perform(get(BASE).header("Authorization", bearer(novo.token())))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].doctorConfirmed").value(true))
                .andExpect(jsonPath("$[0].doctor.id").value(doctor.id().toString()));
    }

    @Test
    void doctorsWithoutVerifiedCrmCannotInviteAndCannotBeRequested() throws Exception {
        Account unverified = accounts.doctor("dr.ana@clinica.com", "16899535009", "Dra. Ana", false);

        invite(unverified, "paciente@exemplo.com").andExpect(status().isForbidden());
        request(patient, unverified, null).andExpect(status().isNotFound());
        invite(patient, "outro@exemplo.com").andExpect(status().isForbidden());
    }

    @Test
    void duplicateOpenRequestIsAConflictAndDeclinedOnesCanBeRequestedAgain() throws Exception {
        String id = JsonPath.read(request(patient, doctor, null).andReturn().getResponse().getContentAsString(), "$.id");
        request(patient, doctor, null).andExpect(status().isConflict());

        act(doctor, id, "decline").andExpect(jsonPath("$.status").value("DECLINED"));
        act(doctor, id, "accept").andExpect(status().isConflict());
        allergy(doctor, patient).andExpect(status().isForbidden());

        request(patient, doctor, null).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void endingTheLinkStopsTheAccessAndKeepsTheHistory() throws Exception {
        String id = JsonPath.read(request(patient, doctor, null).andReturn().getResponse().getContentAsString(), "$.id");
        act(doctor, id, "accept");
        allergy(doctor, patient).andExpect(status().isCreated());

        act(patient, id, "end").andExpect(jsonPath("$.status").value("ENDED"));

        allergy(doctor, patient).andExpect(status().isForbidden());
        mockMvc.perform(get(BASE).header("Authorization", bearer(doctor.token())))
                .andExpect(jsonPath("$[0].status").value("ENDED"));
        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(doctor.token())))
                .andExpect(jsonPath("$[0].type").value("LINK_ENDED"));
    }

    @Test
    void outsidersCannotAnswerOrEndALink() throws Exception {
        Account other = accounts.patient("outro@exemplo.com", "39053344705");
        String id = JsonPath.read(request(patient, doctor, null).andReturn().getResponse().getContentAsString(), "$.id");

        act(other, id, "accept").andExpect(status().isForbidden());
        act(other, id, "end").andExpect(status().isForbidden());
        act(caregiver, id, "decline").andExpect(status().isForbidden());
    }

    @Test
    void doctorSeesOnlyHisOwnLinksAndPatientsSeeTheirs() throws Exception {
        Account anotherDoctor = accounts.doctor("dr.ana@clinica.com", "16899535009", "Dra. Ana", true);
        request(patient, doctor, null);
        request(patient, anotherDoctor, null);

        mockMvc.perform(get(BASE).header("Authorization", bearer(doctor.token())))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get(BASE).header("Authorization", bearer(patient.token())))
                .andExpect(jsonPath("$.length()").value(2));
    }

    private ResultActions request(Account caller, Account doctorAccount, Account patientAccount) throws Exception {
        String patientId = patientAccount == null ? "" : ",\"patientId\":\"%s\"".formatted(patientAccount.id());
        return mockMvc.perform(post(BASE + "/requests").header("Authorization", bearer(caller.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"doctorId\":\"%s\"%s}".formatted(doctorAccount.id(), patientId)));
    }

    private ResultActions invite(Account who, String email) throws Exception {
        return mockMvc.perform(post(BASE + "/invitations").header("Authorization", bearer(who.token()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"%s\"}".formatted(email)));
    }

    private ResultActions act(Account who, String id, String action) throws Exception {
        return mockMvc.perform(post(BASE + "/" + id + "/" + action).header("Authorization", bearer(who.token())));
    }

    private ResultActions allergy(Account doctorAccount, Account patientAccount) throws Exception {
        return mockMvc.perform(post("/api/doctors/me/patients/" + patientAccount.id() + "/allergies")
                .header("Authorization", bearer(doctorAccount.token()))
                .contentType(MediaType.APPLICATION_JSON).content(ALLERGY));
    }
}
