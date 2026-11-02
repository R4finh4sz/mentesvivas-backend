package com.mentesvivas.backend.dto.termos;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record AceitarTermosRequest(
        @NotNull Integer versao,
        @AssertTrue(message = "e necessario aceitar os Termos de Uso e a Politica de Privacidade") boolean aceiteTermos
) {
}
