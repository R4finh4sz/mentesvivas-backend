package com.mentesvivas.backend.support;

import com.mentesvivas.backend.entity.terms.TermsVersion;
import com.mentesvivas.backend.repository.terms.TermsVersionRepository;
import com.mentesvivas.backend.repository.user.UserRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class TermsTestSupport {

    public static final String ADMIN_EMAIL = "admin@mentesvivas.test";
    public static final String ADMIN_PASSWORD = "senhaAdminTeste123";

    private final TermsVersionRepository termsVersionRepository;
    private final UserRepository userRepository;

    public TermsTestSupport(TermsVersionRepository termsVersionRepository, UserRepository userRepository) {
        this.termsVersionRepository = termsVersionRepository;
        this.userRepository = userRepository;
    }

    public int publish() {
        int version = termsVersionRepository.findTopByOrderByVersionDesc()
                .map(terms -> terms.getVersion() + 1)
                .orElse(1);
        var admin = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
        termsVersionRepository.saveAndFlush(new TermsVersion(version, "Termos v" + version, "Conteudo", admin.getId(), Instant.now()));
        return version;
    }
}
