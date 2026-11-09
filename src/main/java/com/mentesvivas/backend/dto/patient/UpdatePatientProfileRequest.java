package com.mentesvivas.backend.dto.patient;

import com.mentesvivas.backend.entity.patient.BloodType;
import com.mentesvivas.backend.util.Normalization;
import jakarta.validation.constraints.Size;

public record UpdatePatientProfileRequest(
        BloodType bloodType,
        @Size(max = 300) String declaredDisease
) {

    public UpdatePatientProfileRequest {
        declaredDisease = Normalization.name(declaredDisease);
    }
}
