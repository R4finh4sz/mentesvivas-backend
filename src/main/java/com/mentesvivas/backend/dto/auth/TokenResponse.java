package com.mentesvivas.backend.dto.auth;

import com.mentesvivas.backend.dto.user.UserResponse;
import com.mentesvivas.backend.entity.user.Role;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        long refreshExpiresIn,
        Role role,
        boolean termsAccepted,
        UserResponse user
) {
}
