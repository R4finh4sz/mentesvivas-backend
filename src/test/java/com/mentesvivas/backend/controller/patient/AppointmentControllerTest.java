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

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static com.mentesvivas.backend.support.AuthTestSupport.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(ClearTemporaryStorage.class)
@AutoConfigureMockMvc
@Transactional
class AppointmentControllerTest {

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
        return "/api/care/patients/" + of.id() + "/appointments";
    }

    private String body(String type, String title, Instant when) {
        return "{\"type\":\"" + type + "\",\"title\":\"" + title + "\",\"location\":\"Clinica Central\","
                + "\"scheduledAt\":\"" + when + "\"}";
    }

    private Instant inDays(long days) {
        return Instant.now().plus(days, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
    }

    private ResultActions create(Account actor, Account target, String body) throws Exception {
        return mockMvc.perform(post(path(target)).header("Authorization", bearer(actor.token()))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions list(Account actor, Account target) throws Exception {
        return mockMvc.perform(get(path(target)).header("Authorization", bearer(actor.token())));
    }

    private String createdId(Account actor, Account target, String body) throws Exception {
        String response = create(actor, target, body).andExpect(status().isCreated()).andReturn().getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.id");
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
    void linkedCaregiverSchedulesAConsultationWithTheReminder() throws Exception {
        linkCaregiver();
        Instant when = inDays(2);

        create(caregiver, patient, body("CONSULTATION", "Cardiologista", when))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("CONSULTATION"))
                .andExpect(jsonPath("$.title").value("Cardiologista"))
                .andExpect(jsonPath("$.location").value("Clinica Central"))
                .andExpect(jsonPath("$.remindAt").value(when.minus(1, ChronoUnit.HOURS).toString()))
                .andExpect(jsonPath("$.createdBy").value(caregiver.id().toString()));

        list(patient, patient).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void listIsOrderedByDateAndSkipsPastOnesByDefault() throws Exception {
        create(patient, patient, body("EXAM", "Hemograma", inDays(5))).andExpect(status().isCreated());
        create(patient, patient, body("CONSULTATION", "Geriatra", inDays(1))).andExpect(status().isCreated());

        list(patient, patient)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Geriatra"))
                .andExpect(jsonPath("$[1].title").value("Hemograma"));
    }

    @Test
    void listCanBeLimitedByPeriod() throws Exception {
        create(patient, patient, body("EXAM", "Hemograma", inDays(5))).andExpect(status().isCreated());
        create(patient, patient, body("CONSULTATION", "Geriatra", inDays(1))).andExpect(status().isCreated());

        mockMvc.perform(get(path(patient)).param("from", inDays(0).toString()).param("to", inDays(3).toString())
                        .header("Authorization", bearer(patient.token())))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Geriatra"));
    }

    @Test
    void updateReschedulesAndMovesTheReminder() throws Exception {
        String id = createdId(patient, patient, body("EXAM", "Hemograma", inDays(2)));
        Instant newDate = inDays(4);

        mockMvc.perform(put(path(patient) + "/" + id).header("Authorization", bearer(patient.token()))
                        .contentType(MediaType.APPLICATION_JSON).content(body("EXAM", "Hemograma completo", newDate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Hemograma completo"))
                .andExpect(jsonPath("$.remindAt").value(newDate.minus(1, ChronoUnit.HOURS).toString()));
    }

    @Test
    void deleteRemovesTheAppointment() throws Exception {
        String id = createdId(patient, patient, body("EXAM", "Hemograma", inDays(2)));

        mockMvc.perform(delete(path(patient) + "/" + id).header("Authorization", bearer(patient.token())))
                .andExpect(status().isNoContent());

        list(patient, patient).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(delete(path(patient) + "/" + id).header("Authorization", bearer(patient.token())))
                .andExpect(status().isNotFound());
    }

    @Test
    void pastDateIsRejected() throws Exception {
        create(patient, patient, body("EXAM", "Hemograma", inDays(-1))).andExpect(status().isBadRequest());
    }

    @Test
    void requiredFieldsAreValidated() throws Exception {
        create(patient, patient, "{\"title\":\"x\",\"scheduledAt\":\"" + inDays(1) + "\"}")
                .andExpect(status().isBadRequest());
        create(patient, patient, body("EXAM", "  ", inDays(1))).andExpect(status().isBadRequest());
        create(patient, patient, "{\"type\":\"EXAM\",\"title\":\"x\"}").andExpect(status().isBadRequest());
        create(patient, patient, body("EXAM", "a".repeat(121), inDays(1))).andExpect(status().isBadRequest());
    }

    @Test
    void caregiverReadingIsRegisteredInTheAccessLog() throws Exception {
        linkCaregiver();
        list(caregiver, patient).andExpect(status().isOk());

        mockMvc.perform(get("/api/patients/me/access-log").header("Authorization", bearer(patient.token())))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].area").value("APPOINTMENTS"))
                .andExpect(jsonPath("$.items[0].accessorId").value(caregiver.id().toString()));
    }

    @Test
    void unlinkedCaregiverIsDenied() throws Exception {
        create(caregiver, patient, body("EXAM", "Hemograma", inDays(1))).andExpect(status().isForbidden());
        list(caregiver, patient).andExpect(status().isForbidden());
    }

    @Test
    void patientCannotTouchAnotherPatientAgenda() throws Exception {
        Account other = accounts.patient("outro@exemplo.com", "39053344705");
        String id = createdId(patient, patient, body("EXAM", "Hemograma", inDays(2)));

        create(other, patient, body("EXAM", "Hemograma", inDays(1))).andExpect(status().isForbidden());
        mockMvc.perform(delete(path(patient) + "/" + id).header("Authorization", bearer(other.token())))
                .andExpect(status().isForbidden());
    }

    @Test
    void appointmentIsNotReachableThroughAnotherPatientPath() throws Exception {
        String id = createdId(patient, patient, body("EXAM", "Hemograma", inDays(2)));
        Account other = accounts.patient("outro@exemplo.com", "39053344705");

        mockMvc.perform(delete(path(other) + "/" + id).header("Authorization", bearer(other.token())))
                .andExpect(status().isNotFound());
        list(patient, patient).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void doctorHasNoAccessToTheAgenda() throws Exception {
        list(doctor, patient).andExpect(status().isForbidden());
        create(doctor, patient, body("EXAM", "Hemograma", inDays(1))).andExpect(status().isForbidden());
    }

    @Test
    void anonymousRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get(path(patient))).andExpect(status().isUnauthorized());
    }
}
