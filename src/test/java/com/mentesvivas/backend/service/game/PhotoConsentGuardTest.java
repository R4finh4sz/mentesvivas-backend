package com.mentesvivas.backend.service.game;

import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhotoConsentGuardTest {

    private static final UUID PATIENT = UUID.randomUUID();

    @Mock
    private UserRepository users;
    @InjectMocks
    private PhotoConsentGuard guard;

    @Test
    void passesWhenTheConsentWasGiven() {
        User patient = new User();
        patient.setHealthDataConsentedAt(Instant.now());
        when(users.findById(PATIENT)).thenReturn(Optional.of(patient));

        assertThatCode(() -> guard.requireConsent(PATIENT)).doesNotThrowAnyException();
    }

    @Test
    void blocksWhenThereIsNoConsent() {
        when(users.findById(PATIENT)).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> guard.requireConsent(PATIENT)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void failsWhenThePatientDoesNotExist() {
        when(users.findById(PATIENT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.requireConsent(PATIENT)).isInstanceOf(ResourceNotFoundException.class);
    }
}
