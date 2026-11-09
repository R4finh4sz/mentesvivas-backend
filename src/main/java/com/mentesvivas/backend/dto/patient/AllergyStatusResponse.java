package com.mentesvivas.backend.dto.patient;

import com.mentesvivas.backend.entity.patient.AllergyStatus;

public record AllergyStatusResponse(
        AllergyStatus status
) {
}
