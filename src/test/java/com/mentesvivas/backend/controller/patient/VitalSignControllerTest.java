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
class VitalSignControllerTest {

    private static final String VALID = """
            {"systolic":120,"diastolic":80,"heartRate":72,"glucose":95,"glucoseType":"FASTING",
             "temperature":36.5,"oxygenSaturation":98,"notes":"sem queixas"}
            """;

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
        return "/api/care/patients/" + of.id() + "/vital-signs";
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
    void patientRecordsAndReadsOwnVitalSigns() throws Exception {
        record(patient, patient, VALID)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.systolic").value(120))
                .andExpect(jsonPath("$.glucoseType").value("FASTING"))
                .andExpect(jsonPath("$.temperature").value(36.5))
                .andExpect(jsonPath("$.source").value("TYPED"))
                .andExpect(jsonPath("$.recordedBy").value(patient.id().toString()));

        history(patient, patient)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].heartRate").value(72));
    }

    @Test
    void historyIsOrderedFromNewestToOldest() throws Exception {
        record(patient, patient, "{\"heartRate\":60}").andExpect(status().isCreated());
        Thread.sleep(5);
        record(patient, patient, "{\"heartRate\":90}").andExpect(status().isCreated());

        history(patient, patient)
                .andExpect(jsonPath("$[0].heartRate").value(90))
                .andExpect(jsonPath("$[1].heartRate").value(60));
    }

    @Test
    void linkedCaregiverRecordsOnBehalfOfThePatient() throws Exception {
        linkCaregiver();

        record(caregiver, patient, VALID)
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
                .andExpect(jsonPath("$.items[0].area").value("MEASUREMENTS"))
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
        record(caregiver, patient, VALID).andExpect(status().isForbidden());
        history(caregiver, patient).andExpect(status().isForbidden());
    }

    @Test
    void caregiverLosesAccessWhenTheLinkEnds() throws Exception {
        linkCaregiver();
        String links = mockMvc.perform(get("/api/caregiver-links").header("Authorization", bearer(patient.token())))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(links, "$[0].id");
        mockMvc.perform(post("/api/caregiver-links/" + id + "/end").header("Authorization", bearer(patient.token())))
                .andExpect(status().isOk());

        history(caregiver, patient).andExpect(status().isForbidden());
    }

    @Test
    void patientCannotAccessAnotherPatient() throws Exception {
        Account other = accounts.patient("outro@exemplo.com", "39053344705");

        record(other, patient, VALID).andExpect(status().isForbidden());
        history(other, patient).andExpect(status().isForbidden());
    }

    @Test
    void linkedDoctorReadsButCannotRecord() throws Exception {
        linkDoctor();
        record(patient, patient, VALID).andExpect(status().isCreated());

        history(doctor, patient)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        record(doctor, patient, VALID).andExpect(status().isForbidden());
    }

    @Test
    void unlinkedDoctorIsDenied() throws Exception {
        history(doctor, patient).andExpect(status().isForbidden());
    }

    @Test
    void glucoseRequiresItsType() throws Exception {
        record(patient, patient, "{\"glucose\":95}").andExpect(status().isBadRequest());
    }

    @Test
    void bloodPressureNeedsBothValues() throws Exception {
        record(patient, patient, "{\"systolic\":120}").andExpect(status().isBadRequest());
    }

    @Test
    void requestWithoutAnyMeasurementIsRejected() throws Exception {
        record(patient, patient, "{\"notes\":\"so uma nota\"}").andExpect(status().isBadRequest());
    }

    @Test
    void valuesOutsideTheHumanSpectrumAreBlocked() throws Exception {
        record(patient, patient, "{\"oxygenSaturation\":150}").andExpect(status().isBadRequest());
        record(patient, patient, "{\"systolic\":80,\"diastolic\":120}").andExpect(status().isBadRequest());

        history(patient, patient).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void ocrOriginIsFlaggedToCheckAgainstTheOriginal() throws Exception {
        record(patient, patient, "{\"heartRate\":70,\"source\":\"OCR\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("OCR"))
                .andExpect(jsonPath("$.checkAgainstOriginal").value(true));
    }

    @Test
    void anonymousRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get(path(patient))).andExpect(status().isUnauthorized());
    }
}
