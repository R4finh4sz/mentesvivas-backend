package com.mentesvivas.backend.controller.breathing;

import com.jayway.jsonpath.JsonPath;
import com.mentesvivas.backend.entity.link.CaregiverPatientLink;
import com.mentesvivas.backend.entity.link.LinkStatus;
import com.mentesvivas.backend.repository.link.CaregiverPatientLinkRepository;
import com.mentesvivas.backend.support.AdminTestSupport;
import com.mentesvivas.backend.support.ClearTemporaryStorage;
import com.mentesvivas.backend.support.LinkTestSupport;
import com.mentesvivas.backend.support.LinkTestSupport.Account;
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
class BreathingControllerTest {

    private static final String CLINIC = "/api/doctors/me/breathing-exercises";
    private static final String ADMIN = "/api/admin/breathing-exercises";
    private static final String PATIENT = "/api/patients/me/breathing-exercises";
    private static final String EXERCISE = """
            {"title":"Exercício de teste","inhaleSeconds":2,"holdSeconds":0,"exhaleSeconds":3,"cycles":2}
            """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AdminTestSupport admin;
    @Autowired
    private LinkTestSupport accounts;
    @Autowired
    private CaregiverPatientLinkRepository links;

    @Test
    void exerciseRequiresValidationAndPublicationAndOnlyNotifiesActiveCaregivers() throws Exception {
        Account patient = accounts.patient("paciente@exemplo.com", "11144477735");
        Account doctor = accounts.doctor("dr.paulo@clinica.com", "12345678909", "Dr. Paulo Lima", true);
        Account caregiver = accounts.caregiver("cuidador@exemplo.com", "52998224725");
        Account former = accounts.caregiver("outro@exemplo.com", "39053344705");
        links.save(new CaregiverPatientLink(caregiver.id(), patient.id(), LinkStatus.ACTIVE));
        links.save(new CaregiverPatientLink(former.id(), patient.id(), LinkStatus.ENDED));
        String adminToken = admin.token();

        validate(doctor.token(), EXERCISE.replace("\"cycles\":2", "\"cycles\":0"))
                .andExpect(status().isBadRequest());
        String validated = validate(doctor.token(), EXERCISE)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.validatedBy").value(doctor.id().toString()))
                .andExpect(jsonPath("$.published").value(false))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(validated, "$.exercise.id");

        read(PATIENT, patient.token()).andExpect(jsonPath("$.exercises.length()").value(0));
        start(id, patient.token()).andExpect(status().isNotFound());
        publication(id, doctor.token(), true).andExpect(status().isForbidden());
        read(ADMIN, adminToken).andExpect(status().isOk()).andExpect(jsonPath("$[0].exercise.id").value(id));
        publication(id, adminToken, true).andExpect(status().isOk()).andExpect(jsonPath("$.published").value(true));
        read(PATIENT, patient.token()).andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].id").value(id))
                .andExpect(jsonPath("$.startNotice").isNotEmpty());
        start(id, caregiver.token()).andExpect(status().isForbidden());
        start(id, patient.token()).andExpect(status().isOk())
                .andExpect(jsonPath("$.exercise.exhaleSeconds").value(3))
                .andExpect(jsonPath("$.notice").isNotEmpty());

        read("/api/notifications", caregiver.token()).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("BREATHING_STARTED"))
                .andExpect(jsonPath("$[0].referenceId").value(patient.id().toString()))
                .andExpect(jsonPath("$[0].silent").value(true));
        read("/api/notifications", former.token()).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        publication(id, adminToken, false).andExpect(status().isOk());
        start(id, patient.token()).andExpect(status().isNotFound());
        read("/api/notifications", caregiver.token()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void onlyDoctorsWithVerifiedCredentialsCanValidate() throws Exception {
        Account doctor = accounts.doctor("dra.ana@clinica.com", "39053344705", "Dra. Ana", false);
        validate(doctor.token(), EXERCISE).andExpect(status().isForbidden());
        validate(admin.token(), EXERCISE).andExpect(status().isForbidden());
    }

    private ResultActions validate(String token, String body) throws Exception {
        return mockMvc.perform(post(CLINIC).header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions publication(String id, String token, boolean published) throws Exception {
        return mockMvc.perform(put(ADMIN + "/" + id + "/publication").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content("{\"published\":" + published + "}"));
    }

    private ResultActions read(String route, String token) throws Exception {
        return mockMvc.perform(get(route).header("Authorization", bearer(token)));
    }

    private ResultActions start(String id, String token) throws Exception {
        return mockMvc.perform(post(PATIENT + "/" + id + "/start").header("Authorization", bearer(token)));
    }
}
