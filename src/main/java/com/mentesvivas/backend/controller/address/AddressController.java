package com.mentesvivas.backend.controller.address;

import com.mentesvivas.backend.dto.address.AddressRequest;
import com.mentesvivas.backend.dto.address.AddressResponse;
import com.mentesvivas.backend.dto.address.CepLookupResponse;
import com.mentesvivas.backend.service.address.AddressService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping("/api/users/me/address")
    public AddressResponse get() {
        return addressService.get();
    }

    @PutMapping("/api/users/me/address")
    public AddressResponse save(@Valid @RequestBody AddressRequest body) {
        return addressService.save(body);
    }

    @GetMapping("/api/addresses/cep/{cep}")
    public CepLookupResponse lookupCep(
            @PathVariable @Pattern(regexp = "^\\d{8}$", message = "o CEP possui oito digitos") String cep) {
        return addressService.lookupCep(cep);
    }
}
