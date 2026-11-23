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

import java.time.LocalDate;

import static com.mentesvivas.backend.support.AuthTestSupport.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(ClearTemporaryStorage.class)
@AutoConfigureMockMvc
@Transactional
class ExamControllerTest {

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
        return "/api/care/patients/" + of.id() + "/exams";
    }

    private String body(String category, String title, LocalDate date) {
        return "{\"category\":\"" + category + "\",\"fileType\":\"PDF\",\"title\":\"" + title + "\","
                + "\"fileUrl\":\"https://arquivos.exemplo.com/laudo.pdf\",\"examDate\":\"" + date + "\"}";
    }

    private ResultActions upload(Account actor, Account target, String body) throws Exception {
        return mockMvc.perform(post(path(target)).header("Authorization", bearer(actor.token()))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions list(Account actor, Account target) throws Exception {
        return mockMvc.perform(get(path(target)).header("Authorization", bearer(actor.token())));
    }

    private String uploadedId(Account actor, Account target, String body) throws Exception {
        String response = upload(actor, target, body).andExpect(status().isCreated()).andReturn().getResponse()
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

    private void linkDoctor() throws Exception {
        mockMvc.perform(post("/api/doctor-links/invitations").header("Authorization", bearer(doctor.token()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"paciente@exemplo.com\"}"))
                .andExpect(status().isAccepted());
        String links = mockMvc.perform(get("/api/doctor-links").header("Authorization", bearer(patient.token())))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(links, "$[0].id");
        mockMvc.perform(post("/api/doctor-links/" + id + "/accept").header("Authorization", bearer(patient.token())))
                .andExpect(status().isOk());
    }

    @Test
    void linkedCaregiverUploadsAnExamForThePatient() throws Exception {
        linkCaregiver();

        upload(caregiver, patient, body("BLOOD", "Hemograma", LocalDate.now().minusDays(2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("BLOOD"))
                .andExpect(jsonPath("$.fileType").value("PDF"))
                .andExpect(jsonPath("$.title").value("Hemograma"))
                .andExpect(jsonPath("$.fileUrl").value("https://arquivos.exemplo.com/laudo.pdf"))
                .andExpect(jsonPath("$.uploadedBy").value(caregiver.id().toString()));

        list(patient, patient).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void listIsNewestExamFirstAndCanFilterByCategory() throws Exception {
        upload(patient, patient, body("BLOOD", "Hemograma", LocalDate.now().minusDays(10))).andExpect(status().isCreated());
        upload(patient, patient, body("IMAGING", "Raio-X", LocalDate.now().minusDays(1))).andExpect(status().isCreated());

        list(patient, patient)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Raio-X"))
                .andExpect(jsonPath("$[1].title").value("Hemograma"));

        mockMvc.perform(get(path(patient)).param("category", "BLOOD")
                        .header("Authorization", bearer(patient.token())))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Hemograma"));
    }

    @Test
    void linkedDoctorReadsTheExamsButCannotUpload() throws Exception {
        upload(patient, patient, body("BLOOD", "Hemograma", LocalDate.now())).andExpect(status().isCreated());
        linkDoctor();

        list(doctor, patient).andExpect(jsonPath("$.length()").value(1));
        upload(doctor, patient, body("BLOOD", "Outro", LocalDate.now())).andExpect(status().isForbidden());
    }

    @Test
    void doctorReadingIsRegisteredInTheAccessLog() throws Exception {
        linkDoctor();
        list(doctor, patient).andExpect(status().isOk());

        mockMvc.perform(get("/api/patients/me/access-log").header("Authorization", bearer(patient.token())))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].area").value("DOCUMENTS"))
                .andExpect(jsonPath("$.items[0].accessorId").value(doctor.id().toString()));
    }

    @Test
    void unlinkedDoctorIsDenied() throws Exception {
        list(doctor, patient).andExpect(status().isForbidden());
    }

    @Test
    void deleteRemovesTheExam() throws Exception {
        String id = uploadedId(patient, patient, body("OTHER", "Laudo", LocalDate.now()));

        mockMvc.perform(delete(path(patient) + "/" + id).header("Authorization", bearer(patient.token())))
                .andExpect(status().isNoContent());

        list(patient, patient).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(delete(path(patient) + "/" + id).header("Authorization", bearer(patient.token())))
                .andExpect(status().isNotFound());
    }

    @Test
    void futureExamDateIsRejected() throws Exception {
        upload(patient, patient, body("BLOOD", "Hemograma", LocalDate.now().plusDays(2)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiredFieldsAndLinkAreValidated() throws Exception {
        String date = LocalDate.now().toString();
        upload(patient, patient, "{\"fileType\":\"PDF\",\"title\":\"x\",\"fileUrl\":\"https://a.com/x\","
                + "\"examDate\":\"" + date + "\"}").andExpect(status().isBadRequest());
        upload(patient, patient, body("BLOOD", "  ", LocalDate.now())).andExpect(status().isBadRequest());
        upload(patient, patient, body("BLOOD", "a".repeat(121), LocalDate.now())).andExpect(status().isBadRequest());
        upload(patient, patient, "{\"category\":\"BLOOD\",\"fileType\":\"PDF\",\"title\":\"x\","
                + "\"fileUrl\":\"http://inseguro.com/x.pdf\",\"examDate\":\"" + date + "\"}")
                .andExpect(status().isBadRequest());
        upload(patient, patient, "{\"category\":\"BLOOD\",\"fileType\":\"PDF\",\"title\":\"x\","
                + "\"fileUrl\":\"https://a.com/x\"}").andExpect(status().isBadRequest());
    }

    @Test
    void unlinkedCaregiverIsDenied() throws Exception {
        upload(caregiver, patient, body("BLOOD", "Hemograma", LocalDate.now())).andExpect(status().isForbidden());
        list(caregiver, patient).andExpect(status().isForbidden());
    }

    @Test
    void patientCannotTouchAnotherPatientExams() throws Exception {
        Account other = accounts.patient("outro@exemplo.com", "39053344705");
        String id = uploadedId(patient, patient, body("BLOOD", "Hemograma", LocalDate.now()));

        upload(other, patient, body("BLOOD", "x", LocalDate.now())).andExpect(status().isForbidden());
        list(other, patient).andExpect(status().isForbidden());
        mockMvc.perform(delete(path(other) + "/" + id).header("Authorization", bearer(other.token())))
                .andExpect(status().isNotFound());
        list(patient, patient).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void anonymousRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get(path(patient))).andExpect(status().isUnauthorized());
    }
}
