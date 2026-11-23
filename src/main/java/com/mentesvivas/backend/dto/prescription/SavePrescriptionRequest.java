package com.mentesvivas.backend.dto.prescription;

import com.mentesvivas.backend.entity.prescription.DocumentKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SavePrescriptionRequest(
        DocumentKind kind,
        @Size(max = 2000, message = "as orientacoes aceitam ate 2000 caracteres") String orientations,
        @Size(max = 30, message = "informe no maximo 30 itens") List<@Valid PrescriptionItemRequest> items,
        Boolean confirmAllergyRisk
) {

    public SavePrescriptionRequest {
        orientations = orientations == null || orientations.isBlank() ? null : orientations.strip();
        items = items == null ? List.of() : List.copyOf(items);
    }

    public DocumentKind kindOrDefault() {
        return kind == null ? DocumentKind.PRESCRIPTION : kind;
    }

    public boolean allergyRiskConfirmed() {
        return Boolean.TRUE.equals(confirmAllergyRisk);
    }
}
