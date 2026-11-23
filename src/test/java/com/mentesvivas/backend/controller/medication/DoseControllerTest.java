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

import static com.mentesvivas.backend.support.AuthTestSupport.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(ClearTemporaryStorage.class)
@AutoConfigureMockMvc
@Transactional
class DoseControllerTest {

    private static final String LOSARTAN = "{\"name\":\"Losartana\",\"strength\":\"50 mg\",\"unitsPerDose\":1,"
            + "\"times\":[\"08:00\"],\"stockQuantity\":10}";
    private static final String SLOT = "2026-11-23T08:00:00Z";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private LinkTestSupport accounts;

    private Account patient;
    private Account caregiver;
    private Account doctor;
    private Account stranger;

    @BeforeEach
    void setUp() throws Exception {
        patient = accounts.patient("paciente@exemplo.com", "11144477735");
        caregiver = accounts.caregiver("cuidador@exemplo.com", "52998224725");
        doctor = accounts.doctor("dr.paulo@clinica.com", "12345678909", "Dr. Paulo Lima", true);
        stranger = accounts.caregiver("outro@exemplo.com", "39053344705");
    }

    private String base() {
        return "/api/care/patients/" + patient.id();
    }

    private String createMedication() throws Exception {
        String response = mockMvc.perform(post(base() + "/medications")
                .header("Authorization", bearer(patient.token())).contentType(MediaType.APPLICATION_JSON)
                .content(LOSARTAN)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private ResultActions record(Account actor, String medicationId, String status) throws Exception {
        return mockMvc.perform(post(base() + "/medications/" + medicationId + "/doses")
                .header("Authorization", bearer(actor.token())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"scheduledFor\":\"" + SLOT + "\",\"status\":\"" + status + "\"}"));
    }

    private ResultActions correct(Account actor, String doseId, String body) throws Exception {
        return mockMvc.perform(post(base() + "/doses/" + doseId + "/corrections")
                .header("Authorization", bearer(actor.token())).contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions doses(Account actor) throws Exception {
        return mockMvc.perform(get(base() + "/doses").param("from", "2026-11-23T00:00:00Z")
                .param("to", "2026-11-24T00:00:00Z").header("Authorization", bearer(actor.token())));
    }

    private ResultActions medications(Account actor) throws Exception {
        return mockMvc.perform(get(base() + "/medications").header("Authorization", bearer(actor.token())));
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

    @Test
    void takenDoseIsRecordedAndDiscountsTheStock() throws Exception {
        String medicationId = createMedication();

        record(patient, medicationId, "TAKEN")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("TAKEN"))
                .andExpect(jsonPath("$.duplicate").value(false))
                .andExpect(jsonPath("$.corrected").value(false))
                .andExpect(jsonPath("$.recordedBy").value(patient.id().toString()));

        medications(patient).andExpect(jsonPath("$[0].stockQuantity").value(9));
        doses(patient).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void refusedDoseKeepsTheStock() throws Exception {
        String medicationId = createMedication();

        record(patient, medicationId, "REFUSED").andExpect(status().isCreated());

        medications(patient).andExpect(jsonPath("$[0].stockQuantity").value(10));
    }

    @Test
    void confirmingTwiceInARowIsBlockedAsDoubleClick() throws Exception {
        String medicationId = createMedication();
        record(patient, medicationId, "TAKEN").andExpect(status().isCreated());

        record(patient, medicationId, "TAKEN").andExpect(status().isConflict());

        medications(patient).andExpect(jsonPath("$[0].stockQuantity").value(9));
        doses(patient).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void caregiverAndPatientConfirmingTheSameDoseKeepOneDiscount() throws Exception {
        linkCaregiver();
        String medicationId = createMedication();
        record(patient, medicationId, "TAKEN").andExpect(status().isCreated());

        record(caregiver, medicationId, "TAKEN")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.duplicate").value(true))
                .andExpect(jsonPath("$.recordedBy").value(caregiver.id().toString()));

        medications(patient).andExpect(jsonPath("$[0].stockQuantity").value(9));
        doses(patient).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void correctionNeedsAJustificationAndRestoresTheStock() throws Exception {
        String medicationId = createMedication();
        String response = record(patient, medicationId, "TAKEN").andReturn().getResponse().getContentAsString();
        String doseId = JsonPath.read(response, "$.id");

        correct(patient, doseId, "{\"status\":\"REFUSED\"}").andExpect(status().isBadRequest());
        correct(patient, doseId, "{\"status\":\"REFUSED\",\"justification\":\"  \"}")
                .andExpect(status().isBadRequest());
        correct(patient, doseId, "{\"status\":\"REFUSED\",\"justification\":\"marcada por engano\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUSED"))
                .andExpect(jsonPath("$.corrected").value(true));

        medications(patient).andExpect(jsonPath("$[0].stockQuantity").value(10));
        correct(patient, doseId, "{\"status\":\"REFUSED\",\"justification\":\"de novo\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void doctorReadsTheDosesButCannotRecordThem() throws Exception {
        String medicationId = createMedication();
        record(patient, medicationId, "TAKEN").andExpect(status().isCreated());
        mockMvc.perform(post("/api/doctor-links/invitations").header("Authorization", bearer(doctor.token()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"paciente@exemplo.com\"}"))
                .andExpect(status().isAccepted());
        String links = mockMvc.perform(get("/api/doctor-links").header("Authorization", bearer(patient.token())))
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(post("/api/doctor-links/" + JsonPath.read(links, "$[0].id") + "/accept")
                .header("Authorization", bearer(patient.token()))).andExpect(status().isOk());

        doses(doctor).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        record(doctor, medicationId, "TAKEN").andExpect(status().isForbidden());
    }

    @Test
    void unlinkedCaregiverHasNoAccess() throws Exception {
        String medicationId = createMedication();

        record(stranger, medicationId, "TAKEN").andExpect(status().isForbidden());
        doses(stranger).andExpect(status().isForbidden());
    }

    @Test
    void unknownMedicationAndInvalidRequestsAreRejected() throws Exception {
        record(patient, java.util.UUID.randomUUID().toString(), "TAKEN").andExpect(status().isNotFound());
        String medicationId = createMedication();
        mockMvc.perform(post(base() + "/medications/" + medicationId + "/doses")
                .header("Authorization", bearer(patient.token())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"TAKEN\"}")).andExpect(status().isBadRequest());
        record(patient, medicationId, "LATE").andExpect(status().isBadRequest());
    }

    @Test
    void deactivatedMedicationRejectsDoses() throws Exception {
        String medicationId = createMedication();
        mockMvc.perform(post(base() + "/medications/" + medicationId + "/deactivate")
                .header("Authorization", bearer(patient.token()))).andExpect(status().isNoContent());

        record(patient, medicationId, "TAKEN").andExpect(status().isConflict());
    }
}
