package com.mentesvivas.backend.dto.terms;

import java.time.Instant;

public record TermsAcceptanceResponse(
        Integer version,
        Instant acceptedAt
) {
}
