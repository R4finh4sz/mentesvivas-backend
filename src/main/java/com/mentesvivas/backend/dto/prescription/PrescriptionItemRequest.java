package com.mentesvivas.backend.dto.prescription;

import com.mentesvivas.backend.util.Normalization;
import jakarta.validation.constraints.Size;

public record PrescriptionItemRequest(
        @Size(max = 150, message = "o nome aceita ate 150 caracteres") String medicationName,
        @Size(max = 100, message = "a concentracao ou apresentacao aceita ate 100 caracteres") String presentation,
        @Size(max = 100, message = "a dose aceita ate 100 caracteres") String dose,
        @Size(max = 60, message = "a via aceita ate 60 caracteres") String route,
        @Size(max = 100, message = "a frequencia aceita ate 100 caracteres") String frequency,
        @Size(max = 100, message = "a duracao aceita ate 100 caracteres") String duration,
        @Size(max = 60, message = "a quantidade aceita ate 60 caracteres") String quantity,
        @Size(max = 500, message = "as orientacoes do item aceitam ate 500 caracteres") String instructions,
        Boolean continuousUse,
        @Size(max = 300, message = "a justificativa aceita ate 300 caracteres") String allergyOverrideJustification
) {

    public PrescriptionItemRequest {
        medicationName = clean(medicationName);
        presentation = clean(presentation);
        dose = clean(dose);
        route = clean(route);
        frequency = clean(frequency);
        duration = clean(duration);
        quantity = clean(quantity);
        instructions = clean(instructions);
        allergyOverrideJustification = clean(allergyOverrideJustification);
    }

    public boolean isContinuousUse() {
        return Boolean.TRUE.equals(continuousUse);
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : Normalization.name(value);
    }
}
