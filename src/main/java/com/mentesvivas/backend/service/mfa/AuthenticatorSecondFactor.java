package com.mentesvivas.backend.service.mfa;

import com.mentesvivas.backend.config.TwoFactorProperties;
import com.mentesvivas.backend.dto.auth.TwoFactorChallengeResponse;
import com.mentesvivas.backend.entity.user.MfaMethod;
import com.mentesvivas.backend.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticatorSecondFactor implements SecondFactor {

    private static final String MESSAGE =
            "Abra o aplicativo autenticador e informe o codigo de seis digitos (ou um codigo de backup) para concluir o acesso.";

    private final AuthenticatorCredentialVerifier verifier;
    private final TwoFactorProperties properties;

    @Override
    public MfaMethod method() {
        return MfaMethod.AUTHENTICATOR;
    }

    @Override
    public TwoFactorChallengeResponse challenge(User user) {
        return new TwoFactorChallengeResponse(true, MESSAGE, properties.codeValidity().toSeconds(),
                MfaMethod.AUTHENTICATOR);
    }

    @Override
    public void verify(User user, String code) {
        verifier.verify(user, code);
    }
}
