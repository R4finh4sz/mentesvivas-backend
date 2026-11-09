package com.mentesvivas.backend.service.address;

import com.mentesvivas.backend.exception.common.ServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ViaCepLookupTest {

    private MockRestServiceServer server;
    private ViaCepLookup lookup;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://viacep.test/ws");
        server = MockRestServiceServer.bindTo(builder).build();
        lookup = new ViaCepLookup(builder.build());
    }

    @Test
    void mapsTheProviderPayloadToAnAddress() {
        server.expect(requestTo("https://viacep.test/ws/01001000/json/"))
                .andRespond(withSuccess("""
                        {"cep":"01001-000","logradouro":"Praca da Se","complemento":"lado impar",
                         "bairro":"Se","localidade":"Sao Paulo","uf":"SP","ibge":"3550308"}
                        """, MediaType.APPLICATION_JSON));

        assertThat(lookup.find("01001000")).contains(new CepAddress("01001000", "Praca da Se", "Sao Paulo", "SP"));
    }

    @Test
    void unknownCepReturnsEmpty() {
        server.expect(requestTo("https://viacep.test/ws/99999999/json/"))
                .andRespond(withSuccess("{\"erro\":true}", MediaType.APPLICATION_JSON));

        assertThat(lookup.find("99999999")).isEmpty();
    }

    @Test
    void unknownCepWithStringFlagReturnsEmpty() {
        server.expect(requestTo("https://viacep.test/ws/99999999/json/"))
                .andRespond(withSuccess("{\"erro\":\"true\"}", MediaType.APPLICATION_JSON));

        assertThat(lookup.find("99999999")).isEmpty();
    }

    @Test
    void providerFailureBecomesServiceUnavailable() {
        server.expect(requestTo("https://viacep.test/ws/01001000/json/"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> lookup.find("01001000"))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("manualmente");
    }
}
