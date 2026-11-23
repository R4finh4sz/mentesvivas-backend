package com.mentesvivas.backend.service.patient.routine;

import com.mentesvivas.backend.exception.patient.ImpossibleValueException;

import java.math.BigDecimal;

final class RoutineChecks {

    private RoutineChecks() {
    }

    static void required(Object value, String label) {
        if (value == null) {
            throw new ImpossibleValueException("Informe " + label + ".");
        }
    }

    static void range(Integer value, int min, int max, String label) {
        required(value, label);
        if (value < min || value > max) {
            throw new ImpossibleValueException("%s invalido: aceitamos de %d a %d.".formatted(label, min, max));
        }
    }

    static void range(BigDecimal value, BigDecimal min, BigDecimal max, String label) {
        required(value, label);
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new ImpossibleValueException("%s invalido: aceitamos de %s a %s."
                    .formatted(label, min.toPlainString(), max.toPlainString()));
        }
    }
}
