package com.mentesvivas.backend.dto.user;

import com.mentesvivas.backend.entity.user.Role;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullName,
        String email,
        Role role
) {
}
