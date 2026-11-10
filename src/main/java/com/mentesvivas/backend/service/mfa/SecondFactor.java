package com.mentesvivas.backend.service.mfa;

import com.mentesvivas.backend.dto.auth.TwoFactorChallengeResponse;
import com.mentesvivas.backend.entity.user.MfaMethod;
import com.mentesvivas.backend.entity.user.User;

public interface SecondFactor {

    MfaMethod method();

    TwoFactorChallengeResponse challenge(User user);

    void verify(User user, String code);
}
