package com.mentesvivas.backend.controller.user;

import com.jayway.jsonpath.JsonPath;
import com.mentesvivas.backend.support.AdminTestSupport;
import com.mentesvivas.backend.support.ClearTemporaryStorage;
import com.mentesvivas.backend.support.LinkTestSupport;
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
class DataRequestControllerTest {

    private static final String MINE = "/api/users/me/data-requests";
    private static final String QUEUE = "/api/admin/data-requests";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private AdminTestSupport admin;
    @Autowired
    private LinkTestSupport accounts;

    @Test
    void onlyTheOwnerAndTheAdministratorCanTrackTheRequest() throws Exception {
        var patient = accounts.patient("paciente@exemplo.com", "11144477735");
        var caregiver = accounts.caregiver("cuidador@exemplo.com", "52998224725");
        String adminToken = bearer(admin.token());
        String patientToken = bearer(patient.token());
        String opened = mvc.perform(post(MINE).header("Authorization", patientToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"DELETION\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(opened, "$.id");

        mvc.perform(get(MINE).header("Authorization", bearer(caregiver.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get(QUEUE).header("Authorization", patientToken)).andExpect(status().isForbidden());
        mvc.perform(get(QUEUE).param("status", "OPEN").header("Authorization", adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
        update(id, patientToken, "IN_REVIEW").andExpect(status().isForbidden());
        update(id, adminToken, "RESOLVED").andExpect(status().isConflict());
        update(id, adminToken, "IN_REVIEW").andExpect(status().isOk());
        update(id, adminToken, "RESOLVED").andExpect(status().isOk());
        update(id, adminToken, "RESOLVED").andExpect(status().isOk());
        update(id, adminToken, "OPEN").andExpect(status().isConflict());
        mvc.perform(get(MINE).header("Authorization", patientToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("RESOLVED"));
    }

    private ResultActions update(String id, String token, String target) throws Exception {
        return mvc.perform(put(QUEUE + "/" + id + "/status").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"%s\"}".formatted(target)));
    }
}
