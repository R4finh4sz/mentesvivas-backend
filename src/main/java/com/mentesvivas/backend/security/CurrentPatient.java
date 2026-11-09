package com.mentesvivas.backend.security;

import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentPatient {

    private final CurrentUser currentUser;

    public User get() {
        User user = currentUser.get();
        if (user.getRole() != Role.PATIENT) {
            throw new AccessDeniedException("Acesso restrito a pacientes.");
        }
        return user;
    }
}
