package com.mentesvivas.backend.dto.terms;

import com.mentesvivas.backend.entity.terms.TermsAudience;
import com.mentesvivas.backend.entity.terms.TermsType;
import com.mentesvivas.backend.entity.user.Role;

import java.time.Instant;
import java.util.Set;

public record TermsResponse(
        Integer version,
        TermsType type,
        TermsAudience audience,
        Set<Role> roles,
        String title,
        String content,
        Instant publishedAt
) {
}
