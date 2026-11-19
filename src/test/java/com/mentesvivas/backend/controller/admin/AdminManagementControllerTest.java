package com.mentesvivas.backend.controller.admin;

import com.jayway.jsonpath.JsonPath;
import com.mentesvivas.backend.entity.audit.AuditEvent;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.repository.audit.AuditEventRepository;
import com.mentesvivas.backend.support.AdminTestSupport;
import com.mentesvivas.backend.support.AuthTestSupport;
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

import java.util.UUID;

import static com.mentesvivas.backend.support.AuthTestSupport.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(ClearTemporaryStorage.class)
@AutoConfigureMockMvc
@Transactional
class AdminManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private LinkTestSupport accounts;
    @Autowired
    private AdminTestSupport admin;
    @Autowired
    private AuditEventRepository events;

    private Account patient;
    private Account caregiver;
    private Account doctor;
    private String adminToken;
    private UUID adminId;

    @BeforeEach
    void setUp() throws Exception {
        patient = accounts.patient("paciente@exemplo.com", "11144477735");
        caregiver = accounts.caregiver("cuidador@exemplo.com", "52998224725");
        doctor = accounts.doctor("dr.paulo@clinica.com", "12345678909", "Dr. Paulo Lima", false);
        adminToken = admin.token();
        adminId = UUID.fromString(JsonPath.read(mockMvc.perform(get("/api/users/me")
                .header("Authorization", bearer(adminToken))).andReturn().getResponse().getContentAsString(), "$.id"));
    }

    private ResultActions adminGet(String path) throws Exception {
        return mockMvc.perform(get(path).header("Authorization", bearer(adminToken)));
    }

    private ResultActions adminPost(String path, String body) throws Exception {
        return mockMvc.perform(post(path).header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions login(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, AuthTestSupport.PASSWORD)));
    }

    @Test
    void listsEveryUserByRoleWithAccountDataOnly() throws Exception {
        adminGet("/api/admin/users?role=PATIENT")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(patient.id().toString()))
                .andExpect(jsonPath("$.items[0].email").value("paciente@exemplo.com"))
                .andExpect(jsonPath("$.items[0].active").value(true))
                .andExpect(jsonPath("$.items[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.items[0].totpSecret").doesNotExist())
                .andExpect(jsonPath("$.items[0].cpf").doesNotExist());
        adminGet("/api/admin/users?role=DOCTOR")
                .andExpect(jsonPath("$.items[0].crmVerified").value(false));
        adminGet("/api/admin/users").andExpect(jsonPath("$.totalItems").value(4));
    }

    @Test
    void listingIsPaginatedAndRecordedInTheAudit() throws Exception {
        adminGet("/api/admin/users?size=2&page=1")
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.size").value(2));

        assertThat(events.findByActorUserIdOrderByOccurredAtDesc(adminId))
                .extracting(AuditEvent::getEventType).contains(AuditEventType.ADMIN_USERS_LISTED);
    }

    @Test
    void onlyTheAdministratorUsesTheAdminAreaForUsersAndMetrics() throws Exception {
        for (Account other : new Account[]{patient, caregiver, doctor}) {
            mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(other.token())))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/admin/metrics").header("Authorization", bearer(other.token())))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/metrics")).andExpect(status().isUnauthorized());
    }

    @Test
    void deactivatedAccountCannotLoginAndItsSessionsStopWorking() throws Exception {
        String refresh = JsonPath.read(login("paciente@exemplo.com").andReturn().getResponse().getContentAsString(),
                "$.refreshToken");

        adminPost("/api/admin/users/" + patient.id() + "/deactivate", "{\"justification\":\"Conta comprometida\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        login("paciente@exemplo.com").andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(patient.token())))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(refresh))).andExpect(status().isUnauthorized());
    }

    @Test
    void nothingIsDeletedWhenAnAccountIsDeactivated() throws Exception {
        adminPost("/api/admin/users/" + caregiver.id() + "/deactivate", "{\"justification\":\"Teste\"}")
                .andExpect(status().isOk());

        adminGet("/api/admin/users?role=CAREGIVER")
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].active").value(false));
    }

    @Test
    void reactivatingAllowsTheLoginAgain() throws Exception {
        adminPost("/api/admin/users/" + patient.id() + "/deactivate", "{\"justification\":\"Suspeita\"}")
                .andExpect(status().isOk());

        adminPost("/api/admin/users/" + patient.id() + "/reactivate", "{\"justification\":\"Verificado\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        login("paciente@exemplo.com").andExpect(status().isOk());
    }

    @Test
    void deactivationAndReactivationAreAuditedWithTheJustification() throws Exception {
        adminPost("/api/admin/users/" + patient.id() + "/deactivate", "{\"justification\":\"Suspeita de fraude\"}");
        adminPost("/api/admin/users/" + patient.id() + "/reactivate", "{\"justification\":\"Falso positivo\"}");

        var audited = events.findByActorUserIdOrderByOccurredAtDesc(adminId);
        assertThat(audited).extracting(AuditEvent::getEventType)
                .contains(AuditEventType.USER_DEACTIVATED, AuditEventType.USER_REACTIVATED);
        assertThat(audited.stream().filter(event -> event.getEventType() == AuditEventType.USER_DEACTIVATED)
                .findFirst().orElseThrow().getJustification()).isEqualTo("Suspeita de fraude");
    }

    @Test
    void justificationIsRequired() throws Exception {
        adminPost("/api/admin/users/" + patient.id() + "/deactivate", "{\"justification\":\" \"}")
                .andExpect(status().isBadRequest());
        adminPost("/api/admin/users/" + patient.id() + "/deactivate", "{}").andExpect(status().isBadRequest());
    }

    @Test
    void theAdministratorCannotDeactivateTheirOwnAccount() throws Exception {
        adminPost("/api/admin/users/" + adminId + "/deactivate", "{\"justification\":\"Quero sair\"}")
                .andExpect(status().isConflict());

        admin.token();
    }

    @Test
    void unknownUserIsNotFoundAndDoubleDeactivationIsAConflict() throws Exception {
        adminPost("/api/admin/users/" + UUID.randomUUID() + "/deactivate", "{\"justification\":\"x\"}")
                .andExpect(status().isNotFound());

        adminPost("/api/admin/users/" + patient.id() + "/deactivate", "{\"justification\":\"x\"}").andExpect(status().isOk());
        adminPost("/api/admin/users/" + patient.id() + "/deactivate", "{\"justification\":\"x\"}").andExpect(status().isConflict());
    }

    @Test
    void thereIsNoWayToDeleteAnAccountOrTheAdministratorData() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/admin/users/" + patient.id()).header("Authorization", bearer(adminToken)))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/users/me").header("Authorization", bearer(adminToken)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void metricsAreAggregatedAndCarryNoIdentification() throws Exception {
        adminPost("/api/admin/users/" + caregiver.id() + "/deactivate", "{\"justification\":\"Teste\"}");

        adminGet("/api/admin/metrics")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usersByRole.PATIENT.active").value(1))
                .andExpect(jsonPath("$.usersByRole.CAREGIVER.inactive").value(1))
                .andExpect(jsonPath("$.usersByRole.ADMINISTRATOR.active").value(1))
                .andExpect(jsonPath("$.doctorsPendingCrm").value(1))
                .andExpect(jsonPath("$.loginsSucceeded").isNumber())
                .andExpect(jsonPath("$.loginsFailed").isNumber())
                .andExpect(jsonPath("$.doctorLinks.ACTIVE").value(0))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void metricsPeriodFiltersTheLoginCounters() throws Exception {
        adminGet("/api/admin/metrics?from=2000-01-01T00:00:00Z&to=2000-01-02T00:00:00Z")
                .andExpect(jsonPath("$.loginsSucceeded").value(0))
                .andExpect(jsonPath("$.loginsFailed").value(0));
    }
}
