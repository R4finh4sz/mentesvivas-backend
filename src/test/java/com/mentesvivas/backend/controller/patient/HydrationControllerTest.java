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
class HydrationControllerTest {

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

    private String path(Account of) {
        return "/api/care/patients/" + of.id() + "/hydration";
    }

    private ResultActions summary(Account actor) throws Exception {
        return mockMvc.perform(get(path(patient)).header("Authorization", bearer(actor.token())));
    }

    private void drink(int ml) throws Exception {
        mockMvc.perform(post("/api/care/patients/" + patient.id() + "/routine")
                .header("Authorization", bearer(patient.token())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"HYDRATION\",\"amountMl\":" + ml + "}")).andExpect(status().isCreated());
    }

    private void weigh(double kg) throws Exception {
        mockMvc.perform(post("/api/patients/me/measurements").header("Authorization", bearer(patient.token()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"weightKg\":" + kg + "}"))
                .andExpect(status().isCreated());
    }

    private void declareDisease(String disease) throws Exception {
        mockMvc.perform(put("/api/patients/me/profile").header("Authorization", bearer(patient.token()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"declaredDisease\":\"" + disease + "\"}"))
                .andExpect(status().isOk());
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

    private ResultActions defineGoal(Account actor, String body) throws Exception {
        return mockMvc.perform(put("/api/doctors/me/patients/" + patient.id() + "/hydration-goal")
                .header("Authorization", bearer(actor.token())).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void withoutWeightThereIsNoGoal() throws Exception {
        summary(patient)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goalStatus").value("WEIGHT_MISSING"))
                .andExpect(jsonPath("$.goalMinMl").doesNotExist())
                .andExpect(jsonPath("$.consumedTodayMl").value(0));
    }

    @Test
    void goalComesFromTheWeightAndTodaysIntakeIsSummed() throws Exception {
        weigh(80);
        drink(250);
        drink(300);

        summary(patient)
                .andExpect(jsonPath("$.goalStatus").value("FORMULA"))
                .andExpect(jsonPath("$.weightKg").value(80.0))
                .andExpect(jsonPath("$.goalMinMl").value(2000))
                .andExpect(jsonPath("$.goalMaxMl").value(2400))
                .andExpect(jsonPath("$.consumedTodayMl").value(550))
                .andExpect(jsonPath("$.remainingMinMl").value(1450))
                .andExpect(jsonPath("$.remainingMaxMl").value(1850))
                .andExpect(jsonPath("$.consultDoctorAboutGoal").value(false));
    }

    @Test
    void intakeFromAnotherDayIsNotCounted() throws Exception {
        weigh(80);
        mockMvc.perform(post("/api/care/patients/" + patient.id() + "/routine")
                .header("Authorization", bearer(patient.token())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"HYDRATION\",\"amountMl\":900,\"occurredAt\":\"2026-01-01T12:00:00Z\"}"))
                .andExpect(status().isCreated());

        summary(patient).andExpect(jsonPath("$.consumedTodayMl").value(0));
    }

    @Test
    void hypertensionAndDiabetesKeepTheGoalWithAWarning() throws Exception {
        weigh(80);
        declareDisease("Hipertensão");

        summary(patient)
                .andExpect(jsonPath("$.goalStatus").value("FORMULA"))
                .andExpect(jsonPath("$.consultDoctorAboutGoal").value(true))
                .andExpect(jsonPath("$.conditionValidated").value(false));
    }

    @Test
    void renalOrCardiacConditionInhibitsTheFormula() throws Exception {
        weigh(80);
        declareDisease("Insuficiência renal");
        drink(200);

        summary(patient)
                .andExpect(jsonPath("$.goalStatus").value("DOCTOR_GOAL_REQUIRED"))
                .andExpect(jsonPath("$.goalMinMl").doesNotExist())
                .andExpect(jsonPath("$.consumedTodayMl").value(200));
    }

    @Test
    void linkedDoctorDefinesTheGoalAndItReplacesTheFormula() throws Exception {
        weigh(80);
        declareDisease("Insuficiência cardíaca");
        linkDoctor();

        defineGoal(doctor, "{\"goalMl\":1500}").andExpect(status().isNoContent());
        drink(500);

        summary(patient)
                .andExpect(jsonPath("$.goalStatus").value("DOCTOR_DEFINED"))
                .andExpect(jsonPath("$.goalMinMl").value(1500))
                .andExpect(jsonPath("$.goalMaxMl").value(1500))
                .andExpect(jsonPath("$.remainingMinMl").value(1000));
        summary(doctor).andExpect(jsonPath("$.goalMinMl").value(1500));

        mockMvc.perform(get("/api/patients/me/changes").header("Authorization", bearer(patient.token())))
                .andExpect(jsonPath("$[0].field").value("HYDRATION_GOAL"))
                .andExpect(jsonPath("$[0].changedBy").value(doctor.id().toString()));
    }

    @Test
    void invalidDoctorGoalIsRejected() throws Exception {
        linkDoctor();

        defineGoal(doctor, "{}").andExpect(status().isBadRequest());
        defineGoal(doctor, "{\"goalMl\":50}").andExpect(status().isBadRequest());
        defineGoal(doctor, "{\"goalMl\":20000}").andExpect(status().isBadRequest());
    }

    @Test
    void onlyTheLinkedDoctorCanDefineTheGoal() throws Exception {
        defineGoal(doctor, "{\"goalMl\":1500}").andExpect(status().isForbidden());
        defineGoal(patient, "{\"goalMl\":1500}").andExpect(status().isForbidden());
        defineGoal(caregiver, "{\"goalMl\":1500}").andExpect(status().isForbidden());
    }

    @Test
    void unlinkedCaregiverIsDenied() throws Exception {
        summary(caregiver).andExpect(status().isForbidden());
    }

    @Test
    void patientCannotReadAnotherPatient() throws Exception {
        Account other = accounts.patient("outro@exemplo.com", "39053344705");

        summary(other).andExpect(status().isForbidden());
    }

    @Test
    void anonymousRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get(path(patient))).andExpect(status().isUnauthorized());
    }
}
