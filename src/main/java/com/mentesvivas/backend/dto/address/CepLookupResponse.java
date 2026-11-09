package com.mentesvivas.backend.dto.address;

public record CepLookupResponse(
        String cep,
        String street,
        String city,
        String state
) {
}
