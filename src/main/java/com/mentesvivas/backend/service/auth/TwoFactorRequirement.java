package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class TwoFactorRequirement {

    private static final Set<Role> ROLES_WITH_CLINICAL_ACCESS = Set.of(Role.DOCTOR);

    public boolean isRequiredFor(User user) {
        return ROLES_WITH_CLINICAL_ACCESS.contains(user.getRole());
    }
}
