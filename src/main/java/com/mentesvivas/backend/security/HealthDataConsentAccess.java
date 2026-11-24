package com.mentesvivas.backend.security;

import com.mentesvivas.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HealthDataConsentAccess {

    private final UserRepository users;

    public void requireActive(UUID patientId) {
        if (users.existsByIdAndHealthDataConsentRevokedAtIsNotNull(patientId)) {
            throw new AccessDeniedException("O paciente precisa aceitar novamente o consentimento para dados de saúde.");
        }
    }
}
