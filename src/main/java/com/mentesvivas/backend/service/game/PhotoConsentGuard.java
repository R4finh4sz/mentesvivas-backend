package com.mentesvivas.backend.service.game;

import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PhotoConsentGuard {

    private final UserRepository users;

    public void requireConsent(UUID patientId) {
        boolean consented = users.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente nao encontrado."))
                .getHealthDataConsentedAt() != null;
        if (!consented) {
            throw new AccessDeniedException("O paciente ainda nao deu o consentimento para o uso de fotos.");
        }
    }
}
