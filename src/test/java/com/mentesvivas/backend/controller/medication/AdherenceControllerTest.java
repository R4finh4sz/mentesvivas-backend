package com.mentesvivas.backend.controller.medication;

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

import java.time.Instant;

import static com.mentesvivas.backend.support.AuthTestSupport.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(ClearTemporaryStorage.class)
@AutoConfigureMockMvc
@Transactional
class AdherenceControllerTest {

    private static final String LOSARTAN = "{\"name\":\"Losartana\",\"unitsPerDose\":1,"
            + "\"times\":[\"08:00\"],\"stockQuantity\":10}";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private LinkTestSupport accounts;

    private Account patient;
    private Account doctor;
    private Account stranger;

    @BeforeEach
    void setUp() throws Exception {
        patient = accounts.patient("paciente@exemplo.com", "11144477735");
        doctor = accounts.doctor("dr.paulo@clinica.com", "12345678909", "Dr. Paulo Lima", true);
        stranger = accounts.caregiver("outro@exemplo.com", "39053344705");
    }

    private String base() {
        return "/api/care/patients/" + patient.id();
    }

    private ResultActions adherence(Account actor) throws Exception {
        return mockMvc.perform(get(base() + "/adherence").header("Authorization", bearer(actor.token())));
    }

    private void takeDose() throws Exception {
        String response = mockMvc.perform(post(base() + "/medications")
                .header("Authorization", bearer(patient.token())).contentType(MediaType.APPLICATION_JSON)
                .content(LOSARTAN)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String medicationId = JsonPath.read(response, "$.id");
        mockMvc.perform(post(base() + "/medications/" + medicationId + "/doses")
                .header("Authorization", bearer(patient.token())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"scheduledFor\":\"" + Instant.now().minusSeconds(3600) + "\",\"status\":\"TAKEN\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void withoutMedicationsThereIsNoPercentageNorAlert() throws Exception {
        adherence(patient)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expectedDoses").value(0))
                .andExpect(jsonPath("$.adherencePercent").doesNotExist())
                .andExpect(jsonPath("$.thresholdPercent").value(70))
                .andExpect(jsonPath("$.belowThreshold").value(false));
    }

    @Test
    void countsTheTakenDosesOfActiveMedications() throws Exception {
        takeDose();

        adherence(patient).andExpect(status().isOk()).andExpect(jsonPath("$.taken").value(1));
    }

    @Test
    void doctorWithoutLinkIsForbiddenAndStrangerToo() throws Exception {
        adherence(doctor).andExpect(status().isForbidden());
        adherence(stranger).andExpect(status().isForbidden());
    }

    @Test
    void linkedDoctorReadsTheAdherence() throws Exception {
        mockMvc.perform(post("/api/doctor-links/invitations").header("Authorization", bearer(doctor.token()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"paciente@exemplo.com\"}"))
                .andExpect(status().isAccepted());
        String links = mockMvc.perform(get("/api/doctor-links").header("Authorization", bearer(patient.token())))
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(post("/api/doctor-links/" + JsonPath.read(links, "$[0].id") + "/accept")
                .header("Authorization", bearer(patient.token()))).andExpect(status().isOk());

        adherence(doctor).andExpect(status().isOk()).andExpect(jsonPath("$.belowThreshold").value(false));
    }
}
