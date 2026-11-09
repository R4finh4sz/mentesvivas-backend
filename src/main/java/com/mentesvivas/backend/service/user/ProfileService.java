package com.mentesvivas.backend.service.user;

import com.mentesvivas.backend.dto.user.ProfileResponse;
import com.mentesvivas.backend.dto.user.UpdateProfileRequest;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.mapper.user.UserMapper;
import com.mentesvivas.backend.security.CurrentUser;
import com.mentesvivas.backend.service.auth.EmailConfirmationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final CurrentUser currentUser;
    private final UserUniquenessValidator uniquenessValidator;
    private final EmailConfirmationService emailConfirmationService;

    @Transactional(readOnly = true)
    public ProfileResponse get() {
        return UserMapper.toProfile(currentUser.get());
    }

    @Transactional
    public ProfileResponse update(UpdateProfileRequest request) {
        User user = currentUser.get();

        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.birthDate() != null) {
            user.setBirthDate(request.birthDate());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        boolean emailChanged = changeEmail(user, request.email());

        if (emailChanged) {
            emailConfirmationService.sendCodeKeepingFlow(user);
        }
        return UserMapper.toProfile(user);
    }

    private boolean changeEmail(User user, String newEmail) {
        if (newEmail == null || newEmail.equals(user.getEmail())) {
            return false;
        }
        uniquenessValidator.validateEmail(newEmail);
        user.setEmail(newEmail);
        user.setEmailConfirmed(false);
        return true;
    }
}
