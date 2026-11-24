package com.mentesvivas.backend.dto.patient;

import com.mentesvivas.backend.entity.patient.GlucoseType;
import com.mentesvivas.backend.entity.patient.ClinicalColor;
import com.mentesvivas.backend.entity.patient.MeasurementSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VitalSignResponse(
        UUID id,
        Integer systolic,
        Integer diastolic,
        Integer heartRate,
        Integer glucose,
        GlucoseType glucoseType,
        BigDecimal temperature,
        Integer oxygenSaturation,
        String notes,
        MeasurementSource source,
        boolean checkAgainstOriginal,
        UUID recordedBy,
        Instant measuredAt,
        ClinicalColor clinicalColor,
        UUID clinicalPolicyId
) {
}
