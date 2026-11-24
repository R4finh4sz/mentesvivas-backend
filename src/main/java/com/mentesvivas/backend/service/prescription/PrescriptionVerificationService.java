package com.mentesvivas.backend.service.prescription;

import com.mentesvivas.backend.dto.prescription.VerificationResponse;
import com.mentesvivas.backend.dto.prescription.VerificationStatus;
import com.mentesvivas.backend.entity.prescription.Prescription;
import com.mentesvivas.backend.entity.prescription.PrescriptionStatus;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.prescription.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class PrescriptionVerificationService {

    private final PrescriptionRepository repository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public VerificationResponse verify(String code) {
        Prescription prescription = repository.findByVerificationCodeAndStatus(code, PrescriptionStatus.SIGNED)
                .orElseThrow(() -> new ResourceNotFoundException("Documento nao encontrado."));
        return new VerificationResponse(statusOf(prescription));
    }

    private VerificationStatus statusOf(Prescription prescription) {
        if (prescription.isReplaced()) {
            return VerificationStatus.REPLACED;
        }
        if (prescription.isExpiredAt(clock.instant())) {
            return VerificationStatus.EXPIRED;
        }
        return VerificationStatus.VALID;
    }
}
