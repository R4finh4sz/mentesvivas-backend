package com.mentesvivas.backend.dto.alert;

import com.mentesvivas.backend.dto.patient.AllergyResponse;
import com.mentesvivas.backend.dto.patient.EmergencyContactResponse;

import java.util.List;

public record AlertDetailResponse(
        AlertResponse alert,
        List<AllergyResponse> severeAllergies,
        List<EmergencyContactResponse> emergencyContacts
) {
}
