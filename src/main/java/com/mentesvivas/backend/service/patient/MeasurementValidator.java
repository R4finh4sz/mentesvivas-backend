package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.exception.patient.ImpossibleValueException;
import com.mentesvivas.backend.exception.patient.ValueNeedsConfirmationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MeasurementValidator {

    public void validateWeight(BigDecimal weightKg, boolean confirmed) {
        validate("Peso", "kg", weightKg, MeasurementLimits.WEIGHT_KG, confirmed);
    }

    public void validateHeight(BigDecimal heightM, boolean confirmed) {
        validate("Altura", "m", heightM, MeasurementLimits.HEIGHT_M, confirmed);
    }

    private void validate(String label, String unit, BigDecimal value, MeasurementLimits limits, boolean confirmed) {
        if (limits.isImpossible(value)) {
            throw new ImpossibleValueException("%s invalido: informe um valor real.".formatted(label));
        }
        if (limits.isOutsideExpected(value) && !confirmed) {
            throw new ValueNeedsConfirmationException(
                    "%s fora do esperado (%s a %s %s). Confirme o valor para salvar."
                            .formatted(label, limits.expectedMin().stripTrailingZeros().toPlainString(),
                                    limits.expectedMax().stripTrailingZeros().toPlainString(), unit));
        }
    }
}
