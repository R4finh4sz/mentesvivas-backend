package com.mentesvivas.backend.dto.audit;

import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.user.Role;

import java.time.Instant;
import java.util.UUID;

public record AccessEntryResponse(
        Instant occurredAt,
        PatientDataArea area,
        UUID accessorId,
        String accessorName,
        Role accessorRole,
        String crm,
        String crmState
) {
}
