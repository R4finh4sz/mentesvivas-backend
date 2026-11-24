package com.mentesvivas.backend.dto.prescription;

import com.mentesvivas.backend.util.Normalization;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveDrugRequest(
        @NotBlank(message = "informe o nome do medicamento")
        @Size(max = 150, message = "o nome aceita ate 150 caracteres") String name,
        @NotBlank(message = "informe o principio ativo")
        @Size(max = 150, message = "o principio ativo aceita ate 150 caracteres") String activeIngredient,
        @Size(max = 100, message = "a classe aceita ate 100 caracteres") String therapeuticClass,
        boolean controlled,
        Boolean active
) {

    public SaveDrugRequest {
        name = Normalization.name(name);
        activeIngredient = Normalization.name(activeIngredient);
        therapeuticClass = therapeuticClass == null || therapeuticClass.isBlank()
                ? null : Normalization.name(therapeuticClass);
    }

    public boolean isActive() {
        return active == null || active;
    }
}
