package com.mentesvivas.backend.dto.admin;

import java.util.UUID;

public record PendingDoctorResponse(
        UUID id,
        String fullName,
        String email,
        String crm,
        String crmState
) {
}
