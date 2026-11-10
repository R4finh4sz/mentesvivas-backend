package com.mentesvivas.backend.dto.link;

import java.util.UUID;

public record DoctorSummaryResponse(
        UUID id,
        String fullName,
        String crm,
        String crmState,
        String photoUrl
) {
}
