package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.service.audit.AuditTrail;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.auth.InvalidCredentialsException;
import com.mentesvivas.backend.repository.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CredentialAuthenticator {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginLockoutService loginLockoutService;
    private final AuditTrail auditTrail;
    private final String dummyHash;

    public CredentialAuthenticator(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                   LoginLockoutService loginLockoutService, AuditTrail auditTrail) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginLockoutService = loginLockoutService;
        this.auditTrail = auditTrail;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-constant-time");
    }

    public User authenticate(String email, String password) {
        loginLockoutService.requireUnlocked(email);
        Optional<User> user = userRepository.findByEmail(email);

        String hash = user.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(password, hash);
        if (user.isEmpty() || !passwordMatches) {
            loginLockoutService.recordFailure(email);
            auditTrail.recordIndependently(AuditRecord.of(AuditEventType.LOGIN_FAILED,
                    user.map(User::getId).orElse(null)));
            throw new InvalidCredentialsException();
        }
        loginLockoutService.recordSuccess(email);
        return user.get();
    }
}
