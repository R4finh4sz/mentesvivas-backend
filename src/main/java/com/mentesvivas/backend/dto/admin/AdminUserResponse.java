package com.mentesvivas.backend.dto.admin;

import com.mentesvivas.backend.entity.user.Role;

import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String fullName,
        String email,
        Role role,
        boolean active,
        boolean emailConfirmed,
        Boolean crmVerified,
        Instant createdAt
) {
}
