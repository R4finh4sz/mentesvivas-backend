package com.mentesvivas.backend.service.address;

import com.mentesvivas.backend.dto.address.AddressRequest;
import com.mentesvivas.backend.dto.address.AddressResponse;
import com.mentesvivas.backend.dto.address.CepLookupResponse;
import com.mentesvivas.backend.entity.address.Address;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.mapper.address.AddressMapper;
import com.mentesvivas.backend.repository.address.AddressRepository;
import com.mentesvivas.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final CurrentUser currentUser;
    private final AddressRepository addressRepository;
    private final CepLookup cepLookup;

    @Transactional(readOnly = true)
    public AddressResponse get() {
        return addressRepository.findByUserId(currentUser.get().getId())
                .map(AddressMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Endereco nao cadastrado."));
    }

    @Transactional
    public AddressResponse save(AddressRequest request) {
        User user = currentUser.get();
        Address address = addressRepository.findByUserId(user.getId()).orElseGet(() -> {
            Address created = new Address();
            created.setUser(user);
            return created;
        });
        address.setCep(request.cep());
        address.setStreet(request.street());
        address.setNumber(request.number());
        address.setComplement(request.complement());
        address.setCity(request.city());
        address.setState(request.state());
        return AddressMapper.toResponse(addressRepository.save(address));
    }

    public CepLookupResponse lookupCep(String cep) {
        return cepLookup.find(cep)
                .map(AddressMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("CEP nao encontrado. Preencha o endereco manualmente."));
    }
}
