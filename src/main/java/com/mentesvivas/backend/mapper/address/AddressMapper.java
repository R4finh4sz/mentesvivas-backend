package com.mentesvivas.backend.mapper.address;

import com.mentesvivas.backend.dto.address.AddressResponse;
import com.mentesvivas.backend.dto.address.CepLookupResponse;
import com.mentesvivas.backend.entity.address.Address;
import com.mentesvivas.backend.service.address.CepAddress;

public final class AddressMapper {

    private AddressMapper() {
    }

    public static AddressResponse toResponse(Address address) {
        return new AddressResponse(address.getCep(), address.getStreet(), address.getNumber(),
                address.getComplement(), address.getCity(), address.getState());
    }

    public static CepLookupResponse toResponse(CepAddress cepAddress) {
        return new CepLookupResponse(cepAddress.cep(), cepAddress.street(), cepAddress.city(), cepAddress.state());
    }
}
