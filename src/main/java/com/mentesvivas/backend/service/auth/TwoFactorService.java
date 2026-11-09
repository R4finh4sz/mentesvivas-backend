package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.config.TwoFactorProperties;
import com.mentesvivas.backend.dto.auth.TwoFactorChallengeResponse;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.auth.InvalidCodeException;
import com.mentesvivas.backend.repository.user.UserRepository;
import com.mentesvivas.backend.service.code.CodePolicy;
import com.mentesvivas.backend.service.code.OneTimeCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TwoFactorService {

    private static final String CHALLENGE_MESSAGE =
            "Enviamos um codigo de verificacao para o seu e-mail. Informe-o para concluir o acesso.";

    private final UserRepository userRepository;
    private final OneTimeCodeService codeService;
    private final CodePolicy twoFactorPolicy;
    private final TwoFactorEmails emails;
    private final TwoFactorRequirement requirement;
    private final TwoFactorProperties properties;

    public TwoFactorChallengeResponse challenge(User user) {
        codeService.generate(twoFactorPolicy, user.getEmail())
                .ifPresent(code -> emails.sendCode(user.getEmail(), code));
        return new TwoFactorChallengeResponse(true, CHALLENGE_MESSAGE, properties.codeValidity().toSeconds());
    }

    public User verify(String email, String code) {
        codeService.validateAndConsume(twoFactorPolicy, email, code);
        User user = userRepository.findByEmail(email).orElseThrow(InvalidCodeException::new);
        if (!requirement.isRequiredFor(user)) {
            throw new InvalidCodeException();
        }
        return user;
    }
}
