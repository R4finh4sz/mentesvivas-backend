package com.mentesvivas.backend.controller.address;

import com.mentesvivas.backend.exception.common.ServiceUnavailableException;
import com.mentesvivas.backend.service.address.CepAddress;
import com.mentesvivas.backend.service.address.CepLookup;
import com.mentesvivas.backend.support.AuthTestSupport;
import com.mentesvivas.backend.support.ClearTemporaryStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.mentesvivas.backend.support.AuthTestSupport.bearer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(ClearTemporaryStorage.class)
@AutoConfigureMockMvc
@Transactional
class AddressControllerTest {

    private static final String ADDRESS = """
            {"cep":"01001-000","street":"Praca da Se","number":"10","complement":"Apto 2",
             "city":"Sao Paulo","state":"sp"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthTestSupport auth;

    @MockitoBean
    private CepLookup cepLookup;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = auth.registerAndLogin("nina@exemplo.com", "52998224725", "CAREGIVER");
    }

    @Test
    void addressIsNotFoundBeforeBeingSaved() throws Exception {
        mockMvc.perform(get("/api/users/me/address").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void saveNormalizesAndGetReturnsTheAddress() throws Exception {
        save(ADDRESS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cep").value("01001000"))
                .andExpect(jsonPath("$.state").value("SP"));

        mockMvc.perform(get("/api/users/me/address").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.street").value("Praca da Se"))
                .andExpect(jsonPath("$.complement").value("Apto 2"));
    }

    @Test
    void savingAgainUpdatesTheSameAddress() throws Exception {
        save(ADDRESS).andExpect(status().isOk());
        save(ADDRESS.replace("Sao Paulo", "Santos")).andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me/address").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.city").value("Santos"));
    }

    @Test
    void invalidAddressIsRejected() throws Exception {
        save(ADDRESS.replace("01001-000", "123")).andExpect(status().isBadRequest());
        save(ADDRESS.replace("\"sp\"", "\"XX\"")).andExpect(status().isBadRequest());
        save(ADDRESS.replace("Praca da Se", "  ")).andExpect(status().isBadRequest());
    }

    @Test
    void addressRoutesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me/address")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/addresses/cep/01001000")).andExpect(status().isUnauthorized());
    }

    @Test
    void cepLookupFillsTheAddress() throws Exception {
        when(cepLookup.find("01001000"))
                .thenReturn(Optional.of(new CepAddress("01001000", "Praca da Se", "Sao Paulo", "SP")));

        mockMvc.perform(get("/api/addresses/cep/01001000").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.street").value("Praca da Se"))
                .andExpect(jsonPath("$.city").value("Sao Paulo"));
    }

    @Test
    void unknownCepAsksForManualFilling() throws Exception {
        when(cepLookup.find("99999999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/addresses/cep/99999999").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void providerOutageReturnsServiceUnavailableWithManualFallbackMessage() throws Exception {
        when(cepLookup.find("01001000")).thenThrow(new ServiceUnavailableException(
                "Nao foi possivel buscar o CEP agora. Preencha o endereco manualmente.", null));

        mockMvc.perform(get("/api/addresses/cep/01001000").header("Authorization", bearer(token)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("Nao foi possivel buscar o CEP agora. Preencha o endereco manualmente."));
    }

    @Test
    void malformedCepIsRejected() throws Exception {
        mockMvc.perform(get("/api/addresses/cep/123").header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
    }

    private ResultActions save(String json) throws Exception {
        return mockMvc.perform(put("/api/users/me/address").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
