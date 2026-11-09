package com.mentesvivas.backend.mapper.user;

import com.mentesvivas.backend.dto.user.UserResponse;
import com.mentesvivas.backend.entity.user.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                user.isEmailConfirmed());
    }
}
