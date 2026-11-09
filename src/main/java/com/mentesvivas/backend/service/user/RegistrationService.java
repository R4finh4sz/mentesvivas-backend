package com.mentesvivas.backend.service.user;

import com.mentesvivas.backend.dto.user.DoctorRegistrationRequest;
import com.mentesvivas.backend.dto.user.RegistrationRequest;
import com.mentesvivas.backend.dto.user.UserResponse;
import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.mapper.user.UserMapper;
import com.mentesvivas.backend.repository.user.UserRepository;
import com.mentesvivas.backend.service.auth.EmailConfirmationService;
import com.mentesvivas.backend.service.terms.TermsAcceptanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final UserUniquenessValidator uniquenessValidator;
    private final PasswordEncoder passwordEncoder;
    private final TermsAcceptanceService termsAcceptanceService;
    private final EmailConfirmationService emailConfirmationService;

    @Transactional
    public UserResponse register(RegistrationRequest request) {
        termsAcceptanceService.validateCurrentVersion(request.termsVersion());
        User user = newUser(request.fullName(), request.cpf(), request.phone(),
                request.email(), request.password(), request.role());
        user.setBirthDate(request.birthDate());
        user.setHealthDataConsentedAt(Instant.now());
        return saveWithAcceptance(user, request.termsVersion());
    }

    @Transactional
    public UserResponse registerDoctor(DoctorRegistrationRequest request) {
        termsAcceptanceService.validateCurrentVersion(request.termsVersion());
        User user = newUser(request.fullName(), request.cpf(), request.phone(),
                request.email(), request.password(), Role.DOCTOR);
        user.setCrm(request.crm());
        user.setCrmState(request.crmState());
        user.setCrmVerified(false);
        return saveWithAcceptance(user, request.termsVersion());
    }

    private UserResponse saveWithAcceptance(User user, Integer termsVersion) {
        User saved = userRepository.save(user);
        termsAcceptanceService.register(saved, termsVersion);
        emailConfirmationService.sendCodeKeepingFlow(saved);
        return UserMapper.toResponse(saved);
    }

    private User newUser(String name, String cpf, String phone, String email, String password, Role role) {
        uniquenessValidator.validate(email, cpf);

        User user = new User();
        user.setFullName(name);
        user.setCpf(cpf);
        user.setPhone(phone);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEmailConfirmed(false);
        return user;
    }
}
