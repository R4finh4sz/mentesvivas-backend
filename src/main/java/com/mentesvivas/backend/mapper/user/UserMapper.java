package com.mentesvivas.backend.mapper.user;

import com.mentesvivas.backend.dto.user.ProfileResponse;
import com.mentesvivas.backend.dto.user.UserResponse;
import com.mentesvivas.backend.entity.user.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                user.isEmailConfirmed());
    }

    public static ProfileResponse toProfile(User user) {
        return new ProfileResponse(user.getId(), user.getFullName(), user.getCpf(), user.getBirthDate(),
                user.getPhone(), user.getEmail(), user.isEmailConfirmed(), user.getRole(),
                user.getCrm(), user.getCrmState(), user.isCrmVerified());
    }
}
