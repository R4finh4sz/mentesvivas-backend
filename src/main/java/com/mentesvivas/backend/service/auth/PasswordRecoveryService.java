package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.config.PasswordRecoveryProperties;
import com.mentesvivas.backend.dto.auth.PasswordRecoveryRequestedResponse;
import com.mentesvivas.backend.dto.auth.ResetPasswordRequest;
import com.mentesvivas.backend.exception.auth.InvalidRecoveryCodeException;
import com.mentesvivas.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {

    private static final String REQUEST_MESSAGE =
            "Se o e-mail estiver cadastrado, enviaremos um codigo de verificacao.";

    private final UserRepository userRepository;
    private final RecoveryCodeService codeService;
    private final PasswordRecoveryEmails emails;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final LoginLockoutService loginLockoutService;
    private final PasswordRecoveryProperties properties;

    public PasswordRecoveryRequestedResponse requestRecovery(String email) {
        userRepository.findByEmail(email).ifPresent(user ->
                codeService.generate(email).ifPresent(code -> emails.sendCode(email, code)));
        return new PasswordRecoveryRequestedResponse(REQUEST_MESSAGE, properties.codeValidity().toSeconds());
    }

    @Transactional
    public void reset(ResetPasswordRequest request) {
        codeService.validateAndConsume(request.email(), request.code());
        var user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidRecoveryCodeException::new);

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenService.revokeAll(user);
        loginLockoutService.release(request.email());
        emails.notifyPasswordChanged(user.getEmail());
    }
}
