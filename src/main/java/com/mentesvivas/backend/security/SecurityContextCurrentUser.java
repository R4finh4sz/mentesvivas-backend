package com.mentesvivas.backend.security;

import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.auth.InvalidSessionException;
import com.mentesvivas.backend.exception.auth.UserNotAuthenticatedException;
import com.mentesvivas.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SecurityContextCurrentUser implements CurrentUser {

    private final UserRepository userRepository;

    @Override
    public User get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UserNotAuthenticatedException();
        }
        return userRepository.findById(idOf(authentication))
                .orElseThrow(InvalidSessionException::new);
    }

    private UUID idOf(Authentication authentication) {
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ex) {
            throw new InvalidSessionException();
        }
    }
}
