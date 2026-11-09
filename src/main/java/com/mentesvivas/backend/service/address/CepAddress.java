package com.mentesvivas.backend.service.address;

public record CepAddress(
        String cep,
        String street,
        String city,
        String state
) {
}
