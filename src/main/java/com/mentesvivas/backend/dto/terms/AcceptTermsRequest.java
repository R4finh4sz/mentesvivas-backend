package com.mentesvivas.backend.dto.terms;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record AcceptTermsRequest(
        @NotNull Integer version,
        @AssertTrue(message = "e necessario aceitar os Termos de Uso e a Politica de Privacidade") boolean acceptTerms
) {
}
