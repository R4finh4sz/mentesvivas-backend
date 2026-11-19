package com.mentesvivas.backend.controller.patient;

import com.jayway.jsonpath.JsonPath;
import com.mentesvivas.backend.entity.audit.AccessEvent;
import com.mentesvivas.backend.repository.audit.AccessEventRepository;
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

import java.lang.reflect.Method;
import java.util.Arrays;

import static com.mentesvivas.backend.support.AuthTestSupport.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(ClearTemporaryStorage.class)
@AutoConfigureMockMvc
@Transactional
class AccessLogControllerTest {

    private static final String BASE = "/api/patients/me/access-log";
    private static final String ALLERGY = "{\"category\":\"MEDICATION\",\"description\":\"Dipirona\",\"severity\":\"SEVERE\"}";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private LinkTestSupport accounts;

    private Account patient;
    private Account doctor;

    @BeforeEach
    void setUp() throws Exception {
        patient = accounts.patient("paciente@exemplo.com", "11144477735");
        doctor = accounts.doctor("dr.paulo@clinica.com", "12345678909", "Dr. Paulo Lima", true);
        mockMvc.perform(post("/api/doctor-links/invitations").header("Authorization", bearer(doctor.token()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"paciente@exemplo.com\"}"));
        String id = JsonPath.read(mockMvc.perform(get("/api/doctor-links").header("Authorization", bearer(patient.token())))
                .andReturn().getResponse().getContentAsString(), "$[0].id");
        mockMvc.perform(post("/api/doctor-links/" + id + "/accept").header("Authorization", bearer(patient.token())))
                .andExpect(status().isOk());
    }

    private ResultActions doctorAddsAnAllergy() throws Exception {
        return mockMvc.perform(post("/api/doctors/me/patients/" + patient.id() + "/allergies")
                .header("Authorization", bearer(doctor.token())).contentType(MediaType.APPLICATION_JSON).content(ALLERGY));
    }

    private ResultActions log(String query) throws Exception {
        return mockMvc.perform(get(BASE + query).header("Authorization", bearer(patient.token())));
    }

    @Test
    void emptyBeforeAnyoneAccessesThePatientData() throws Exception {
        log("").andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void doctorAccessAppearsWithTheNameRoleCrmAndAreaResolvedAtQueryTime() throws Exception {
        doctorAddsAnAllergy().andExpect(status().isCreated());

        log("").andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].area").value("ALLERGIES"))
                .andExpect(jsonPath("$.items[0].accessorId").value(doctor.id().toString()))
                .andExpect(jsonPath("$.items[0].accessorName").value("Dr. Paulo Lima"))
                .andExpect(jsonPath("$.items[0].accessorRole").value("DOCTOR"))
                .andExpect(jsonPath("$.items[0].crm").value("123456"))
                .andExpect(jsonPath("$.items[0].occurredAt").exists());
    }

    @Test
    void thePatientOwnActionsAreNotListed() throws Exception {
        mockMvc.perform(post("/api/patients/me/allergies").header("Authorization", bearer(patient.token()))
                .contentType(MediaType.APPLICATION_JSON).content(ALLERGY)).andExpect(status().isCreated());
        mockMvc.perform(get("/api/patients/me/profile").header("Authorization", bearer(patient.token())));

        log("").andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void everyDoctorOperationOnThePatientDataIsRecordedWithItsArea() throws Exception {
        String created = doctorAddsAnAllergy().andReturn().getResponse().getContentAsString();
        String allergyId = JsonPath.read(created, "$.id");
        mockMvc.perform(put("/api/doctors/me/patients/" + patient.id() + "/declared-disease")
                .header("Authorization", bearer(doctor.token())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"disease\":\"Alzheimer\"}")).andExpect(status().isNoContent());
        mockMvc.perform(put("/api/doctors/me/patients/" + patient.id() + "/allergies/" + allergyId)
                .header("Authorization", bearer(doctor.token())).contentType(MediaType.APPLICATION_JSON).content(ALLERGY));

        log("").andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.items[?(@.area == 'DECLARED_DISEASE')]").isNotEmpty());
    }

    @Test
    void listIsPaginatedAndMostRecentFirst() throws Exception {
        for (int i = 0; i < 3; i++) {
            doctorAddsAnAllergy().andExpect(status().isCreated());
        }

        log("?size=2&page=0").andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.size").value(2));
        log("?size=2&page=1").andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void periodFilterExcludesAccessesOutsideTheWindow() throws Exception {
        doctorAddsAnAllergy().andExpect(status().isCreated());

        log("?from=2999-01-01T00:00:00Z").andExpect(jsonPath("$.totalItems").value(0));
        log("?to=2000-01-01T00:00:00Z").andExpect(jsonPath("$.totalItems").value(0));
        log("?from=2000-01-01T00:00:00Z&to=2999-01-01T00:00:00Z").andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void oneEntryNeverExposesAnotherPatientAccesses() throws Exception {
        doctorAddsAnAllergy().andExpect(status().isCreated());
        Account other = accounts.patient("outro@exemplo.com", "39053344705");

        mockMvc.perform(get(BASE).header("Authorization", bearer(other.token())))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void onlyPatientsConsultTheirLog() throws Exception {
        Account caregiver = accounts.caregiver("cuidador@exemplo.com", "52998224725");

        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE).header("Authorization", bearer(doctor.token()))).andExpect(status().isForbidden());
        mockMvc.perform(get(BASE).header("Authorization", bearer(caregiver.token()))).andExpect(status().isForbidden());
    }

    @Test
    void accessEventsCannotBeChangedByTheApplication() {
        assertThat(AccessEvent.class.isAnnotationPresent(org.hibernate.annotations.Immutable.class)).isTrue();
        assertThat(Arrays.stream(AccessEvent.class.getDeclaredMethods()).map(Method::getName))
                .noneMatch(name -> name.startsWith("set"));
        assertThat(Arrays.stream(AccessEventRepository.class.getMethods()).map(Method::getName))
                .noneMatch(name -> name.startsWith("delete") || name.startsWith("remove") || name.startsWith("update"));
    }
}
