package com.mentesvivas.backend.service.auth;

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
    private final Map<MfaMethod, SecondFactor> factors = new EnumMap<>(MfaMethod.class);

    public TwoFactorService(UserRepository userRepository, TwoFactorRequirement requirement,
                            TemporaryValue pendingChallenges, TokenHasher hasher,
                            TwoFactorProperties properties, List<SecondFactor> secondFactors) {
        this.userRepository = userRepository;
        this.requirement = requirement;
        this.pendingChallenges = pendingChallenges;
        this.hasher = hasher;
        this.properties = properties;
        secondFactors.forEach(factor -> factors.put(factor.method(), factor));
    }

    public TwoFactorChallengeResponse challenge(User user) {
        pendingChallenges.store(pendingKey(user.getEmail()), "1", properties.codeValidity());
        return factorOf(user).challenge(user);
    }

    public User verify(String email, String code) {
        User user = userRepository.findByEmail(email).orElseThrow(InvalidCodeException::new);
        if (!requirement.isRequiredFor(user) || pendingChallenges.get(pendingKey(email)).isEmpty()) {
            throw new InvalidCodeException();
        }
        factorOf(user).verify(user, code);
        pendingChallenges.remove(pendingKey(email));
        return user;
    }

    private SecondFactor factorOf(User user) {
        return factors.get(user.getMfaMethod());
    }

    private String pendingKey(String email) {
        return "two-factor:pending:" + hasher.hash(email);
    }
}
