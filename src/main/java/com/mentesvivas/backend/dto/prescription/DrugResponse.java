package com.mentesvivas.backend.dto.prescription;

import java.util.UUID;

public record DrugResponse(
        UUID id,
        String name,
        String activeIngredient,
        String therapeuticClass,
        boolean controlled,
        boolean active
) {
}
