package com.mentesvivas.backend.service.address;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mentesvivas.backend.exception.common.ServiceUnavailableException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

@Component
public class ViaCepLookup implements CepLookup {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ViaCepPayload(
            @JsonProperty("logradouro") String street,
            @JsonProperty("localidade") String city,
            @JsonProperty("uf") String state,
            @JsonProperty("erro") String error
    ) {
    }

    private final RestClient restClient;

    public ViaCepLookup(RestClient cepRestClient) {
        this.restClient = cepRestClient;
    }

    @Override
    public Optional<CepAddress> find(String cep) {
        ViaCepPayload payload;
        try {
            payload = restClient.get().uri("/{cep}/json/", cep).retrieve().body(ViaCepPayload.class);
        } catch (RestClientException ex) {
            throw new ServiceUnavailableException(
                    "Nao foi possivel buscar o CEP agora. Preencha o endereco manualmente.", ex);
        }
        if (payload == null || "true".equals(payload.error())) {
            return Optional.empty();
        }
        return Optional.of(new CepAddress(cep, payload.street(), payload.city(), payload.state()));
    }
}
