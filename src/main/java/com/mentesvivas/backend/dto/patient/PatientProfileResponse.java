package com.mentesvivas.backend.dto.patient;

import com.mentesvivas.backend.entity.patient.AllergyStatus;
import com.mentesvivas.backend.entity.patient.BloodType;

import java.math.BigDecimal;
import java.util.List;

public record PatientProfileResponse(
        BloodType bloodType,
        String declaredDisease,
        boolean declaredDiseaseValidated,
        AllergyStatus allergyStatus,
        List<AllergyResponse> allergies,
        BigDecimal weightKg,
        BigDecimal heightM,
        BigDecimal bmi,
        List<EmergencyContactResponse> emergencyContacts,
        CompletenessResponse completeness
) {
}
