package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class TwoFactorRequirement {

    private static final Set<Role> ROLES_REQUIRING_SECOND_FACTOR = Set.of(Role.DOCTOR, Role.ADMINISTRATOR);

    public boolean isRequiredFor(User user) {
        return ROLES_REQUIRING_SECOND_FACTOR.contains(user.getRole());
    }
}
