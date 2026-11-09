package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.config.PasswordRecoveryProperties;
import com.mentesvivas.backend.dto.auth.CodeRequestedResponse;
import com.mentesvivas.backend.dto.auth.ResetPasswordRequest;
import com.mentesvivas.backend.exception.auth.InvalidCodeException;
import com.mentesvivas.backend.repository.user.UserRepository;
import com.mentesvivas.backend.service.code.CodePolicy;
import com.mentesvivas.backend.service.code.OneTimeCodeService;
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
    private final OneTimeCodeService codeService;
    private final CodePolicy passwordRecoveryPolicy;
    private final PasswordRecoveryEmails emails;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final LoginLockoutService loginLockoutService;
    private final PasswordRecoveryProperties properties;

    public CodeRequestedResponse requestRecovery(String email) {
        userRepository.findByEmail(email).ifPresent(user ->
                codeService.generate(passwordRecoveryPolicy, email).ifPresent(code -> emails.sendCode(email, code)));
        return new CodeRequestedResponse(REQUEST_MESSAGE, properties.codeValidity().toSeconds());
    }

    @Transactional
    public void reset(ResetPasswordRequest request) {
        codeService.validateAndConsume(passwordRecoveryPolicy, request.email(), request.code());
        var user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCodeException::new);

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenService.revokeAll(user);
        loginLockoutService.release(request.email());
        emails.notifyPasswordChanged(user.getEmail());
    }
}
