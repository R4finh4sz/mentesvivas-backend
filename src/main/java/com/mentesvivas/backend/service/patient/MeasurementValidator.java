package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.config.MeasurementProperties;
import com.mentesvivas.backend.exception.patient.ImpossibleValueException;
import com.mentesvivas.backend.exception.patient.ValueNeedsConfirmationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class MeasurementValidator {

    private final MeasurementProperties properties;

    public void validateWeight(BigDecimal weightKg, boolean confirmed) {
        validate("Peso", "kg", weightKg, properties.weight(), confirmed);
    }

    public void validateHeight(BigDecimal heightM, boolean confirmed) {
        validate("Altura", "m", heightM, properties.height(), confirmed);
    }

    private void validate(String label, String unit, BigDecimal value,
                          MeasurementProperties.Limits limits, boolean confirmed) {
        if (value.compareTo(limits.possibleMin()) < 0 || value.compareTo(limits.possibleMax()) > 0) {
            throw new ImpossibleValueException("%s invalido: informe um valor real.".formatted(label));
        }
        boolean outsideExpected = value.compareTo(limits.expectedMin()) < 0 || value.compareTo(limits.expectedMax()) > 0;
        if (outsideExpected && !confirmed) {
            throw new ValueNeedsConfirmationException(
                    "%s fora do esperado (%s a %s %s). Confirme o valor para salvar."
                            .formatted(label, limits.expectedMin().stripTrailingZeros().toPlainString(),
                                    limits.expectedMax().stripTrailingZeros().toPlainString(), unit));
        }
    }
}
