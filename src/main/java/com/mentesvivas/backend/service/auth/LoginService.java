package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.dto.auth.LoginRequest;
import com.mentesvivas.backend.dto.auth.LoginResult;
import com.mentesvivas.backend.dto.auth.TokenResponse;
import com.mentesvivas.backend.dto.auth.VerifyTwoFactorRequest;
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
    private final TwoFactorRequirement twoFactorRequirement;
    private final TwoFactorService twoFactorService;
    private final String dummyHash;

    public LoginService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        TokenService tokenService, LoginLockoutService loginLockoutService,
                        TwoFactorRequirement twoFactorRequirement, TwoFactorService twoFactorService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.loginLockoutService = loginLockoutService;
        this.twoFactorRequirement = twoFactorRequirement;
        this.twoFactorService = twoFactorService;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-constant-time");
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        loginLockoutService.requireUnlocked(request.email());
        Optional<User> user = userRepository.findByEmail(request.email());

        String hash = user.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
        if (user.isEmpty() || !passwordMatches) {
            loginLockoutService.recordFailure(request.email());
            throw new InvalidCredentialsException();
        }
        loginLockoutService.recordSuccess(request.email());
        if (twoFactorRequirement.isRequiredFor(user.get())) {
            return LoginResult.challenged(twoFactorService.challenge(user.get()));
        }
        return LoginResult.authenticated(tokenService.issue(user.get()));
    }

    @Transactional
    public TokenResponse verifyTwoFactor(VerifyTwoFactorRequest request) {
        return tokenService.issue(twoFactorService.verify(request.email(), request.code()));
    }
}
