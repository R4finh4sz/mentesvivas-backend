package com.mentesvivas.backend.dto.patient;

import com.mentesvivas.backend.entity.patient.GlucoseType;
import com.mentesvivas.backend.entity.patient.MeasurementSource;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RecordVitalSignRequest(
        Integer systolic,
        Integer diastolic,
        Integer heartRate,
        Integer glucose,
        GlucoseType glucoseType,
        BigDecimal temperature,
        Integer oxygenSaturation,
        @Size(max = 500, message = "as observacoes aceitam ate 500 caracteres") String notes,
        MeasurementSource source
) {

    @AssertTrue(message = "informe ao menos uma afericao")
    public boolean isAnyValueInformed() {
        return systolic != null || diastolic != null || heartRate != null || glucose != null
                || temperature != null || oxygenSaturation != null;
    }

    @AssertTrue(message = "informe a pressao sistolica e a diastolica juntas")
    public boolean isBloodPressureComplete() {
        return (systolic == null) == (diastolic == null);
    }

    @AssertTrue(message = "informe o tipo da glicemia (jejum ou pos-prandial)")
    public boolean isGlucoseTypeConsistent() {
        return (glucose == null) == (glucoseType == null);
    }
}
