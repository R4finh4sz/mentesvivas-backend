package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.dto.auth.LoginRequest;
import com.mentesvivas.backend.dto.auth.TokenResponse;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.auth.InvalidCredentialsException;
import com.mentesvivas.backend.repository.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final LoginLockoutService loginLockoutService;
    private final String dummyHash;

    public LoginService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        TokenService tokenService, LoginLockoutService loginLockoutService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.loginLockoutService = loginLockoutService;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-constant-time");
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        loginLockoutService.requireUnlocked(request.email());
        Optional<User> user = userRepository.findByEmail(request.email());

        String hash = user.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
        if (user.isEmpty() || !passwordMatches) {
            loginLockoutService.recordFailure(request.email());
            throw new InvalidCredentialsException();
        }
        loginLockoutService.recordSuccess(request.email());
        return tokenService.issue(user.get());
    }
}
