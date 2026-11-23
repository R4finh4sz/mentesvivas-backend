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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static com.mentesvivas.backend.support.AuthTestSupport.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(ClearTemporaryStorage.class)
@AutoConfigureMockMvc
@Transactional
class ExamControllerTest {

    private static final byte[] PDF = "%PDF-1.7 laudo".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};

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

    private ResultActions upload(Account actor, Account target, String category, String title, LocalDate date,
                                 MockMultipartFile file) throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart(path(target));
        if (file != null) {
            request.file(file);
        }
        if (category != null) {
            request.param("category", category);
        }
        if (title != null) {
            request.param("title", title);
        }
        if (date != null) {
            request.param("examDate", date.toString());
        }
        return mockMvc.perform(request.header("Authorization", bearer(actor.token())));
    }

    private MockMultipartFile pdf() {
        return new MockMultipartFile("file", "laudo.pdf", "application/pdf", PDF);
    }

    private ResultActions uploadPdf(Account actor, Account target, String category, String title) throws Exception {
        return upload(actor, target, category, title, LocalDate.now(), pdf());
    }

    private ResultActions list(Account actor, Account target) throws Exception {
        return mockMvc.perform(get(path(target)).header("Authorization", bearer(actor.token())));
    }

    private ResultActions download(Account actor, Account target, String id) throws Exception {
        return mockMvc.perform(get(path(target) + "/" + id + "/file")
                .header("Authorization", bearer(actor.token())));
    }

    private String uploadedId(Account actor, Account target, String title) throws Exception {
        String response = uploadPdf(actor, target, "OTHER", title).andExpect(status().isCreated()).andReturn()
                .getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
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
    void linkedCaregiverUploadsAnExamForThePatient() throws Exception {
        linkCaregiver();

        upload(caregiver, patient, "BLOOD", "Hemograma", LocalDate.now().minusDays(2), pdf())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("BLOOD"))
                .andExpect(jsonPath("$.fileType").value("PDF"))
                .andExpect(jsonPath("$.title").value("Hemograma"))
                .andExpect(jsonPath("$.fileName").value("laudo.pdf"))
                .andExpect(jsonPath("$.contentType").value("application/pdf"))
                .andExpect(jsonPath("$.sizeBytes").value(PDF.length))
                .andExpect(jsonPath("$.uploadedBy").value(caregiver.id().toString()));

        list(patient, patient).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void uploadedFileIsReturnedUnchanged() throws Exception {
        String id = uploadedId(patient, patient, "Laudo");

        download(patient, patient, id)
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(PDF))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Disposition",
                        "inline; filename=\"laudo.pdf\"; filename*=UTF-8''laudo.pdf"));
    }

    @Test
    void imagesAreAcceptedWithTheirDetectedType() throws Exception {
        MockMultipartFile image = new MockMultipartFile("file", "raio-x.png", "application/octet-stream", PNG);

        upload(patient, patient, "IMAGING", "Raio-X", LocalDate.now(), image)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileType").value("IMAGE"))
                .andExpect(jsonPath("$.contentType").value("image/png"));
    }

    @Test
    void contentThatIsNotPdfOrImageIsRejectedWhateverTheNameSays() throws Exception {
        MockMultipartFile fake = new MockMultipartFile("file", "laudo.pdf", "application/pdf",
                "<html></html>".getBytes(StandardCharsets.US_ASCII));

        upload(patient, patient, "BLOOD", "Hemograma", LocalDate.now(), fake).andExpect(status().isBadRequest());
        list(patient, patient).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void missingOrEmptyFileIsRejected() throws Exception {
        upload(patient, patient, "BLOOD", "Hemograma", LocalDate.now(), null).andExpect(status().isBadRequest());
        upload(patient, patient, "BLOOD", "Hemograma", LocalDate.now(),
                new MockMultipartFile("file", "vazio.pdf", "application/pdf", new byte[0]))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listIsNewestExamFirstAndCanFilterByCategory() throws Exception {
        upload(patient, patient, "BLOOD", "Hemograma", LocalDate.now().minusDays(10), pdf())
                .andExpect(status().isCreated());
        upload(patient, patient, "IMAGING", "Raio-X", LocalDate.now().minusDays(1), pdf())
                .andExpect(status().isCreated());

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
    void linkedDoctorReadsAndDownloadsButCannotUpload() throws Exception {
        String id = uploadedId(patient, patient, "Hemograma");
        linkDoctor();

        list(doctor, patient).andExpect(jsonPath("$.length()").value(1));
        download(doctor, patient, id).andExpect(status().isOk()).andExpect(content().bytes(PDF));
        uploadPdf(doctor, patient, "BLOOD", "Outro").andExpect(status().isForbidden());
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
    void doctorDownloadIsRegisteredInTheAccessLog() throws Exception {
        String id = uploadedId(patient, patient, "Hemograma");
        linkDoctor();
        download(doctor, patient, id).andExpect(status().isOk());

        mockMvc.perform(get("/api/patients/me/access-log").header("Authorization", bearer(patient.token())))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].area").value("DOCUMENTS"));
    }

    @Test
    void unlinkedDoctorIsDenied() throws Exception {
        String id = uploadedId(patient, patient, "Hemograma");

        list(doctor, patient).andExpect(status().isForbidden());
        download(doctor, patient, id).andExpect(status().isForbidden());
    }

    @Test
    void deleteRemovesTheExamAndItsFile() throws Exception {
        String id = uploadedId(patient, patient, "Laudo");

        mockMvc.perform(delete(path(patient) + "/" + id).header("Authorization", bearer(patient.token())))
                .andExpect(status().isNoContent());

        list(patient, patient).andExpect(jsonPath("$.length()").value(0));
        download(patient, patient, id).andExpect(status().isNotFound());
        mockMvc.perform(delete(path(patient) + "/" + id).header("Authorization", bearer(patient.token())))
                .andExpect(status().isNotFound());
    }

    @Test
    void futureExamDateIsRejected() throws Exception {
        upload(patient, patient, "BLOOD", "Hemograma", LocalDate.now().plusDays(2), pdf())
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiredFieldsAreValidated() throws Exception {
        upload(patient, patient, null, "Hemograma", LocalDate.now(), pdf()).andExpect(status().isBadRequest());
        upload(patient, patient, "BLOOD", "  ", LocalDate.now(), pdf()).andExpect(status().isBadRequest());
        upload(patient, patient, "BLOOD", "a".repeat(121), LocalDate.now(), pdf())
                .andExpect(status().isBadRequest());
        upload(patient, patient, "BLOOD", "Hemograma", null, pdf()).andExpect(status().isBadRequest());
    }

    @Test
    void unlinkedCaregiverIsDenied() throws Exception {
        String id = uploadedId(patient, patient, "Hemograma");

        uploadPdf(caregiver, patient, "BLOOD", "Hemograma").andExpect(status().isForbidden());
        list(caregiver, patient).andExpect(status().isForbidden());
        download(caregiver, patient, id).andExpect(status().isForbidden());
    }

    @Test
    void patientCannotTouchAnotherPatientExams() throws Exception {
        Account other = accounts.patient("outro@exemplo.com", "39053344705");
        String id = uploadedId(patient, patient, "Hemograma");

        uploadPdf(other, patient, "BLOOD", "x").andExpect(status().isForbidden());
        list(other, patient).andExpect(status().isForbidden());
        download(other, patient, id).andExpect(status().isForbidden());
        download(other, other, id).andExpect(status().isNotFound());
        mockMvc.perform(delete(path(other) + "/" + id).header("Authorization", bearer(other.token())))
                .andExpect(status().isNotFound());
        list(patient, patient).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void anonymousRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get(path(patient))).andExpect(status().isUnauthorized());
    }
}
