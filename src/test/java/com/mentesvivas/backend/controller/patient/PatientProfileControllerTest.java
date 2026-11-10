package com.mentesvivas.backend.controller.patient;

import com.mentesvivas.backend.support.AuthTestSupport;
import com.mentesvivas.backend.support.ClearTemporaryStorage;
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
class PatientProfileControllerTest {

    private static final String BASE = "/api/patients/me";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthTestSupport auth;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = auth.registerAndLogin("paciente@exemplo.com", "52998224725", "PATIENT");
    }

    @Test
    void routesAreRestrictedToPatients() throws Exception {
        String caregiver = auth.registerAndLogin("cuidador@exemplo.com", "11144477735", "CAREGIVER");

        mockMvc.perform(get(BASE + "/profile")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/profile").header("Authorization", bearer(caregiver))).andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/allergies").header("Authorization", bearer(caregiver))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }

    @Test
    void newProfileHasEverythingMissingAndModulesLocked() throws Exception {
        profile()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allergyStatus").value("NOT_INFORMED"))
                .andExpect(jsonPath("$.declaredDiseaseValidated").value(false))
                .andExpect(jsonPath("$.completeness.modulesUnlocked").value(false))
                .andExpect(jsonPath("$.completeness.missing[0]").value("ALLERGY_STATUS"))
                .andExpect(jsonPath("$.completeness.missing.length()").value(4));
    }

    @Test
    void addingAnAllergyMarksItInformedAndLogsAuthorAndTime() throws Exception {
        send(post(BASE + "/allergies"), "{\"category\":\"MEDICATION\",\"description\":\"Dipirona\",\"severity\":\"SEVERE\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.severity").value("SEVERE"));

        profile()
                .andExpect(jsonPath("$.allergyStatus").value("INFORMED"))
                .andExpect(jsonPath("$.allergies[0].description").value("Dipirona"));
        fetch(BASE + "/changes")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].field").value("ALLERGY"))
                .andExpect(jsonPath("$[0].changeType").value("CREATED"))
                .andExpect(jsonPath("$[0].changedBy").isNotEmpty())
                .andExpect(jsonPath("$[0].changedAt").isNotEmpty());
    }

    @Test
    void invalidAllergyIsRejected() throws Exception {
        send(post(BASE + "/allergies"), "{\"category\":\"MEDICATION\",\"description\":\" \",\"severity\":\"SEVERE\"}")
                .andExpect(status().isBadRequest());
        send(post(BASE + "/allergies"), "{\"category\":\"DUST\",\"description\":\"Po\",\"severity\":\"MILD\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void noKnownAllergiesCanBeDeclaredOnlyWhileThereAreNone() throws Exception {
        send(put(BASE + "/allergy-status"), "{\"status\":\"NONE_KNOWN\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NONE_KNOWN"));

        send(post(BASE + "/allergies"), "{\"category\":\"FOOD\",\"description\":\"Amendoim\",\"severity\":\"MILD\"}")
                .andExpect(status().isCreated());
        send(put(BASE + "/allergy-status"), "{\"status\":\"NONE_KNOWN\"}").andExpect(status().isConflict());
    }

    @Test
    void informedStatusCannotBeSetDirectly() throws Exception {
        send(put(BASE + "/allergy-status"), "{\"status\":\"INFORMED\"}").andExpect(status().isBadRequest());
    }

    @Test
    void measurementInsideTheExpectedRangeIsSavedAndShowsInTheProfile() throws Exception {
        send(post(BASE + "/measurements"), "{\"weightKg\":72.46,\"heightM\":1.678}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.weightKg").value(72.5))
                .andExpect(jsonPath("$.heightM").value(1.68));

        profile().andExpect(jsonPath("$.weightKg").value(72.5)).andExpect(jsonPath("$.heightM").value(1.68))
                .andExpect(jsonPath("$.bmi").value(25.7));
    }

    @Test
    void bmiIsEmptyUntilWeightAndHeightAreRecorded() throws Exception {
        send(post(BASE + "/measurements"), "{\"weightKg\":70}").andExpect(status().isCreated());

        profile().andExpect(jsonPath("$.bmi").isEmpty());
    }

    @Test
    void unexpectedButPlausibleValueNeedsConfirmation() throws Exception {
        send(post(BASE + "/measurements"), "{\"weightKg\":25}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.requiresConfirmation").value(true))
                .andExpect(jsonPath("$.detail").value("Peso fora do esperado (30 a 300 kg). Confirme o valor para salvar."));

        send(post(BASE + "/measurements"), "{\"weightKg\":25,\"confirmOutOfRange\":true}")
                .andExpect(status().isCreated());
    }

    @Test
    void impossibleValueIsBlockedEvenWithConfirmation() throws Exception {
        send(post(BASE + "/measurements"), "{\"heightM\":4.5,\"confirmOutOfRange\":true}")
                .andExpect(status().isBadRequest());
        send(post(BASE + "/measurements"), "{\"weightKg\":2,\"confirmOutOfRange\":true}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void measurementWithoutValuesIsRejectedAndHistoryKeepsEveryRecord() throws Exception {
        send(post(BASE + "/measurements"), "{}").andExpect(status().isBadRequest());

        send(post(BASE + "/measurements"), "{\"weightKg\":70,\"heightM\":1.65}").andExpect(status().isCreated());
        send(post(BASE + "/measurements"), "{\"weightKg\":68.5}").andExpect(status().isCreated());

        fetch(BASE + "/measurements")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        profile().andExpect(jsonPath("$.weightKg").value(68.5)).andExpect(jsonPath("$.heightM").value(1.65));
    }

    @Test
    void emergencyContactsRequireAtLeastTwoWithDistinctOrder() throws Exception {
        send(put(BASE + "/emergency-contacts"),
                "{\"contacts\":[{\"name\":\"Ana\",\"relationship\":\"Filha\",\"phone\":\"11988887777\",\"order\":1}]}")
                .andExpect(status().isBadRequest());
        send(put(BASE + "/emergency-contacts"), contacts(1, 1)).andExpect(status().isBadRequest());
        send(put(BASE + "/emergency-contacts"), contacts(0, 1)).andExpect(status().isBadRequest());
    }

    @Test
    void replacingEmergencyContactsKeepsOnlyTheLatestListSortedByOrder() throws Exception {
        send(put(BASE + "/emergency-contacts"), contacts(2, 1)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].order").value(1))
                .andExpect(jsonPath("$[1].order").value(2));
        send(put(BASE + "/emergency-contacts"), contacts(1, 2)).andExpect(status().isOk());

        fetch(BASE + "/emergency-contacts").andExpect(jsonPath("$.length()").value(2));
        fetch(BASE + "/changes").andExpect(jsonPath("$[0].field").value("EMERGENCY_CONTACTS"));
    }

    @Test
    void completeProfileUnlocksTheModules() throws Exception {
        send(put(BASE + "/allergy-status"), "{\"status\":\"NONE_KNOWN\"}").andExpect(status().isOk());
        send(post(BASE + "/measurements"), "{\"weightKg\":70,\"heightM\":1.65}").andExpect(status().isCreated());
        send(put(BASE + "/emergency-contacts"), contacts(1, 2)).andExpect(status().isOk());

        profile()
                .andExpect(jsonPath("$.completeness.modulesUnlocked").value(true))
                .andExpect(jsonPath("$.completeness.missing.length()").value(0));
    }

    @Test
    void declaredDiseaseStaysNotValidatedAndIsLogged() throws Exception {
        send(put(BASE + "/profile"), "{\"bloodType\":\"O_POSITIVE\",\"declaredDisease\":\"Alzheimer inicial\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bloodType").value("O_POSITIVE"))
                .andExpect(jsonPath("$.declaredDisease").value("Alzheimer inicial"))
                .andExpect(jsonPath("$.declaredDiseaseValidated").value(false));

        fetch(BASE + "/changes").andExpect(jsonPath("$[0].field").value("DECLARED_DISEASE"));
    }

    @Test
    void invalidBloodTypeIsRejected() throws Exception {
        send(put(BASE + "/profile"), "{\"bloodType\":\"C_POSITIVE\"}").andExpect(status().isBadRequest());
    }

    private String contacts(int firstOrder, int secondOrder) {
        return """
                {"contacts":[{"name":"Ana","relationship":"Filha","phone":"11988887777","order":%d},
                             {"name":"Bruno","relationship":"Filho","phone":"11977776666","order":%d}]}
                """.formatted(firstOrder, secondOrder);
    }

    private ResultActions profile() throws Exception {
        return fetch(BASE + "/profile");
    }

    private ResultActions fetch(String url) throws Exception {
        return mockMvc.perform(get(url)
                .header("Authorization", bearer(token)));
    }

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder,
                               String json) throws Exception {
        return mockMvc.perform(builder.header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
