package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.config.EmailConfirmationProperties;
import com.mentesvivas.backend.dto.auth.CodeRequestedResponse;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.auth.InvalidCodeException;
import com.mentesvivas.backend.exception.common.ServiceUnavailableException;
import com.mentesvivas.backend.repository.user.UserRepository;
import com.mentesvivas.backend.service.code.CodePolicy;
import com.mentesvivas.backend.service.code.OneTimeCodeService;
import com.mentesvivas.backend.service.link.PendingInvitationAttacher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailConfirmationService {

    private static final String REQUEST_MESSAGE =
            "Se o e-mail estiver cadastrado e pendente de confirmacao, enviaremos um codigo.";

    private final UserRepository userRepository;
    private final OneTimeCodeService codeService;
    private final CodePolicy emailConfirmationPolicy;
    private final EmailConfirmationEmails emails;
    private final EmailConfirmationProperties properties;
    private final PendingInvitationAttacher invitationAttacher;

    public void sendCode(User user) {
        if (user.isEmailConfirmed()) {
            return;
        }
        codeService.generate(emailConfirmationPolicy, user.getEmail())
                .ifPresent(code -> emails.sendCode(user.getEmail(), code));
    }

    public void sendCodeKeepingFlow(User user) {
        try {
            sendCode(user);
        } catch (ServiceUnavailableException ex) {
            log.warn("Could not send the e-mail confirmation code; the user can request a new one", ex);
        }
    }

    public CodeRequestedResponse resend(String email) {
        userRepository.findByEmail(email).ifPresent(this::sendCode);
        return new CodeRequestedResponse(REQUEST_MESSAGE, properties.codeValidity().toSeconds());
    }

    @Transactional
    public void confirm(String email, String code) {
        codeService.validateAndConsume(emailConfirmationPolicy, email, code);
        User user = userRepository.findByEmail(email).orElseThrow(InvalidCodeException::new);
        user.setEmailConfirmed(true);
        invitationAttacher.attach(user);
    }
}
