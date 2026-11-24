package com.mentesvivas.backend.dto.prescription;

import java.util.UUID;

public record PrescriptionItemResponse(
        UUID id,
        String medicationName,
        String presentation,
        String dose,
        String route,
        String frequency,
        String duration,
        String quantity,
        String instructions,
        boolean continuousUse,
        String allergyOverrideJustification
) {
}
