package com.mentesvivas.backend.controller.patient;

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
class PatientCareTeamTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private LinkTestSupport accounts;

    private Account patient;
    private Account caregiver;
    private Account doctor;

    @BeforeEach
    void setUp() throws Exception {
        patient = accounts.patient("paciente@exemplo.com", "11144477735");
        caregiver = accounts.caregiver("cuidador@exemplo.com", "52998224725");
        doctor = accounts.doctor("dr.paulo@clinica.com", "12345678909", "Dr. Paulo Lima", true);
    }

    private String linkId(String path, Account account) throws Exception {
        String body = mockMvc.perform(get(path).header("Authorization", bearer(account.token())))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$[0].id");
    }

    private void invite(String path, Account sender) throws Exception {
        mockMvc.perform(post(path).header("Authorization", bearer(sender.token()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"paciente@exemplo.com\"}"))
                .andExpect(status().isAccepted());
    }

    private void profileCareTeamIsEmpty() throws Exception {
        mockMvc.perform(get("/api/patients/me/profile").header("Authorization", bearer(patient.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.careTeam.doctors.length()").value(0))
                .andExpect(jsonPath("$.careTeam.caregivers.length()").value(0));
    }

    @Test
    void profileStartsWithAnEmptyCareTeam() throws Exception {
        profileCareTeamIsEmpty();
    }

    @Test
    void pendingInvitationsDoNotAppearInTheCareTeam() throws Exception {
        invite("/api/caregiver-links/invitations", caregiver);
        invite("/api/doctor-links/invitations", doctor);

        profileCareTeamIsEmpty();
    }

    @Test
    void acceptedCaregiverAndDoctorAppearInTheProfile() throws Exception {
        invite("/api/caregiver-links/invitations", caregiver);
        mockMvc.perform(post("/api/caregiver-links/" + linkId("/api/caregiver-links", patient) + "/accept")
                .header("Authorization", bearer(patient.token()))).andExpect(status().isOk());
        invite("/api/doctor-links/invitations", doctor);
        mockMvc.perform(post("/api/doctor-links/" + linkId("/api/doctor-links", patient) + "/accept")
                .header("Authorization", bearer(patient.token()))).andExpect(status().isOk());

        mockMvc.perform(get("/api/patients/me/profile").header("Authorization", bearer(patient.token())))
                .andExpect(jsonPath("$.careTeam.doctors.length()").value(1))
                .andExpect(jsonPath("$.careTeam.doctors[0].id").value(doctor.id().toString()))
                .andExpect(jsonPath("$.careTeam.doctors[0].crm").value("123456"))
                .andExpect(jsonPath("$.careTeam.caregivers.length()").value(1))
                .andExpect(jsonPath("$.careTeam.caregivers[0].id").value(caregiver.id().toString()));
    }

    @Test
    void endedLinkLeavesTheCareTeam() throws Exception {
        invite("/api/caregiver-links/invitations", caregiver);
        String id = linkId("/api/caregiver-links", patient);
        mockMvc.perform(post("/api/caregiver-links/" + id + "/accept").header("Authorization", bearer(patient.token())))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/caregiver-links/" + id + "/end").header("Authorization", bearer(patient.token())))
                .andExpect(status().isOk());

        profileCareTeamIsEmpty();
    }
}
