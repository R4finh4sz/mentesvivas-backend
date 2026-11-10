package com.mentesvivas.backend.security;

import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoleRequirement {

    private final CurrentUser currentUser;

    public User require(Role role) {
        User user = currentUser.get();
        if (user.getRole() != role) {
            throw new AccessDeniedException("Acesso restrito ao perfil " + role.name() + ".");
        }
        return user;
    }
}
