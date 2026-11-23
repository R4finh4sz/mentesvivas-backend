package com.mentesvivas.backend.dto.medication;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AdjustStockRequest(
        @NotNull(message = "informe o novo estoque")
        @DecimalMin(value = "0", message = "o estoque nao pode ser negativo")
        @DecimalMax(value = "100000", message = "o estoque informado e muito alto") BigDecimal quantity,
        @NotBlank(message = "informe a justificativa da correcao")
        @Size(max = 300, message = "a justificativa aceita ate 300 caracteres") String justification
) {
}
