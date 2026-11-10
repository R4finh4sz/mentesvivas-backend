package com.mentesvivas.backend.controller.admin;

import com.jayway.jsonpath.JsonPath;
import com.mentesvivas.backend.support.AuthTestSupport;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.mentesvivas.backend.support.AuthTestSupport.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(ClearTemporaryStorage.class)
@AutoConfigureMockMvc
@Transactional
class DoctorCrmControllerTest {

    private static final String DOCTOR_EMAIL = "dr.paulo@clinica.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthTestSupport auth;

    @Autowired
    private TermsTestSupport terms;

    private String adminToken;
    private UUID doctorId;

    @BeforeEach
    void setUp() throws Exception {
        int version = terms.publish();
        String body = mockMvc.perform(post("/api/auth/register/doctor").contentType(MediaType.APPLICATION_JSON).content("""
                {"fullName":"Dr. Paulo","cpf":"12345678909","phone":"11988887777",
                 "email":"%s","password":"%s","passwordConfirmation":"%s",
                 "crm":"123456","crmState":"SP","termsVersion":%d,"acceptTerms":true}
                """.formatted(DOCTOR_EMAIL, AuthTestSupport.PASSWORD, AuthTestSupport.PASSWORD, version)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        doctorId = UUID.fromString(JsonPath.read(body, "$.id"));
        adminToken = loginAdmin();
    }

    private String loginAdmin() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}"
                                .formatted(TermsTestSupport.ADMIN_EMAIL, TermsTestSupport.ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    @Test
    void adminVerifiesTheCrmAndTheDoctorLeavesThePendingList() throws Exception {
        mockMvc.perform(get("/api/admin/doctors/pending-crm").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(doctorId.toString()))
                .andExpect(jsonPath("$[0].crm").value("123456"));

        mockMvc.perform(post("/api/admin/doctors/{id}/verify-crm", doctorId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/admin/doctors/{id}/verify-crm", doctorId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/doctors/pending-crm").header("Authorization", bearer(adminToken)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void unknownDoctorReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/admin/doctors/{id}/verify-crm", UUID.randomUUID())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonAdminsCannotUseTheAdminRoutes() throws Exception {
        String patient = auth.registerAndLogin("paciente@exemplo.com", "52998224725", "PATIENT");

        mockMvc.perform(get("/api/admin/doctors/pending-crm")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/doctors/pending-crm").header("Authorization", bearer(patient)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/doctors/{id}/verify-crm", doctorId).header("Authorization", bearer(patient)))
                .andExpect(status().isForbidden());
    }
}
