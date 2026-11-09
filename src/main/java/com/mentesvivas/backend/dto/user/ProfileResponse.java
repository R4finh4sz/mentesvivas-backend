package com.mentesvivas.backend.dto.user;

import com.mentesvivas.backend.entity.user.Role;

import java.time.LocalDate;
import java.util.UUID;

public record ProfileResponse(
        UUID id,
        String fullName,
        String cpf,
        LocalDate birthDate,
        String phone,
        String email,
        boolean emailConfirmed,
        Role role,
        String crm,
        String crmState,
        boolean crmVerified
) {
}
