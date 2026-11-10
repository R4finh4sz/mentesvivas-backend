package com.mentesvivas.backend.service.mfa;

import com.mentesvivas.backend.config.TwoFactorProperties;
import com.mentesvivas.backend.dto.auth.TwoFactorChallengeResponse;
import com.mentesvivas.backend.entity.user.MfaMethod;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.service.auth.TwoFactorEmails;
import com.mentesvivas.backend.service.code.CodePolicy;
import com.mentesvivas.backend.service.code.OneTimeCodeService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static com.mentesvivas.backend.config.CodePolicyConfig.TWO_FACTOR;

@Component
public class EmailSecondFactor implements SecondFactor {

    private static final String MESSAGE =
            "Enviamos um codigo de verificacao para o seu e-mail. Informe-o para concluir o acesso.";

    private final OneTimeCodeService codeService;
    private final CodePolicy policy;
    private final TwoFactorEmails emails;
    private final TwoFactorProperties properties;

    public EmailSecondFactor(OneTimeCodeService codeService, @Qualifier(TWO_FACTOR) CodePolicy policy,
                             TwoFactorEmails emails, TwoFactorProperties properties) {
        this.codeService = codeService;
        this.policy = policy;
        this.emails = emails;
        this.properties = properties;
    }

    @Override
    public MfaMethod method() {
        return MfaMethod.EMAIL;
    }

    @Override
    public TwoFactorChallengeResponse challenge(User user) {
        codeService.generate(policy, user.getEmail())
                .ifPresent(code -> emails.sendCode(user.getEmail(), code));
        return new TwoFactorChallengeResponse(true, MESSAGE, properties.codeValidity().toSeconds(), MfaMethod.EMAIL);
    }

    @Override
    public void verify(User user, String code) {
        codeService.validateAndConsume(policy, user.getEmail(), code);
    }
}
