package com.mentesvivas.backend.service.address;

import com.mentesvivas.backend.dto.address.AddressRequest;
import com.mentesvivas.backend.entity.address.Address;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.address.AddressRepository;
import com.mentesvivas.backend.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private CurrentUser currentUser;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private CepLookup cepLookup;

    private AddressService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new AddressService(currentUser, addressRepository, cepLookup);
        user = new User() {
            @Override
            public UUID getId() {
                return USER_ID;
            }
        };
    }

    private AddressRequest request() {
        return new AddressRequest("01001-000", "Praca da Se", "10", "Apto 2", "Sao Paulo", "sp");
    }

    @Test
    void getWithoutAddressReturnsNotFound() {
        when(currentUser.get()).thenReturn(user);
        when(addressRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get()).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void saveCreatesTheAddressForTheCurrentUser() {
        when(currentUser.get()).thenReturn(user);
        when(addressRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.save(request());

        assertThat(response.cep()).isEqualTo("01001000");
        assertThat(response.state()).isEqualTo("SP");
        verify(addressRepository).save(any(Address.class));
    }

    @Test
    void saveUpdatesTheExistingAddress() {
        Address existing = new Address();
        existing.setUser(user);
        existing.setCity("Rio de Janeiro");
        when(currentUser.get()).thenReturn(user);
        when(addressRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(addressRepository.save(existing)).thenReturn(existing);

        service.save(request());

        assertThat(existing.getCity()).isEqualTo("Sao Paulo");
        assertThat(existing.getComplement()).isEqualTo("Apto 2");
    }

    @Test
    void lookupCepReturnsTheFoundAddress() {
        when(cepLookup.find("01001000")).thenReturn(Optional.of(new CepAddress("01001000", "Praca da Se", "Sao Paulo", "SP")));

        assertThat(service.lookupCep("01001000").street()).isEqualTo("Praca da Se");
    }

    @Test
    void lookupCepNotFoundAsksForManualFilling() {
        when(cepLookup.find("99999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.lookupCep("99999999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("manualmente");
    }
}
