package com.mentesvivas.backend.dto.clinical;

import com.mentesvivas.backend.dto.medication.MedicationResponse;
import com.mentesvivas.backend.dto.patient.AllergyResponse;
import com.mentesvivas.backend.dto.patient.CareTeamResponse;
import com.mentesvivas.backend.dto.patient.EmergencyContactResponse;
import com.mentesvivas.backend.entity.patient.AllergyStatus;
import com.mentesvivas.backend.entity.patient.BloodType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ClinicalSummaryResponse(
        UUID patientId,
        String fullName,
        LocalDate birthDate,
        String phone,
        String email,
        String photoUrl,
        BloodType bloodType,
        BigDecimal weightKg,
        BigDecimal heightM,
        BigDecimal bmi,
        String declaredDisease,
        boolean declaredDiseaseValidated,
        AllergyStatus allergyStatus,
        List<AllergyResponse> allergies,
        List<MedicationResponse> medications,
        List<EmergencyContactResponse> emergencyContacts,
        CareTeamResponse careTeam
) {
}
