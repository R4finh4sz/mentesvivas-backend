package com.mentesvivas.backend.dto.terms;

import java.time.Instant;

public record TermsResponse(
        Integer version,
        String title,
        String content,
        Instant publishedAt
) {
}
