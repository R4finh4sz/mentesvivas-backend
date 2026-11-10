package com.mentesvivas.backend.controller.clinical;

import com.jayway.jsonpath.JsonPath;
import com.mentesvivas.backend.entity.link.DoctorPatientLink;
import com.mentesvivas.backend.entity.link.LinkStatus;
import com.mentesvivas.backend.repository.link.DoctorPatientLinkRepository;
import com.mentesvivas.backend.repository.user.UserRepository;
import com.mentesvivas.backend.support.AuthTestSupport;
import com.mentesvivas.backend.support.CapturedEmailSender;
import com.mentesvivas.backend.support.ClearTemporaryStorage;
import com.mentesvivas.backend.support.TermsTestSupport;
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
import java.util.UUID;

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
class DoctorClinicalControllerTest {

    private static final String DOCTOR = "dr.paulo@clinica.com";
    private static final String ALLERGY = "{\"category\":\"MEDICATION\",\"description\":\"Dipirona\",\"severity\":\"SEVERE\"}";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AuthTestSupport auth;
    @Autowired
    private TermsTestSupport terms;
    @Autowired
    private CapturedEmailSender emails;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DoctorPatientLinkRepository linkRepository;

    private String patientToken;
    private UUID patientId;
    private UUID doctorId;
    private String doctorToken;

    @BeforeEach
    void setUp() throws Exception {
        patientToken = auth.registerAndLogin("paciente@exemplo.com", "52998224725", "PATIENT");
        patientId = UUID.fromString(JsonPath.read(mockMvc.perform(get("/api/users/me")
                .header("Authorization", bearer(patientToken))).andReturn().getResponse().getContentAsString(), "$.id"));

        int version = terms.publish();
        doctorId = UUID.fromString(JsonPath.read(mockMvc.perform(post("/api/auth/register/doctor")
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"fullName":"Dr. Paulo","cpf":"12345678909","phone":"11988887777",
                         "email":"%s","password":"%s","passwordConfirmation":"%s",
                         "crm":"123456","crmState":"SP","termsVersion":%d,"acceptTerms":true}
                        """.formatted(DOCTOR, AuthTestSupport.PASSWORD, AuthTestSupport.PASSWORD, version)))
                .andReturn().getResponse().getContentAsString(), "$.id"));
        emails.clear();
        doctorToken = loginDoctor();
    }

    @Test
    void doctorWithoutVerifiedCrmCannotEdit() throws Exception {
        link(LinkStatus.ACTIVE);

        send(post(base() + "/allergies"), ALLERGY).andExpect(status().isForbidden());
    }

    @Test
    void doctorWithoutActiveLinkCannotEdit() throws Exception {
        verifyCrm();

        send(post(base() + "/allergies"), ALLERGY).andExpect(status().isForbidden());
        link(LinkStatus.PENDING);
        send(post(base() + "/allergies"), ALLERGY).andExpect(status().isForbidden());
    }

    @Test
    void patientsAndCaregiversCannotUseTheDoctorRoutes() throws Exception {
        mockMvc.perform(post(base() + "/allergies").header("Authorization", bearer(patientToken))
                .contentType(MediaType.APPLICATION_JSON).content(ALLERGY)).andExpect(status().isForbidden());
        mockMvc.perform(post(base() + "/allergies").contentType(MediaType.APPLICATION_JSON).content(ALLERGY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void linkedDoctorManagesAllergiesAndTheChangesAreAttributedToThem() throws Exception {
        verifyCrm();
        link(LinkStatus.ACTIVE);

        String body = send(post(base() + "/allergies"), ALLERGY)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String allergyId = JsonPath.read(body, "$.id");

        send(put(base() + "/allergies/" + allergyId),
                "{\"category\":\"FOOD\",\"description\":\"Amendoim\",\"severity\":\"MILD\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Amendoim"));

        mockMvc.perform(get("/api/patients/me/allergies").header("Authorization", bearer(patientToken)))
                .andExpect(jsonPath("$[0].description").value("Amendoim"));

        mockMvc.perform(delete(base() + "/allergies/" + allergyId).header("Authorization", bearer(doctorToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/patients/me/profile").header("Authorization", bearer(patientToken)))
                .andExpect(jsonPath("$.allergyStatus").value("NOT_INFORMED"));

        mockMvc.perform(get("/api/patients/me/changes").header("Authorization", bearer(patientToken)))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].changeType").value("DELETED"))
                .andExpect(jsonPath("$[0].changedBy").value(doctorId.toString()));
    }

    @Test
    void doctorCannotTouchAnAllergyThatIsNotFromThePatient() throws Exception {
        verifyCrm();
        link(LinkStatus.ACTIVE);

        send(put(base() + "/allergies/" + UUID.randomUUID()), ALLERGY).andExpect(status().isNotFound());
    }

    @Test
    void doctorValidatesTheDiseaseAndThePatientCannotChangeItAfterwards() throws Exception {
        verifyCrm();
        link(LinkStatus.ACTIVE);
        mockMvc.perform(put("/api/patients/me/profile").header("Authorization", bearer(patientToken))
                .contentType(MediaType.APPLICATION_JSON).content("{\"declaredDisease\":\"Esquecimentos\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.declaredDiseaseValidated").value(false));

        send(put(base() + "/declared-disease"), "{\"disease\":\"Alzheimer\"}").andExpect(status().isNoContent());

        mockMvc.perform(get("/api/patients/me/profile").header("Authorization", bearer(patientToken)))
                .andExpect(jsonPath("$.declaredDisease").value("Alzheimer"))
                .andExpect(jsonPath("$.declaredDiseaseValidated").value(true));
        mockMvc.perform(put("/api/patients/me/profile").header("Authorization", bearer(patientToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"declaredDisease\":\"Outra coisa\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void patientStillDeclaresTheDiseaseTheFirstTime() throws Exception {
        mockMvc.perform(put("/api/patients/me/profile").header("Authorization", bearer(patientToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"declaredDisease\":\"Esquecimentos\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void doctorSetsNoKnownAllergiesOnlyWhenThereAreNone() throws Exception {
        verifyCrm();
        link(LinkStatus.ACTIVE);

        send(put(base() + "/allergy-status"), "{\"status\":\"NONE_KNOWN\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NONE_KNOWN"));
        send(post(base() + "/allergies"), ALLERGY).andExpect(status().isCreated());
        send(put(base() + "/allergy-status"), "{\"status\":\"NONE_KNOWN\"}").andExpect(status().isConflict());
    }

    private String base() {
        return "/api/doctors/me/patients/" + patientId;
    }

    private void verifyCrm() {
        var doctor = userRepository.findById(doctorId).orElseThrow();
        doctor.setCrmVerified(true);
        userRepository.saveAndFlush(doctor);
    }

    private void link(LinkStatus status) {
        linkRepository.saveAndFlush(new DoctorPatientLink(doctorId, patientId, status, Instant.now()));
    }

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                               String json) throws Exception {
        return mockMvc.perform(request.header("Authorization", bearer(doctorToken))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String loginDoctor() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(DOCTOR, AuthTestSupport.PASSWORD)));
        String body = mockMvc.perform(post("/api/auth/2fa/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"code\":\"%s\"}".formatted(DOCTOR, emails.lastCode())))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }
}
