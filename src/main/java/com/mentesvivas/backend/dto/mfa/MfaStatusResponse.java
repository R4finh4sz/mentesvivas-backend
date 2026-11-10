package com.mentesvivas.backend.dto.mfa;

import com.mentesvivas.backend.entity.user.MfaMethod;

import java.util.List;

public record MfaStatusResponse(
        MfaMethod method,
        List<MfaMethod> availableMethods
) {
}
