package com.mentesvivas.backend.dto.address;

public record AddressResponse(
        String cep,
        String street,
        String number,
        String complement,
        String city,
        String state
) {
}
