package com.mentesvivas.backend.controller.prescription;

import com.jayway.jsonpath.JsonPath;
import com.mentesvivas.backend.support.AdminTestSupport;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(ClearTemporaryStorage.class)
@AutoConfigureMockMvc
@Transactional
class DrugCatalogControllerTest {

    private static final String DIPIRONA =
            "{\"name\":\"Novalgina\",\"activeIngredient\":\"Dipirona\",\"therapeuticClass\":\"Analgesico\",\"controlled\":false}";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AdminTestSupport admin;
    @Autowired
    private LinkTestSupport accounts;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = admin.token();
    }

    private ResultActions create(String body) throws Exception {
        return mockMvc.perform(post("/api/admin/drugs").header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void adminCreatesAndListsDrugs() throws Exception {
        create(DIPIRONA)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Novalgina"))
                .andExpect(jsonPath("$.controlled").value(false))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/api/admin/drugs").header("Authorization", bearer(adminToken)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].activeIngredient").value("Dipirona"));
    }

    @Test
    void duplicateNameIsAConflict() throws Exception {
        create(DIPIRONA).andExpect(status().isCreated());

        create(DIPIRONA.replace("Novalgina", "NOVALGINA")).andExpect(status().isConflict());
    }

    @Test
    void adminMarksADrugAsControlledAndInactive() throws Exception {
        String id = JsonPath.read(create(DIPIRONA).andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(put("/api/admin/drugs/" + id).header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Novalgina\",\"activeIngredient\":\"Dipirona\",\"controlled\":true,\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.controlled").value(true))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void updateOfUnknownDrugIsNotFound() throws Exception {
        mockMvc.perform(put("/api/admin/drugs/" + java.util.UUID.randomUUID())
                        .header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content(DIPIRONA))
                .andExpect(status().isNotFound());
    }

    @Test
    void requiredFieldsAreValidated() throws Exception {
        create("{\"activeIngredient\":\"Dipirona\"}").andExpect(status().isBadRequest());
        create("{\"name\":\"Novalgina\"}").andExpect(status().isBadRequest());
    }

    @Test
    void onlyTheAdministratorManagesTheCatalog() throws Exception {
        Account patient = accounts.patient("paciente@exemplo.com", "11144477735");
        Account doctor = accounts.doctor("dr.paulo@clinica.com", "12345678909", "Dr. Paulo Lima", true);

        for (Account account : new Account[]{patient, doctor}) {
            mockMvc.perform(get("/api/admin/drugs").header("Authorization", bearer(account.token())))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/admin/drugs").header("Authorization", bearer(account.token()))
                            .contentType(MediaType.APPLICATION_JSON).content(DIPIRONA))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/admin/drugs")).andExpect(status().isUnauthorized());
    }
}
