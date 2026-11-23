package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.config.VitalSignLimitsProperties;
import com.mentesvivas.backend.config.VitalSignLimitsProperties.Range;
import com.mentesvivas.backend.dto.patient.RecordVitalSignRequest;
import com.mentesvivas.backend.exception.patient.ImpossibleValueException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class VitalSignValidator {

    private final VitalSignLimitsProperties limits;

    public void validate(RecordVitalSignRequest request) {
        check("Pressao sistolica", "mmHg", request.systolic(), limits.systolic());
        check("Pressao diastolica", "mmHg", request.diastolic(), limits.diastolic());
        check("Frequencia cardiaca", "bpm", request.heartRate(), limits.heartRate());
        check("Glicemia", "mg/dL", request.glucose(), limits.glucose());
        check("Temperatura", "C", request.temperature(), limits.temperature());
        check("Oxigenacao", "%", request.oxygenSaturation(), limits.oxygenSaturation());
        if (request.systolic() != null && request.systolic() <= request.diastolic()) {
            throw new ImpossibleValueException("A pressao sistolica deve ser maior que a diastolica.");
        }
    }

    private void check(String label, String unit, Number value, Range range) {
        if (value == null) {
            return;
        }
        if (!range.contains(new BigDecimal(value.toString()))) {
            throw new ImpossibleValueException("%s invalida: aceitamos de %s a %s %s."
                    .formatted(label, range.min().stripTrailingZeros().toPlainString(),
                            range.max().stripTrailingZeros().toPlainString(), unit));
        }
    }
}
