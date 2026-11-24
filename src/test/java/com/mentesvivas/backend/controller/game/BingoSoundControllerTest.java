package com.mentesvivas.backend.controller.game;

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
class BingoSoundControllerTest {

    private static final String PIANO = "{\"instrument\":\"Piano\",\"audioUrl\":\"https://audio/piano.mp3\"}";

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
        return mockMvc.perform(post("/api/admin/bingo-sounds").header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void adminAddsAndListsSounds() throws Exception {
        create(PIANO)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.instrument").value("Piano"))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/api/admin/bingo-sounds").header("Authorization", bearer(adminToken)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].audioUrl").value("https://audio/piano.mp3"));
    }

    @Test
    void duplicatedInstrumentIsAConflict() throws Exception {
        create(PIANO).andExpect(status().isCreated());

        create(PIANO.replace("Piano", "PIANO")).andExpect(status().isConflict());
    }

    @Test
    void audioMustBeAnHttpsLink() throws Exception {
        create("{\"instrument\":\"Piano\",\"audioUrl\":\"http://audio/piano.mp3\"}").andExpect(status().isBadRequest());
    }

    @Test
    void instrumentIsRequired() throws Exception {
        create("{\"audioUrl\":\"https://audio/piano.mp3\"}").andExpect(status().isBadRequest());
    }

    @Test
    void adminDeactivatesASound() throws Exception {
        String id = JsonPath.read(create(PIANO).andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(put("/api/admin/bingo-sounds/" + id).header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instrument\":\"Piano\",\"audioUrl\":\"https://audio/piano.mp3\",\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void updatingAnUnknownSoundReturnsNotFound() throws Exception {
        mockMvc.perform(put("/api/admin/bingo-sounds/" + java.util.UUID.randomUUID())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(PIANO))
                .andExpect(status().isNotFound());
    }

    @Test
    void onlyTheAdministratorManagesTheLibrary() throws Exception {
        Account caregiver = accounts.caregiver("cuidador@exemplo.com", "52998224725");

        mockMvc.perform(post("/api/admin/bingo-sounds").header("Authorization", bearer(caregiver.token()))
                        .contentType(MediaType.APPLICATION_JSON).content(PIANO))
                .andExpect(status().isForbidden());
    }
}
