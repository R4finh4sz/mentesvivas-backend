package com.mentesvivas.backend.mapper.terms;

import com.mentesvivas.backend.dto.terms.TermsAcceptanceResponse;
import com.mentesvivas.backend.dto.terms.TermsResponse;
import com.mentesvivas.backend.entity.terms.TermsAcceptance;
import com.mentesvivas.backend.entity.terms.TermsVersion;

public final class TermsMapper {

    private TermsMapper() {
    }

    public static TermsResponse toResponse(TermsVersion terms) {
        return new TermsResponse(terms.getVersion(), terms.getType(), terms.getAudience(), terms.getAudienceRoles(),
                terms.getTitle(), terms.getContent(), terms.getPublishedAt());
    }

    public static TermsAcceptanceResponse toResponse(TermsAcceptance acceptance) {
        return new TermsAcceptanceResponse(acceptance.getVersion(), acceptance.getAcceptedAt());
    }
}
