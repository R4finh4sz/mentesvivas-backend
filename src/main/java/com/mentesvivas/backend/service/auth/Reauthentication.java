package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.dto.auth.TwoFactorChallengeResponse;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.entity.user.MfaMethod;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.auth.InvalidCodeException;
import com.mentesvivas.backend.service.audit.AuditTrail;
import com.mentesvivas.backend.service.mfa.SecondFactor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class Reauthentication {

    private final PasswordEncoder passwordEncoder;
    private final AuditTrail auditTrail;
    private final Map<MfaMethod, SecondFactor> factors = new EnumMap<>(MfaMethod.class);

    public Reauthentication(PasswordEncoder passwordEncoder, AuditTrail auditTrail,
                            List<SecondFactor> secondFactors) {
        this.passwordEncoder = passwordEncoder;
        this.auditTrail = auditTrail;
        secondFactors.forEach(factor -> factors.put(factor.method(), factor));
    }

    public TwoFactorChallengeResponse challenge(User user) {
        return factors.get(user.getMfaMethod()).challenge(user);
    }

    public void verify(User user, String password, String code) {
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            auditFailure(user);
            throw new AccessDeniedException("Senha invalida. Confirme a senha para continuar.");
        }
        try {
            factors.get(user.getMfaMethod()).verify(user, code);
        } catch (InvalidCodeException ex) {
            auditFailure(user);
            throw ex;
        }
    }

    private void auditFailure(User user) {
        auditTrail.recordIndependently(AuditRecord.of(AuditEventType.TWO_FACTOR_FAILED, user.getId()));
    }
}
