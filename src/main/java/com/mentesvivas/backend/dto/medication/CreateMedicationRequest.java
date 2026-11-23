package com.mentesvivas.backend.dto.medication;

import com.mentesvivas.backend.util.Normalization;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public record CreateMedicationRequest(
        @NotBlank(message = "informe o nome do medicamento")
        @Size(max = 150, message = "o nome aceita ate 150 caracteres") String name,
        @Size(max = 100, message = "a concentracao aceita ate 100 caracteres") String strength,
        @NotNull(message = "informe a quantidade por dose")
        @DecimalMin(value = "0.01", message = "a quantidade por dose deve ser maior que zero")
        @DecimalMax(value = "100", message = "a quantidade por dose e muito alta") BigDecimal unitsPerDose,
        @NotEmpty(message = "informe ao menos um horario")
        @Size(max = 24, message = "informe no maximo 24 horarios") List<@NotNull LocalTime> times,
        @NotNull(message = "informe o estoque atual")
        @DecimalMin(value = "0", message = "o estoque nao pode ser negativo")
        @DecimalMax(value = "100000", message = "o estoque informado e muito alto") BigDecimal stockQuantity,
        Boolean confirmAllergyRisk
) {

    public CreateMedicationRequest {
        name = Normalization.name(name);
        strength = strength == null || strength.isBlank() ? null : Normalization.name(strength);
    }

    public boolean allergyRiskConfirmed() {
        return Boolean.TRUE.equals(confirmAllergyRisk);
    }
}
