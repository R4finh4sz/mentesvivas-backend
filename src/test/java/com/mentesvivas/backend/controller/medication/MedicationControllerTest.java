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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(ClearTemporaryStorage.class)
@AutoConfigureMockMvc
@Transactional
class MedicationControllerTest {

    private static final String LOSARTAN = "{\"name\":\"Losartana\",\"strength\":\"50 mg\",\"unitsPerDose\":1,"
            + "\"times\":[\"20:00\",\"08:00\"],\"stockQuantity\":30}";

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

    private String path() {
        return "/api/care/patients/" + patient.id() + "/medications";
    }

    private ResultActions create(Account actor, String body) throws Exception {
        return mockMvc.perform(post(path()).header("Authorization", bearer(actor.token()))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions list(Account actor) throws Exception {
        return mockMvc.perform(get(path()).header("Authorization", bearer(actor.token())));
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

    private void linkCaregiver() throws Exception {
        accept("/api/caregiver-links/invitations", "/api/caregiver-links", caregiver);
    }

    private void linkDoctor() throws Exception {
        accept("/api/doctor-links/invitations", "/api/doctor-links", doctor);
    }

    private void declareAllergy(String name) throws Exception {
        mockMvc.perform(post("/api/patients/me/allergies").header("Authorization", bearer(patient.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"category\":\"MEDICATION\",\"description\":\"" + name + "\",\"severity\":\"SEVERE\"}"))
                .andExpect(status().isCreated());
    }

    private String createAndGetId(String body) throws Exception {
        String response = create(patient, body).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    @Test
    void patientRegistersAMedicationWithStockProjection() throws Exception {
        create(patient, LOSARTAN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Losartana"))
                .andExpect(jsonPath("$.origin").value("MANUAL"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.times[0]").value("08:00:00"))
                .andExpect(jsonPath("$.times[1]").value("20:00:00"))
                .andExpect(jsonPath("$.stockDaysLeft").value(15))
                .andExpect(jsonPath("$.stockCritical").value(false))
                .andExpect(jsonPath("$.createdBy").value(patient.id().toString()))
                .andExpect(jsonPath("$.warnings[0]").value("ALLERGY_NOT_INFORMED"));

        list(patient).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].warnings.length()").value(0));
    }

    @Test
    void lowStockIsFlaggedAsCritical() throws Exception {
        create(patient, "{\"name\":\"Donepezila\",\"unitsPerDose\":1,\"times\":[\"21:00\"],\"stockQuantity\":4}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockDaysLeft").value(4))
                .andExpect(jsonPath("$.stockCritical").value(true));
    }

    @Test
    void linkedCaregiverManagesTheMedicationOfThePatient() throws Exception {
        linkCaregiver();

        create(caregiver, LOSARTAN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdBy").value(caregiver.id().toString()));
        list(patient).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void allergyMatchAsksForConfirmationAndThenSaves() throws Exception {
        declareAllergy("Dipirona");
        String body = "{\"name\":\"Dipirona\",\"unitsPerDose\":1,\"times\":[\"08:00\"],\"stockQuantity\":10";

        create(patient, body + "}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.requiresConfirmation").value(true));
        list(patient).andExpect(jsonPath("$.length()").value(0));

        create(patient, body + ",\"confirmAllergyRisk\":true}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warnings[0]").value("ALLERGY_MATCH_CONFIRMED"));
    }

    @Test
    void duplicateActiveMedicationIsRejected() throws Exception {
        create(patient, LOSARTAN).andExpect(status().isCreated());

        create(patient, LOSARTAN).andExpect(status().isConflict());
    }

    @Test
    void deactivatedMedicationLeavesTheListButKeepsTheRecord() throws Exception {
        String id = createAndGetId(LOSARTAN);

        mockMvc.perform(post(path() + "/" + id + "/deactivate").header("Authorization", bearer(patient.token())))
                .andExpect(status().isNoContent());

        list(patient).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get(path()).param("includeInactive", "true")
                .header("Authorization", bearer(patient.token())))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].active").value(false));
        create(patient, LOSARTAN).andExpect(status().isCreated());
    }

    @Test
    void stockAdjustmentNeedsAJustification() throws Exception {
        String id = createAndGetId(LOSARTAN);

        mockMvc.perform(put(path() + "/" + id + "/stock").header("Authorization", bearer(patient.token()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":10}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put(path() + "/" + id + "/stock").header("Authorization", bearer(patient.token()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":10,\"justification\":\"  \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put(path() + "/" + id + "/stock").header("Authorization", bearer(patient.token()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":10,\"justification\":\"contagem\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(10))
                .andExpect(jsonPath("$.stockDaysLeft").value(5))
                .andExpect(jsonPath("$.stockCritical").value(true));
    }

    @Test
    void unknownMedicationIsNotFound() throws Exception {
        mockMvc.perform(put(path() + "/" + java.util.UUID.randomUUID() + "/stock")
                .header("Authorization", bearer(patient.token())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":10,\"justification\":\"contagem\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidRequestsAreRejected() throws Exception {
        create(patient, "{\"unitsPerDose\":1,\"times\":[\"08:00\"],\"stockQuantity\":1}")
                .andExpect(status().isBadRequest());
        create(patient, "{\"name\":\"X\",\"unitsPerDose\":0,\"times\":[\"08:00\"],\"stockQuantity\":1}")
                .andExpect(status().isBadRequest());
        create(patient, "{\"name\":\"X\",\"unitsPerDose\":1,\"times\":[],\"stockQuantity\":1}")
                .andExpect(status().isBadRequest());
        create(patient, "{\"name\":\"X\",\"unitsPerDose\":1,\"times\":[\"08:00\"],\"stockQuantity\":-1}")
                .andExpect(status().isBadRequest());
        create(patient, "{\"name\":\"X\",\"unitsPerDose\":1,\"times\":[\"08:00\"]}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void caregiverReadingIsRegisteredInTheAccessLog() throws Exception {
        linkCaregiver();
        list(caregiver).andExpect(status().isOk());

        mockMvc.perform(get("/api/patients/me/access-log").header("Authorization", bearer(patient.token())))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].area").value("MEDICATIONS"));
    }

    @Test
    void linkedDoctorReadsButCannotRegister() throws Exception {
        linkDoctor();
        create(patient, LOSARTAN).andExpect(status().isCreated());

        list(doctor).andExpect(jsonPath("$.length()").value(1));
        create(doctor, "{\"name\":\"Outro\",\"unitsPerDose\":1,\"times\":[\"08:00\"],\"stockQuantity\":1}")
                .andExpect(status().isForbidden());
    }

    @Test
    void unlinkedProfilesAreDenied() throws Exception {
        create(caregiver, LOSARTAN).andExpect(status().isForbidden());
        list(caregiver).andExpect(status().isForbidden());
        list(doctor).andExpect(status().isForbidden());
        Account other = accounts.patient("outro@exemplo.com", "39053344705");
        list(other).andExpect(status().isForbidden());
    }

    @Test
    void medicationAppearsInTheDoctorClinicalSummary() throws Exception {
        linkDoctor();
        create(patient, LOSARTAN).andExpect(status().isCreated());

        mockMvc.perform(get("/api/doctors/me/patients/" + patient.id() + "/summary")
                .header("Authorization", bearer(doctor.token())))
                .andExpect(jsonPath("$.medications.length()").value(1))
                .andExpect(jsonPath("$.medications[0].name").value("Losartana"));
    }

    @Test
    void anonymousRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get(path())).andExpect(status().isUnauthorized());
    }
}
