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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(ClearTemporaryStorage.class)
@AutoConfigureMockMvc
@Transactional
class RoutineRecordControllerTest {

    private static final String SLEEP = "{\"type\":\"SLEEP\",\"sleepQuality\":\"GOOD\",\"sleepHours\":7.5}";
    private static final String MOOD = "{\"type\":\"MOOD\",\"mood\":\"CALM\"}";

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
        return "/api/care/patients/" + of.id() + "/routine";
    }

    private ResultActions record(Account actor, Account target, String body) throws Exception {
        return mockMvc.perform(post(path(target)).header("Authorization", bearer(actor.token()))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions history(Account actor, Account target) throws Exception {
        return mockMvc.perform(get(path(target)).header("Authorization", bearer(actor.token())));
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

    @Test
    void patientRecordsAndReadsOwnRoutine() throws Exception {
        record(patient, patient, SLEEP)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("SLEEP"))
                .andExpect(jsonPath("$.sleepQuality").value("GOOD"))
                .andExpect(jsonPath("$.sleepHours").value(7.5))
                .andExpect(jsonPath("$.source").value("TYPED"))
                .andExpect(jsonPath("$.recordedBy").value(patient.id().toString()));

        history(patient, patient)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void everyRoutineTypeCanBeRecorded() throws Exception {
        record(patient, patient, MOOD).andExpect(status().isCreated());
        record(patient, patient, SLEEP).andExpect(status().isCreated());
        record(patient, patient, "{\"type\":\"MEAL\",\"mealPeriod\":\"LUNCH\",\"mealAccepted\":false}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.mealAccepted").value(false));
        record(patient, patient, "{\"type\":\"HYDRATION\",\"amountMl\":250}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.amountMl").value(250));
        record(patient, patient, "{\"type\":\"BATH\",\"bathCooperation\":\"PARTIAL\"}").andExpect(status().isCreated());
        record(patient, patient, "{\"type\":\"URINE\",\"urineFrequency\":4,\"urineAspect\":\"YELLOW\"}")
                .andExpect(status().isCreated());
        record(patient, patient, "{\"type\":\"STOOL\",\"stoolFrequency\":1,\"bristolScale\":4}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.bristolScale").value(4));

        history(patient, patient).andExpect(jsonPath("$.length()").value(7));
    }

    @Test
    void historyCanBeFilteredByType() throws Exception {
        record(patient, patient, MOOD).andExpect(status().isCreated());
        record(patient, patient, SLEEP).andExpect(status().isCreated());

        mockMvc.perform(get(path(patient)).param("type", "MOOD").header("Authorization", bearer(patient.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("MOOD"));
    }

    @Test
    void historyIsOrderedByWhenItHappenedNewestFirst() throws Exception {
        record(patient, patient, "{\"type\":\"HYDRATION\",\"amountMl\":100,\"occurredAt\":\"2026-01-01T08:00:00Z\"}")
                .andExpect(status().isCreated());
        record(patient, patient, "{\"type\":\"HYDRATION\",\"amountMl\":300,\"occurredAt\":\"2026-01-02T08:00:00Z\"}")
                .andExpect(status().isCreated());

        history(patient, patient)
                .andExpect(jsonPath("$[0].amountMl").value(300))
                .andExpect(jsonPath("$[1].amountMl").value(100));
    }

    @Test
    void occurrenceTimeInTheFutureIsRejected() throws Exception {
        record(patient, patient, "{\"type\":\"HYDRATION\",\"amountMl\":100,\"occurredAt\":\"2999-01-01T08:00:00Z\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void linkedCaregiverRecordsOnBehalfOfThePatient() throws Exception {
        linkCaregiver();

        record(caregiver, patient, MOOD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recordedBy").value(caregiver.id().toString()));

        history(patient, patient).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void caregiverReadingIsRegisteredInTheAccessLog() throws Exception {
        linkCaregiver();
        history(caregiver, patient).andExpect(status().isOk());

        mockMvc.perform(get("/api/patients/me/access-log").header("Authorization", bearer(patient.token())))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].area").value("ROUTINE"))
                .andExpect(jsonPath("$.items[0].accessorId").value(caregiver.id().toString()));
    }

    @Test
    void patientReadingOwnDataIsNotLogged() throws Exception {
        history(patient, patient).andExpect(status().isOk());

        mockMvc.perform(get("/api/patients/me/access-log").header("Authorization", bearer(patient.token())))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void unlinkedCaregiverIsDenied() throws Exception {
        record(caregiver, patient, MOOD).andExpect(status().isForbidden());
        history(caregiver, patient).andExpect(status().isForbidden());
    }

    @Test
    void patientCannotAccessAnotherPatient() throws Exception {
        Account other = accounts.patient("outro@exemplo.com", "39053344705");

        record(other, patient, MOOD).andExpect(status().isForbidden());
        history(other, patient).andExpect(status().isForbidden());
    }

    @Test
    void linkedDoctorReadsButCannotRecord() throws Exception {
        linkDoctor();
        record(patient, patient, MOOD).andExpect(status().isCreated());

        history(doctor, patient)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        record(doctor, patient, MOOD).andExpect(status().isForbidden());
    }

    @Test
    void unlinkedDoctorIsDenied() throws Exception {
        history(doctor, patient).andExpect(status().isForbidden());
    }

    @Test
    void typeIsRequired() throws Exception {
        record(patient, patient, "{\"mood\":\"CALM\"}").andExpect(status().isBadRequest());
    }

    @Test
    void missingFieldsOfTheTypeAreRejected() throws Exception {
        record(patient, patient, "{\"type\":\"MOOD\"}").andExpect(status().isBadRequest());
        record(patient, patient, "{\"type\":\"MEAL\",\"mealPeriod\":\"LUNCH\"}").andExpect(status().isBadRequest());
        record(patient, patient, "{\"type\":\"STOOL\",\"stoolFrequency\":1}").andExpect(status().isBadRequest());
    }

    @Test
    void impossibleValuesAreBlocked() throws Exception {
        record(patient, patient, "{\"type\":\"SLEEP\",\"sleepQuality\":\"GOOD\",\"sleepHours\":30}")
                .andExpect(status().isBadRequest());
        record(patient, patient, "{\"type\":\"HYDRATION\",\"amountMl\":-10}").andExpect(status().isBadRequest());
        record(patient, patient, "{\"type\":\"STOOL\",\"stoolFrequency\":1,\"bristolScale\":9}")
                .andExpect(status().isBadRequest());

        history(patient, patient).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void ocrOriginIsFlaggedToCheckAgainstTheOriginal() throws Exception {
        record(patient, patient, "{\"type\":\"HYDRATION\",\"amountMl\":200,\"source\":\"OCR\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("OCR"))
                .andExpect(jsonPath("$.checkAgainstOriginal").value(true));
    }

    @Test
    void anonymousRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get(path(patient))).andExpect(status().isUnauthorized());
    }
}
