package com.mentesvivas.backend.service.prescription;

import com.mentesvivas.backend.config.PrescriptionProperties;
import com.mentesvivas.backend.dto.prescription.PrescriptionItemResponse;
import com.mentesvivas.backend.dto.prescription.PrescriptionResponse;
import com.mentesvivas.backend.dto.prescription.PrescriptionWarning;
import com.mentesvivas.backend.entity.patient.AllergyStatus;
import com.mentesvivas.backend.entity.patient.PatientProfile;
import com.mentesvivas.backend.entity.prescription.Prescription;
import com.mentesvivas.backend.entity.prescription.PrescriptionItem;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.repository.patient.PatientProfileRepository;
import com.mentesvivas.backend.repository.prescription.PrescriptionItemRepository;
import com.mentesvivas.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PrescriptionViewFactory {

    private final PrescriptionItemRepository itemRepository;
    private final UserRepository userRepository;
    private final PatientProfileRepository profileRepository;
    private final PrescriptionCompleteness completeness;
    private final PrescriptionProperties properties;
    private final Clock clock;

    public PrescriptionResponse build(Prescription prescription) {
        List<PrescriptionItem> items = itemRepository.findByPrescriptionIdOrderByPositionAsc(prescription.getId());
        User patient = userRepository.findById(prescription.getPatientId()).orElseThrow();
        User doctor = userRepository.findById(prescription.getDoctorId()).orElseThrow();
        boolean signed = !prescription.isDraft();
        return new PrescriptionResponse(prescription.getId(), patient.getId(), patient.getFullName(), doctor.getId(),
                doctor.getFullName(), "CRM %s/%s".formatted(doctor.getCrm(), doctor.getCrmState()),
                prescription.getKind(), prescription.getStatus(), prescription.getOrientations(),
                items.stream().map(PrescriptionViewFactory::toResponse).toList(),
                signed ? List.of() : completeness.missingFields(prescription, items),
                warnings(prescription, items), prescription.getCreatedAt(), prescription.getSignedAt(),
                prescription.getValidUntil(), prescription.isExpiredAt(clock.instant()), prescription.isReplaced(),
                prescription.getReplacesId(), prescription.getReplacementJustification(),
                prescription.getReplacedById(), prescription.getVerificationCode(), verificationUrl(prescription),
                prescription.isSignatureSimulated(), prescription.getLastSignatureError());
    }

    public String verificationUrl(Prescription prescription) {
        if (prescription.getVerificationCode() == null) {
            return null;
        }
        return properties.verificationBaseUrl() + prescription.getVerificationCode();
    }

    private List<PrescriptionWarning> warnings(Prescription prescription, List<PrescriptionItem> items) {
        List<PrescriptionWarning> warnings = new ArrayList<>();
        AllergyStatus status = profileRepository.findById(prescription.getPatientId())
                .map(PatientProfile::getAllergyStatus).orElse(AllergyStatus.NOT_INFORMED);
        if (status == AllergyStatus.NOT_INFORMED) {
            warnings.add(PrescriptionWarning.ALLERGY_NOT_INFORMED);
        }
        if (items.stream().anyMatch(item -> item.getAllergyOverrideJustification() != null)) {
            warnings.add(PrescriptionWarning.ALLERGY_OVERRIDDEN);
        }
        return warnings;
    }

    private static PrescriptionItemResponse toResponse(PrescriptionItem item) {
        return new PrescriptionItemResponse(item.getId(), item.getMedicationName(), item.getPresentation(),
                item.getDose(), item.getRoute(), item.getFrequency(), item.getDuration(), item.getQuantity(),
                item.getInstructions(), item.isContinuousUse(), item.getAllergyOverrideJustification());
    }
}
