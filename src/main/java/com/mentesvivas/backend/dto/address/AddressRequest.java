package com.mentesvivas.backend.dto.address;

import com.mentesvivas.backend.util.BrazilianState;
import com.mentesvivas.backend.util.Normalization;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank @Pattern(regexp = "^\\d{8}$", message = "o CEP possui oito digitos") String cep,
        @NotBlank @Size(max = 200) String street,
        @NotBlank @Size(max = 20) String number,
        @Size(max = 100) String complement,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Pattern(regexp = BrazilianState.REGEX, message = "UF invalida") String state
) {

    public AddressRequest {
        cep = Normalization.cep(cep);
        street = Normalization.name(street);
        number = Normalization.name(number);
        complement = Normalization.name(complement);
        city = Normalization.name(city);
        state = Normalization.state(state);
    }
}
