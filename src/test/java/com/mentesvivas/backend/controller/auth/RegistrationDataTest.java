package com.mentesvivas.backend.controller.auth;

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
class RegistrationDataTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthTestSupport auth;

    @Autowired
    private TermsTestSupport terms;

    private int termsVersion;

    @BeforeEach
    void publishTerms() {
        termsVersion = terms.publish();
    }

    private ResultActions register(String role, String extras) throws Exception {
        return mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"fullName":"Pessoa Teste","cpf":"52998224725","birthDate":"1950-03-10",
                 "phone":"11999998888","email":"dados@exemplo.com","password":"%s","passwordConfirmation":"%s",
                 "role":"%s","termsVersion":%d,"acceptTerms":true,"acceptHealthData":true%s}
                """.formatted(AuthTestSupport.PASSWORD, AuthTestSupport.PASSWORD, role, termsVersion, extras)));
    }

    @Test
    void patientRegistrationSavesTheAddressAndTheEmergencyContacts() throws Exception {
        register("PATIENT", AuthTestSupport.registrationExtras("PATIENT")).andExpect(status().isCreated());
        String token = auth.login("dados@exemplo.com");

        mockMvc.perform(get("/api/users/me/address").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("SP"));
        mockMvc.perform(get("/api/patients/me/emergency-contacts").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].order").value(1));
        mockMvc.perform(get("/api/patients/me/profile").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.completeness.missing.length()").value(3));
    }

    @Test
    void caregiverRegistrationSavesTheAddress() throws Exception {
        register("CAREGIVER", AuthTestSupport.registrationExtras("CAREGIVER")).andExpect(status().isCreated());
        String token = auth.login("dados@exemplo.com");

        mockMvc.perform(get("/api/users/me/address").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cep").value("01001000"));
    }

    @Test
    void registrationWithoutAddressReturns400() throws Exception {
        register("CAREGIVER", "").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.address").exists());
    }

    @Test
    void patientRegistrationWithoutContactsReturns400() throws Exception {
        register("PATIENT", AuthTestSupport.ADDRESS_JSON).andExpect(status().isBadRequest());
    }

    @Test
    void patientRegistrationWithOneContactReturns400() throws Exception {
        String oneContact = AuthTestSupport.ADDRESS_JSON
                + ",\"emergencyContacts\":[{\"name\":\"Ana\",\"relationship\":\"Filha\",\"phone\":\"11977776666\",\"order\":1}]";

        register("PATIENT", oneContact).andExpect(status().isBadRequest());
    }

    @Test
    void caregiverRegistrationWithContactsReturns400() throws Exception {
        register("CAREGIVER", AuthTestSupport.ADDRESS_JSON + AuthTestSupport.CONTACTS_JSON)
                .andExpect(status().isBadRequest());
    }

    @Test
    void failedRegistrationDoesNotCreateTheAccount() throws Exception {
        register("PATIENT", AuthTestSupport.ADDRESS_JSON).andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dados@exemplo.com\",\"password\":\"%s\"}".formatted(AuthTestSupport.PASSWORD)))
                .andExpect(status().isUnauthorized());
    }
}
