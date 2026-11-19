package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.service.audit.AuditTrail;
import com.mentesvivas.backend.cache.TemporaryValue;
import com.mentesvivas.backend.config.TwoFactorProperties;
import com.mentesvivas.backend.dto.auth.TwoFactorChallengeResponse;
import com.mentesvivas.backend.entity.user.MfaMethod;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.auth.InvalidCodeException;
import com.mentesvivas.backend.repository.user.UserRepository;
import com.mentesvivas.backend.security.auth.TokenHasher;
import com.mentesvivas.backend.service.mfa.SecondFactor;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class TwoFactorService {

    private final UserRepository userRepository;
    private final TwoFactorRequirement requirement;
    private final TemporaryValue pendingChallenges;
    private final TokenHasher hasher;
    private final TwoFactorProperties properties;
    private final AuditTrail auditTrail;
    private final Map<MfaMethod, SecondFactor> factors = new EnumMap<>(MfaMethod.class);

    public TwoFactorService(UserRepository userRepository, TwoFactorRequirement requirement,
                            TemporaryValue pendingChallenges, TokenHasher hasher,
                            TwoFactorProperties properties, AuditTrail auditTrail,
                            List<SecondFactor> secondFactors) {
        this.userRepository = userRepository;
        this.requirement = requirement;
        this.pendingChallenges = pendingChallenges;
        this.hasher = hasher;
        this.properties = properties;
        this.auditTrail = auditTrail;
        secondFactors.forEach(factor -> factors.put(factor.method(), factor));
    }

    public TwoFactorChallengeResponse challenge(User user) {
        pendingChallenges.store(pendingKey(user.getEmail()), "1", properties.codeValidity());
        TwoFactorChallengeResponse response = factorOf(user).challenge(user);
        auditTrail.record(AuditRecord.of(AuditEventType.TWO_FACTOR_CHALLENGED, user.getId()));
        return response;
    }

    public User verify(String email, String code) {
        User user = userRepository.findByEmail(email).orElseThrow(InvalidCodeException::new);
        if (!requirement.isRequiredFor(user) || pendingChallenges.get(pendingKey(email)).isEmpty()) {
            auditFailure(user);
            throw new InvalidCodeException();
        }
        try {
            factorOf(user).verify(user, code);
        } catch (InvalidCodeException ex) {
            auditFailure(user);
            throw ex;
        }
        pendingChallenges.remove(pendingKey(email));
        auditTrail.record(AuditRecord.of(AuditEventType.TWO_FACTOR_SUCCEEDED, user.getId()));
        return user;
    }

    private void auditFailure(User user) {
        auditTrail.recordIndependently(AuditRecord.of(AuditEventType.TWO_FACTOR_FAILED, user.getId()));
    }

    private SecondFactor factorOf(User user) {
        return factors.get(user.getMfaMethod());
    }

    private String pendingKey(String email) {
        return "two-factor:pending:" + hasher.hash(email);
    }
}
