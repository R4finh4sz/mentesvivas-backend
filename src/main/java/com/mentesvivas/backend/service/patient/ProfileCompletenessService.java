package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.dto.patient.CompletenessResponse;
import com.mentesvivas.backend.entity.patient.AllergyStatus;
import com.mentesvivas.backend.entity.patient.MissingItem;
import com.mentesvivas.backend.repository.patient.BodyMeasurementRepository;
import com.mentesvivas.backend.repository.patient.EmergencyContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProfileCompletenessService {

    private static final int MIN_EMERGENCY_CONTACTS = 2;

    private final BodyMeasurementRepository measurementRepository;
    private final EmergencyContactRepository contactRepository;

    public CompletenessResponse evaluate(UUID patientId, AllergyStatus allergyStatus) {
        List<MissingItem> missing = new ArrayList<>();
        if (allergyStatus == AllergyStatus.NOT_INFORMED) {
            missing.add(MissingItem.ALLERGY_STATUS);
        }
        if (measurementRepository.findFirstByPatientIdAndWeightKgIsNotNullOrderByMeasuredAtDesc(patientId).isEmpty()) {
            missing.add(MissingItem.WEIGHT);
        }
        if (measurementRepository.findFirstByPatientIdAndHeightMIsNotNullOrderByMeasuredAtDesc(patientId).isEmpty()) {
            missing.add(MissingItem.HEIGHT);
        }
        boolean modulesUnlocked = missing.isEmpty();
        if (contactRepository.countByPatientId(patientId) < MIN_EMERGENCY_CONTACTS) {
            missing.add(MissingItem.EMERGENCY_CONTACTS);
        }
        return new CompletenessResponse(modulesUnlocked, List.copyOf(missing));
    }
}
