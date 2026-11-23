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
class OccurrenceControllerTest {

    private static final String FALL =
            "{\"type\":\"FALL\",\"severity\":\"MODERATE\",\"description\":\"Caiu no banheiro\",\"needsHospital\":true}";
    private static final String AGITATION =
            "{\"type\":\"AGITATION\",\"severity\":\"MILD\",\"description\":\"Ficou inquieto a tarde\",\"needsHospital\":false}";

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
        return "/api/care/patients/" + of.id() + "/occurrences";
    }

    private ResultActions record(Account actor, Account target, String body) throws Exception {
        return mockMvc.perform(post(path(target)).header("Authorization", bearer(actor.token()))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions history(Account actor, Account target) throws Exception {
        return mockMvc.perform(get(path(target)).header("Authorization", bearer(actor.token())));
    }

    private ResultActions search(Account actor, Account target, String name, String value) throws Exception {
        return mockMvc.perform(get(path(target)).param(name, value).header("Authorization", bearer(actor.token())));
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
    void linkedCaregiverRecordsAnOccurrenceWithAutomaticTime() throws Exception {
        linkCaregiver();

        record(caregiver, patient, FALL)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("FALL"))
                .andExpect(jsonPath("$.severity").value("MODERATE"))
                .andExpect(jsonPath("$.description").value("Caiu no banheiro"))
                .andExpect(jsonPath("$.needsHospital").value(true))
                .andExpect(jsonPath("$.source").value("TYPED"))
                .andExpect(jsonPath("$.recordedBy").value(caregiver.id().toString()))
                .andExpect(jsonPath("$.occurredAt").isNotEmpty());

        history(patient, patient).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void dictatedDescriptionKeepsItsOrigin() throws Exception {
        record(patient, patient, "{\"type\":\"CONFUSION\",\"severity\":\"MILD\",\"description\":\"Nao reconheceu a sala\","
                + "\"needsHospital\":false,\"source\":\"DICTATED\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("DICTATED"));
    }

    @Test
    void ocrOriginIsRejected() throws Exception {
        record(patient, patient, "{\"type\":\"FALL\",\"severity\":\"MILD\",\"description\":\"x\","
                + "\"needsHospital\":false,\"source\":\"OCR\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void historyIsNewestFirstAndCanBeFilteredByType() throws Exception {
        record(patient, patient, FALL).andExpect(status().isCreated());
        record(patient, patient, AGITATION).andExpect(status().isCreated());

        history(patient, patient)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("AGITATION"));
        search(patient, patient, "type", "FALL")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("FALL"));
    }

    @Test
    void keywordSearchIgnoresCase() throws Exception {
        record(patient, patient, FALL).andExpect(status().isCreated());
        record(patient, patient, AGITATION).andExpect(status().isCreated());

        search(patient, patient, "keyword", "BANHEIRO")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("FALL"));
        search(patient, patient, "keyword", "inexistente").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void keywordSearchTreatsWildcardsAsPlainText() throws Exception {
        record(patient, patient, FALL).andExpect(status().isCreated());

        search(patient, patient, "keyword", "%").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void caregiverReadingIsRegisteredInTheAccessLog() throws Exception {
        linkCaregiver();
        history(caregiver, patient).andExpect(status().isOk());

        mockMvc.perform(get("/api/patients/me/access-log").header("Authorization", bearer(patient.token())))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].area").value("OCCURRENCES"))
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
        record(caregiver, patient, FALL).andExpect(status().isForbidden());
        history(caregiver, patient).andExpect(status().isForbidden());
    }

    @Test
    void patientCannotAccessAnotherPatient() throws Exception {
        Account other = accounts.patient("outro@exemplo.com", "39053344705");

        record(other, patient, FALL).andExpect(status().isForbidden());
        history(other, patient).andExpect(status().isForbidden());
    }

    @Test
    void linkedDoctorReadsButCannotRecord() throws Exception {
        linkDoctor();
        record(patient, patient, FALL).andExpect(status().isCreated());

        history(doctor, patient).andExpect(jsonPath("$.length()").value(1));
        record(doctor, patient, FALL).andExpect(status().isForbidden());
    }

    @Test
    void unlinkedDoctorIsDenied() throws Exception {
        history(doctor, patient).andExpect(status().isForbidden());
    }

    @Test
    void requiredFieldsAreValidated() throws Exception {
        record(patient, patient, "{\"severity\":\"MILD\",\"description\":\"x\",\"needsHospital\":false}")
                .andExpect(status().isBadRequest());
        record(patient, patient, "{\"type\":\"FALL\",\"description\":\"x\",\"needsHospital\":false}")
                .andExpect(status().isBadRequest());
        record(patient, patient, "{\"type\":\"FALL\",\"severity\":\"MILD\",\"description\":\"  \",\"needsHospital\":false}")
                .andExpect(status().isBadRequest());
        record(patient, patient, "{\"type\":\"FALL\",\"severity\":\"MILD\",\"description\":\"x\"}")
                .andExpect(status().isBadRequest());

        history(patient, patient).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void longDescriptionIsRejected() throws Exception {
        record(patient, patient, "{\"type\":\"FALL\",\"severity\":\"MILD\",\"description\":\"" + "a".repeat(1001)
                + "\",\"needsHospital\":false}").andExpect(status().isBadRequest());
    }

    @Test
    void anonymousRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get(path(patient))).andExpect(status().isUnauthorized());
    }
}
